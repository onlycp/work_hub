package service

import data.HubLinkMqttConfig
import data.HubLinkTransportType
import kotlinx.coroutines.*
import utils.Logger
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * HubLink传输接口
 */
interface HubLinkTransport {
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun send(data: ByteArray): Result<Unit>
    suspend fun receive(): Result<ByteArray>
    fun isConnected(): Boolean
}

/**
 * 直接TCP传输实现
 */
class DirectTransport(private val host: String, private val port: Int) : HubLinkTransport {
    private var socket: Socket? = null

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            socket = Socket(host, port)
            Logger.info("HubLink: 直接TCP连接成功 $host:$port")
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.error("HubLink: 直接TCP连接失败", e)
            Result.failure(e)
        }
    }

    override suspend fun disconnect() {
        socket?.close()
        socket = null
    }

    override suspend fun send(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            socket?.getOutputStream()?.write(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun receive(): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val input = socket?.getInputStream()
            val buffer = ByteArray(8192)
            val len = input?.read(buffer) ?: -1
            if (len > 0) {
                Result.success(buffer.copyOf(len))
            } else {
                Result.failure(Exception("连接已关闭"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean = socket?.isConnected == true
}

/**
 * MQTT传输实现 (简化版)
 */
class MqttTransport(private val config: HubLinkMqttConfig) : HubLinkTransport {
    // TODO: 实现完整的MQTT客户端
    // 这里提供简化版本，后续可以集成mqtt-client库

    override suspend fun connect(): Result<Unit> {
        // TODO: 实现MQTT连接
        Logger.info("HubLink: MQTT连接暂未实现 $config")
        return Result.failure(Exception("MQTT传输暂未实现"))
    }

    override suspend fun disconnect() {
        // TODO: 实现MQTT断开
    }

    override suspend fun send(data: ByteArray): Result<Unit> {
        // TODO: 实现MQTT发送
        return Result.failure(Exception("MQTT传输暂未实现"))
    }

    override suspend fun receive(): Result<ByteArray> {
        // TODO: 实现MQTT接收
        return Result.failure(Exception("MQTT传输暂未实现"))
    }

    override fun isConnected(): Boolean = false
}

/**
 * HubLink加密器
 */
class HubLinkCipher(psk: String) {
    private val key: ByteArray
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")

    init {
        // 从PSK派生密钥
        val sha256 = MessageDigest.getInstance("SHA-256")
        key = sha256.digest(psk.toByteArray(Charsets.UTF_8))
    }

    fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val nonce = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val encrypted = cipher.doFinal(data)

        return nonce to encrypted
    }

    fun decrypt(nonce: ByteArray, encryptedData: ByteArray): ByteArray {
        val secretKey = SecretKeySpec(key, "AES")
        val gcmSpec = GCMParameterSpec(128, nonce)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        return cipher.doFinal(encryptedData)
    }
}

/**
 * HubLink协议处理器
 */
class HubLinkProtocol(private val transport: HubLinkTransport, private val psk: String) {
    private val cipher = HubLinkCipher(psk)

    suspend fun performHandshake(): Result<Unit> {
        try {
            // 生成客户端nonce
            val clientNonce = ByteArray(12).apply { SecureRandom().nextBytes(this) }

        // 计算PSK哈希
        val sha256 = MessageDigest.getInstance("SHA-256")
        val pskHash = sha256.digest(psk.toByteArray(Charsets.UTF_8))
        val psk = psk

            // 发送握手请求: VERSION(1) + PSK_HASH(32) + CLIENT_NONCE(12)
            val handshakeRequest = ByteBuffer.allocate(1 + 32 + 12)
            handshakeRequest.put(1) // 版本
            handshakeRequest.put(pskHash)
            handshakeRequest.put(clientNonce)

            transport.send(handshakeRequest.array()).getOrThrow()

            // 读取服务端响应: SUCCESS(1) + SERVER_NONCE(12) + ENCRYPTED_CHALLENGE(48)
            val response = transport.receive().getOrThrow()
            val buffer = ByteBuffer.wrap(response)

            val success = buffer.get()
            if (success.toInt() != 1) {
                return Result.failure(Exception("握手失败: 服务端拒绝"))
            }

            val serverNonce = ByteArray(12)
            buffer.get(serverNonce)

            val encryptedChallenge = ByteArray(48)
            buffer.get(encryptedChallenge)

            // 解密挑战数据验证
            val challenge = cipher.decrypt(serverNonce, encryptedChallenge)
            // TODO: 验证挑战数据内容

            // 发送握手完成响应
            val (responseNonce, encryptedResponse) = cipher.encrypt(challenge)
            val completeResponse = ByteBuffer.allocate(12 + encryptedResponse.size)
            completeResponse.put(responseNonce)
            completeResponse.put(encryptedResponse)

            transport.send(completeResponse.array()).getOrThrow()

            Logger.info("HubLink: 握手完成")
            return Result.success(Unit)

        } catch (e: Exception) {
            Logger.error("HubLink: 握手失败", e)
            return Result.failure(e)
        }
    }

    suspend fun sendData(data: ByteArray): Result<Unit> {
        return try {
            val (nonce, encrypted) = cipher.encrypt(data)

            // 发送: LENGTH(2) + NONCE(12) + ENCRYPTED_DATA
            val buffer = ByteBuffer.allocate(2 + 12 + encrypted.size)
            buffer.putShort((12 + encrypted.size).toShort())
            buffer.put(nonce)
            buffer.put(encrypted)

            transport.send(buffer.array())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun receiveData(): Result<ByteArray> {
        return try {
            val lengthBuf = transport.receive().getOrThrow()
            val length = ByteBuffer.wrap(lengthBuf).getShort()

            val dataBuf = transport.receive().getOrThrow()
            val buffer = ByteBuffer.wrap(dataBuf)

            val nonce = ByteArray(12)
            buffer.get(nonce)

            val encryptedData = ByteArray(length - 12)
            buffer.get(encryptedData)

            val decrypted = cipher.decrypt(nonce, encryptedData)
            Result.success(decrypted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
