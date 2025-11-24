package service

import data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import utils.Logger
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.min

/**
 * HubLink客户端管理器
 */
class HubLinkClientManager(private val config: HubLinkConfig) {
    private val _state = MutableStateFlow<HubLinkState>(HubLinkState.Disconnected)
    val state: StateFlow<HubLinkState> = _state

    val reconnectManager = HubLinkReconnectManager(config, this)
    val reconnectState: StateFlow<HubLinkReconnectState> = reconnectManager.reconnectState

    private var transport: HubLinkTransport? = null
    private var protocol: HubLinkProtocol? = null
    private var localProxyServer: LocalProxyServer? = null
    private var proxyJob: Job? = null
    private var proxyPort: Int = 0

    /**
     * 连接到HubLink服务端
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = HubLinkState.Connecting

            // 输出详细的连接参数日志
            Logger.info("=== HubLink 连接参数详情 ===")
            Logger.info("配置名称: ${config.name}")
            Logger.info("服务器地址: ${config.host}:${config.port}")
            Logger.info("本地代理端口: ${if (config.localPort > 0) config.localPort else "自动分配"}")
            Logger.info("传输类型: ${when (config.transport) {
                HubLinkTransportType.DIRECT -> "直接TCP连接"
                HubLinkTransportType.MQTT -> "MQTT代理"
            }}")
            Logger.info("预共享密钥: ${config.psk}")
            Logger.info("自动重连: ${config.autoReconnect}")
            Logger.info("最大重试次数: ${config.maxRetries}")
            Logger.info("基础重试间隔: ${config.baseRetryDelay}ms")
            Logger.info("最大重试间隔: ${config.maxRetryDelay}ms")
            Logger.info("流量混淆: ${config.obfs ?: "无"}")
            if (config.obfs != null && config.obfsHost != null) {
                Logger.info("混淆主机: ${config.obfsHost}")
            }
            Logger.info("配置共享: ${config.isShared}")

            config.mqttConfig?.let { mqtt ->
                Logger.info("MQTT服务器: ${mqtt.mqttHost}:${mqtt.mqttPort}")
                Logger.info("MQTT SSL: ${mqtt.useSSL}")
                Logger.info("MQTT客户端ID: ${mqtt.clientId}")
                Logger.info("MQTT服务端ID: ${mqtt.serverId}")
                Logger.info("MQTT认证: ${mqtt.username?.let { "已配置" } ?: "无"}")
            }

            Logger.info("=== 开始连接 ${config.name} ===")

            // 创建传输层
            transport = when (config.transport) {
                HubLinkTransportType.DIRECT -> DirectTransport(config.host, config.port)
                HubLinkTransportType.MQTT -> config.mqttConfig?.let { MqttTransport(it) }
                    ?: return@withContext Result.failure(Exception("MQTT配置为空"))
            }

            // 建立连接
            transport?.connect()?.getOrThrow()

            // 创建协议处理器
            protocol = HubLinkProtocol(transport!!, config.psk)

            // 执行握手
            protocol?.performHandshake()?.getOrThrow()

            // 启动本地代理服务器
            proxyPort = if (config.localPort > 0) {
                // 使用指定的本地端口
                config.localPort
            } else {
                // 自动分配可用端口
                findAvailablePort()
            }
            // 启动本地代理服务器
            localProxyServer = LocalProxyServer(this@HubLinkClientManager, proxyPort)
            localProxyServer?.start()

            // 启动代理转发协程
            proxyJob = CoroutineScope(Dispatchers.IO).launch {
                startProxyForwarding()
            }

            // 连接成功
            _state.value = HubLinkState.Connected(proxyPort, config.host, config.port)
            reconnectManager.reset()

            Logger.info("=== HubLink 连接成功 ===")
            Logger.info("配置名称: ${config.name}")
            Logger.info("远程服务器: ${config.host}:${config.port}")
            Logger.info("本地代理端口: $proxyPort")
            Logger.info("传输类型: ${when (config.transport) {
                HubLinkTransportType.DIRECT -> "直接TCP连接"
                HubLinkTransportType.MQTT -> "MQTT代理"
            }}")
            Logger.info("流量混淆: ${config.obfs ?: "无"}")
            Logger.info("连接状态: 已建立，可开始代理转发")
            Result.success(Unit)

        } catch (e: Exception) {
            Logger.error("HubLink: 连接失败 ${config.name}", e)
            _state.value = HubLinkState.Error(e.message ?: "连接失败")

            // 启动自动重连
            if (config.autoReconnect) {
                reconnectManager.startReconnect()
            }

            Result.failure(e)
        }
    }

    /**
     * 断开连接
     */
    suspend fun disconnect() {
        Logger.info("=== HubLink 断开连接 ===")
        Logger.info("配置名称: ${config.name}")
        Logger.info("远程服务器: ${config.host}:${config.port}")
        Logger.info("本地代理端口: ${proxyPort ?: "未分配"}")

        // 停止重连
        reconnectManager.stopReconnect()
        Logger.info("自动重连: 已停止")

        // 停止代理转发
        proxyJob?.cancel()
        proxyJob = null
        Logger.info("代理转发: 已停止")

        // 停止本地代理服务器
        localProxyServer?.stop()
        localProxyServer = null
        Logger.info("本地代理服务器: 已停止")

        // 断开传输连接
        protocol = null
        transport?.disconnect()
        transport = null
        Logger.info("传输连接: 已断开")

        _state.value = HubLinkState.Disconnected
        Logger.info("连接状态: 已完全断开")
    }

    /**
     * 启动代理转发监控
     */
    private suspend fun startProxyForwarding() = withContext(Dispatchers.IO) {
        while (isActive) {
            try {
                // 监控本地代理服务器状态
                // 如果LocalProxyServer检测到连接异常，会通过forwardRequest抛出异常
                delay(5000) // 每5秒检查一次状态

                // 检查transport连接状态
                if (transport?.isConnected() == false) {
                    Logger.warn("HubLink: 检测到连接断开")
                    throw Exception("Connection lost")
                }

            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Logger.error("HubLink: 连接异常", e)
                    // 连接异常，触发重连
                    if (config.autoReconnect) {
                        reconnectManager.startReconnect()
                    }
                    break
                }
            }
        }
    }

    /**
     * 转发代理请求
     */
    suspend fun forwardRequest(data: ByteArray): Result<ByteArray> {
        return try {
            protocol?.sendData(data)?.getOrThrow()
            protocol?.receiveData() ?: Result.failure(Exception("No response received"))
        } catch (e: Exception) {
            Logger.error("HubLink: 请求转发失败", e)
            Result.failure(e)
        }
    }

    /**
     * 查找可用端口
     */
    private fun findAvailablePort(): Int {
        return try {
            ServerSocket(0).use { socket ->
                socket.localPort
            }
        } catch (e: Exception) {
            // 默认端口
            1080
        }
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = transport?.isConnected() == true
}

/**
 * HubLink重连管理器
 */
class HubLinkReconnectManager(
    private val config: HubLinkConfig,
    private val client: HubLinkClientManager
) {
    private var retryCount = 0
    private val maxRetries = config.maxRetries
    private val baseDelayMs = config.baseRetryDelay
    private val maxDelayMs = config.maxRetryDelay

    private var reconnectJob: Job? = null
    private val _reconnectState = MutableStateFlow<HubLinkReconnectState>(HubLinkReconnectState.Idle)
    val reconnectState: StateFlow<HubLinkReconnectState> = _reconnectState

    /**
     * 开始重连
     */
    fun startReconnect() {
        if (reconnectJob?.isActive == true) return

        retryCount = 0
        reconnectJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive && retryCount < maxRetries) {
                val delayMs = calculateDelay()
                _reconnectState.value = HubLinkReconnectState.Waiting(System.currentTimeMillis() + delayMs)

                delay(delayMs)

                _reconnectState.value = HubLinkReconnectState.Retrying
                retryCount++

                Logger.info("HubLink: 重连尝试 ${retryCount}/${maxRetries}，等待 ${delayMs}ms")

                try {
                    client.connect()
                    // 连接成功，重置状态
                    retryCount = 0
                    _reconnectState.value = HubLinkReconnectState.Idle
                    Logger.info("HubLink: 重连成功")
                    break
                } catch (e: Exception) {
                    Logger.warn("HubLink: 重连失败 (${retryCount}/${maxRetries}): ${e.message}")
                    if (retryCount >= maxRetries) {
                        _reconnectState.value = HubLinkReconnectState.MaxRetriesExceeded
                        Logger.error("HubLink: 达到最大重试次数，停止重连")
                        break
                    }
                    // 继续重试
                }
            }
        }
    }

    /**
     * 停止重连
     */
    fun stopReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        retryCount = 0
        _reconnectState.value = HubLinkReconnectState.Idle
    }

    /**
     * 计算重连延迟
     */
    private fun calculateDelay(): Long {
        val exponentialDelay = baseDelayMs * (1L shl min(retryCount, 10))
        val jitter = (0..1000).random()
        return min(exponentialDelay + jitter, maxDelayMs)
    }

    /**
     * 重置状态
     */
    fun reset() {
        stopReconnect()
        retryCount = 0
    }
}
