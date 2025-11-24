package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)

/**
 * HubLink代理协议数据模型
 */

/**
 * HubLink连接配置
 */
@Serializable
data class HubLinkConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 6180,
    val psk: String,  // 预共享密钥 (长度必须>=32字符)
    val localPort: Int = 0,  // 本地SOCKS5代理端口，0表示自动分配
    val transport: HubLinkTransportType = HubLinkTransportType.DIRECT,
    val isShared: Boolean = false,  // 是否共享配置
    val mqttConfig: HubLinkMqttConfig? = null,
    val autoReconnect: Boolean = true,
    val maxRetries: Int = 10,
    val baseRetryDelay: Long = 1000, // 基础重试间隔(毫秒)
    val maxRetryDelay: Long = 300000, // 最大重试间隔(毫秒)
    val obfs: String? = "tls",  // 混淆类型: "tls", "http", null
    val obfsHost: String? = "www.bing.com"  // 混淆主机
)

/**
 * MQTT配置
 */
@Serializable
data class HubLinkMqttConfig(
    val mqttHost: String,              // MQTT 服务器地址
    val mqttPort: Int = 1883,          // MQTT 服务器端口
    val useSSL: Boolean = false,       // 是否使用 SSL/TLS
    val clientId: String,              // MQTT 客户端ID
    val serverId: String,              // MQTT 服务端ID
    val username: String? = null,      // MQTT 认证用户名
    val password: String? = null,      // MQTT 认证密码
    val sessionId: String = java.util.UUID.randomUUID().toString() // 会话ID
)

/**
 * 传输类型枚举
 */
@Serializable
enum class HubLinkTransportType {
    DIRECT,     // 直接TCP连接
    MQTT        // 通过MQTT代理
}

/**
 * HubLink连接状态
 */
sealed class HubLinkState {
    object Disconnected : HubLinkState()
    object Connecting : HubLinkState()
    data class Connected(val localPort: Int, val remoteHost: String, val remotePort: Int) : HubLinkState()
    data class Error(val message: String) : HubLinkState()
}

/**
 * HubLink重连状态
 */
sealed class HubLinkReconnectState {
    object Idle : HubLinkReconnectState()
    data class Waiting(val nextRetryAt: Long) : HubLinkReconnectState()
    object Retrying : HubLinkReconnectState()
    object MaxRetriesExceeded : HubLinkReconnectState()
}

/**
 * HubLink配置管理器
 */
object HubLinkManager {
    private var currentUserId: String = ""
    private val _configs = MutableStateFlow<List<HubLinkConfig>>(emptyList())
    val configs: StateFlow<List<HubLinkConfig>> = _configs.asStateFlow()
    private val clients = mutableMapOf<String, service.HubLinkClientManager>()

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
        loadConfigs()
    }

    /**
     * 加载配置
     */
    private fun loadConfigs() {
        if (currentUserId.isEmpty()) return

        try {
            val mergedData = GitDataManager.getAllMergedData()
            _configs.value = mergedData.hublinkConfigs
            println("✅ 加载代理配置成功，共 ${_configs.value.size} 个配置")
        } catch (e: Exception) {
            println("❌ 加载代理配置失败: ${e.message}")
            _configs.value = emptyList()
        }
    }

    /**
     * 保存配置
     */
    suspend fun saveConfig(config: HubLinkConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 获取当前用户的数据
            val currentUserData = GitDataManager.getUserData(currentUserId)

            // 更新配置列表
            val currentConfigs = _configs.value
            val updatedConfigs = if (currentConfigs.any { it.id == config.id }) {
                // 更新现有配置
                currentConfigs.map { if (it.id == config.id) config else it }
            } else {
                // 添加新配置
                currentConfigs + config
            }

            // 更新状态
            _configs.value = updatedConfigs

            // 保存到Git
            val updatedUserData = currentUserData.copy(hublinkConfigs = updatedConfigs)
            GitDataManager.saveCurrentUserData(currentUserId, updatedUserData).getOrThrow()

            // 更新合并目录，确保UI能看到最新数据
            GitDataManager.mergeAllUserData()

            println("✅ 保存代理配置成功")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ 保存代理配置失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 获取所有配置
     */
    fun getAllConfigs(): List<HubLinkConfig> = _configs.value

    /**
     * 获取配置
     */
    fun getConfig(id: String): HubLinkConfig? = _configs.value.find { it.id == id }

    /**
     * 删除配置
     */
    suspend fun deleteConfig(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 获取当前用户的数据
            val currentUserData = GitDataManager.getUserData(currentUserId)

            // 更新配置列表
            val currentConfigs = _configs.value
            val updatedConfigs = currentConfigs.filter { it.id != id }

            // 更新状态
            _configs.value = updatedConfigs

            // 断开连接
            clients[id]?.disconnect()
            clients.remove(id)

            // 保存到Git
            val updatedUserData = currentUserData.copy(hublinkConfigs = updatedConfigs)
            GitDataManager.saveCurrentUserData(currentUserId, updatedUserData).getOrThrow()

            // 更新合并目录，确保UI能看到最新数据
            GitDataManager.mergeAllUserData()

            println("✅ 删除代理配置成功")
            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ 删除代理配置失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 获取客户端管理器
     */
    fun getClientManager(configId: String): service.HubLinkClientManager? {
        val config = _configs.value.find { it.id == configId } ?: return null
        return clients.getOrPut(configId) { service.HubLinkClientManager(config) }
    }

    /**
     * 创建新配置
     */
    fun createConfig(
        name: String,
        host: String,
        port: Int = 6180,
        localPort: Int = 0,
        psk: String,
        transport: HubLinkTransportType = HubLinkTransportType.DIRECT,
        mqttConfig: HubLinkMqttConfig? = null,
        obfs: String? = null,
        obfsHost: String? = null
    ): HubLinkConfig {
        return HubLinkConfig(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            host = host,
            port = port,
            localPort = localPort,
            psk = psk,
            transport = transport,
            mqttConfig = mqttConfig,
            obfs = obfs,
            obfsHost = obfsHost
        )
    }

    /**
     * 清理所有连接
     */
    suspend fun cleanup() {
        clients.values.forEach { it.disconnect() }
        clients.clear()
    }

    /**
     * 删除客户端管理器
     */
    suspend fun removeClientManager(configId: String) {
        clients[configId]?.disconnect()
        clients.remove(configId)
    }
}
