package data

import kotlinx.serialization.Serializable
import java.util.UUID

// 确保KeyManager被初始化
private val keyManagerInit = KeyManager

/**
 * 端口转发规则数据类
 */
@Serializable
data class PortForwardingRuleData(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "LOCAL", "REMOTE", "DYNAMIC"
    val localPort: Int,
    val remoteHost: String = "localhost",
    val remotePort: Int = 0,
    val description: String = "",
    val autoStart: Boolean = false // 是否自动启动
)

/**
 * 命令规则数据类
 */
@Serializable
data class CommandRuleData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val script: String,
    val logFile: String = "",
    val remarks: String = "",
    val workingDirectory: String = "", // 工作目录，可为空
    val autoStart: Boolean = false // 是否自动启动
)

/**
 * SSH配置数据类
 */
@Serializable
data class SSHConfigData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String = "",
    val privateKeyPath: String = "",
    val privateKeyPassphrase: String = "", // 私钥密码
    val keyId: String? = null, // 关联的密钥ID，为空表示使用直接配置的认证信息
    val group: String = "默认分组",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val portForwardingRules: List<PortForwardingRuleData> = emptyList(),
    val commandRules: List<CommandRuleData> = emptyList(),
    // 创建者ID - 只有创建者能编辑
    val createdBy: String,
    // 是否共享给其他用户查看
    val isShared: Boolean = false
)

/**
 * SSH配置对象（用于连接）
 */
data class SSHConfig(
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val password: String = "",
    val privateKeyPath: String = "",
    val privateKeyPassphrase: String = ""
) {
    companion object {
        /**
         * 从SSHConfigData创建SSHConfig，支持密钥引用
         */
        fun fromSSHConfigData(configData: SSHConfigData): SSHConfig {
            // 如果有关联的密钥，使用密钥信息
            val keyData = configData.keyId?.let { KeyManager.getKeyById(it) }

            return if (keyData != null) {
                // 使用密钥认证
                SSHConfig(
                    name = configData.name,
                    host = configData.host,
                    port = configData.port,
                    username = keyData.username,
                    password = if (keyData.authType == AuthType.PASSWORD) keyData.password else "",
                    privateKeyPath = if (keyData.authType == AuthType.KEY) keyData.privateKeyContent else "",
                    privateKeyPassphrase = if (keyData.authType == AuthType.KEY) keyData.privateKeyPassphrase else ""
                )
            } else {
                // 使用直接配置的认证信息（向后兼容）
                SSHConfig(
                    name = configData.name,
                    host = configData.host,
                    port = configData.port,
                    username = configData.username,
                    password = configData.password,
                    privateKeyPath = configData.privateKeyPath,
                    privateKeyPassphrase = configData.privateKeyPassphrase
                )
            }
        }
    }
}

