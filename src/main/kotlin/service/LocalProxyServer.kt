package service

import kotlinx.coroutines.*
import utils.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer

/**
 * 本地代理服务器 (SOCKS5/HTTP)
 */
class LocalProxyServer(
    private val hubLinkClient: HubLinkClientManager,
    private val port: Int
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    /**
     * 启动代理服务器
     */
    fun start() {
        serverJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                serverSocket = ServerSocket(port)
                Logger.info("HubLink: 本地代理服务器启动在端口 $port")

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let {
                            launch { handleClient(it) }
                        }
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            Logger.error("HubLink: 接受客户端连接失败", e)
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Logger.error("HubLink: 启动本地代理服务器失败", e)
            }
        }
    }

    /**
     * 停止代理服务器
     */
    fun stop() {
        serverJob?.cancel()
        serverSocket?.close()
        serverSocket = null
        Logger.info("HubLink: 本地代理服务器已停止")
    }

    /**
     * 处理客户端连接
     */
    private suspend fun handleClient(clientSocket: Socket) = withContext(Dispatchers.IO) {
        try {
            Logger.debug("HubLink: 新客户端连接: ${clientSocket.remoteSocketAddress}")

            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            // 读取客户端请求类型
            val buffer = ByteArray(1)
            val bytesRead = input.read(buffer)
            if (bytesRead <= 0) return@withContext

            when (buffer[0].toInt()) {
                0x05 -> handleSocks5(clientSocket, input, output)
                'C'.code, 'G'.code, 'P'.code, 'H'.code, 'D'.code -> {
                    // HTTP方法开头，回退并处理HTTP
                    handleHttp(clientSocket, buffer[0], input, output)
                }
                else -> {
                    Logger.warn("HubLink: 未知协议类型: ${buffer[0]}")
                    clientSocket.close()
                }
            }

        } catch (e: Exception) {
            Logger.error("HubLink: 处理客户端连接异常", e)
        } finally {
            try {
                clientSocket.close()
            } catch (e: Exception) {
                // 忽略关闭异常
            }
        }
    }

    /**
     * 处理SOCKS5协议
     */
    private suspend fun handleSocks5(clientSocket: Socket, input: java.io.InputStream, output: java.io.OutputStream) {
        try {
            // SOCKS5握手
            val version = 0x05
            val nMethods = input.read()
            val methods = ByteArray(nMethods)
            input.read(methods)

            // 响应：无认证
            output.write(byteArrayOf(version.toByte(), 0x00))
            output.flush()

            // 读取连接请求
            val request = ByteArray(4)
            input.read(request)

            val cmd = request[1].toInt()
            if (cmd != 0x01) { // 只支持CONNECT
                sendSocks5Error(output, 0x07) // Command not supported
                return
            }

            // 跳过RSV
            input.read()

            // 读取地址类型
            val addrType = input.read()
            val (host, port) = when (addrType) {
                0x01 -> { // IPv4
                    val addr = ByteArray(4)
                    input.read(addr)
                    val portBytes = ByteArray(2)
                    input.read(portBytes)
                    val port = ByteBuffer.wrap(portBytes).short.toInt() and 0xFFFF
                    InetAddress.getByAddress(addr).hostAddress to port
                }
                0x03 -> { // 域名
                    val len = input.read()
                    val domain = ByteArray(len)
                    input.read(domain)
                    val portBytes = ByteArray(2)
                    input.read(portBytes)
                    val port = ByteBuffer.wrap(portBytes).short.toInt() and 0xFFFF
                    String(domain) to port
                }
                0x04 -> { // IPv6
                    val addr = ByteArray(16)
                    input.read(addr)
                    val portBytes = ByteArray(2)
                    input.read(portBytes)
                    val port = ByteBuffer.wrap(portBytes).short.toInt() and 0xFFFF
                    InetAddress.getByAddress(addr).hostAddress to port
                }
                else -> {
                    sendSocks5Error(output, 0x08) // Address type not supported
                    return
                }
            }

            Logger.debug("HubLink: SOCKS5请求连接 $host:$port")

            // 发送连接成功响应
            output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            output.flush()

            // 通过HubLink转发数据
            forwardData(clientSocket, host, port)

        } catch (e: Exception) {
            Logger.error("HubLink: SOCKS5处理异常", e)
            try {
                sendSocks5Error(output, 0x01) // General SOCKS server failure
            } catch (ignore: Exception) {}
        }
    }

    /**
     * 处理HTTP协议
     */
    private suspend fun handleHttp(clientSocket: Socket, firstByte: Byte, input: java.io.InputStream, output: java.io.OutputStream) {
        try {
            // 读取HTTP请求头
            val requestBuffer = ByteArrayOutputStream()
            requestBuffer.write(firstByte.toInt())

            val buffer = ByteArray(1)
            var lineBuffer = StringBuilder()

            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break

                requestBuffer.write(buffer[0].toInt())
                lineBuffer.append(buffer[0].toChar())

                // 检查是否读完请求头
                if (lineBuffer.endsWith("\r\n\r\n")) {
                    break
                }
            }

            val requestData = requestBuffer.toByteArray()
            Logger.debug("HubLink: HTTP请求大小: ${requestData.size} bytes")

            // 通过HubLink转发HTTP请求
            val response = hubLinkClient.forwardRequest(requestData).getOrThrow()
            output.write(response)
            output.flush()
            Logger.debug("HubLink: HTTP响应大小: ${response.size} bytes")

        } catch (e: Exception) {
            Logger.error("HubLink: HTTP处理异常", e)
        }
    }

    /**
     * 转发数据流
     */
    private suspend fun forwardData(clientSocket: Socket, targetHost: String, targetPort: Int) = withContext(Dispatchers.IO) {
        try {
            // 构建连接请求
            val connectRequest = buildConnectRequest(targetHost, targetPort)
            Logger.debug("HubLink: 发送连接请求到 $targetHost:$targetPort")

            // 发送连接请求到服务端
            val connectResponse = hubLinkClient.forwardRequest(connectRequest).getOrThrow()

            Logger.debug("HubLink: 连接响应: ${connectResponse.joinToString("") { "%02x".format(it) }}")

            // 解析响应
            if (!isConnectionSuccess(connectResponse)) {
                Logger.warn("HubLink: 连接请求被拒绝")
                return@withContext
            }

            Logger.debug("HubLink: 连接建立成功，开始转发数据")

            // 开始双向数据转发
            val clientInput = clientSocket.getInputStream()
            val clientOutput = clientSocket.getOutputStream()

            // 启动上行数据转发 (客户端 -> 服务端)
            val upstreamJob = launch {
                try {
                    val buffer = ByteArray(8192)
                    while (isActive) {
                        val len = clientInput.read(buffer)
                        if (len == -1) break

                        val data = buffer.copyOf(len)
                        val response = hubLinkClient.forwardRequest(data).getOrNull()
                        if (response == null) {
                            Logger.warn("HubLink: 上行数据转发失败")
                            break
                        }
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Logger.error("HubLink: 上行数据转发异常", e)
                    }
                }
            }

            // 启动下行数据转发 (服务端 -> 客户端)
            val downstreamJob = launch {
                try {
                    while (isActive) {
                        // 这里需要实现从服务端接收数据的逻辑
                        // 暂时模拟
                        delay(100)
                    }
                } catch (e: Exception) {
                    if (e !is CancellationException) {
                        Logger.error("HubLink: 下行数据转发异常", e)
                    }
                }
            }

            // 等待任一方向结束
            listOf(upstreamJob, downstreamJob).joinAll()

        } catch (e: Exception) {
            Logger.error("HubLink: 数据转发异常", e)
        }
    }

    /**
     * 发送SOCKS5错误响应
     */
    private fun sendSocks5Error(output: java.io.OutputStream, errorCode: Byte) {
        try {
            output.write(byteArrayOf(0x05, errorCode, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
            output.flush()
        } catch (e: Exception) {
            // 忽略
        }
    }

    /**
     * 构建连接请求
     */
    private fun buildConnectRequest(host: String, port: Int): ByteArray {
        // 构建简单的连接请求协议
        val hostBytes = host.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + 2 + 2 + hostBytes.size)
        buffer.put(0x01) // CONNECT命令
        buffer.putShort(hostBytes.size.toShort())
        buffer.put(hostBytes)
        buffer.putShort(port.toShort())
        return buffer.array()
    }

    /**
     * 检查连接响应是否成功
     */
    private fun isConnectionSuccess(response: ByteArray): Boolean {
        return response.isNotEmpty() && response[0].toInt() == 0x00 // 成功标记
    }
}
