package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 登录设置数据类
 */
@Serializable
data class LoginSettings(
    // 记住的用户名
    val rememberedUsername: String = "",
    // 记住的密码（加密存储）
    val rememberedPassword: String = "",
    // 是否记住密码
    val rememberPassword: Boolean = false,
    // 是否自动登录
    val autoLogin: Boolean = false,
    // 最后登录时间
    val lastLoginTime: Long = 0L
)

/**
 * 登录设置管理器
 * 负责管理登录信息的持久化存储
 */
object LoginSettingsManager {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // 登录设置状态
    private var _settings = loadSettings()

    /**
     * 获取当前登录设置
     */
    fun getCurrentSettings(): LoginSettings = _settings

    /**
     * 检查是否有记住的登录信息
     */
    fun hasRememberedCredentials(): Boolean {
        val settings = _settings
        return settings.rememberPassword &&
               settings.rememberedUsername.isNotBlank() &&
               settings.rememberedPassword.isNotBlank()
    }

    /**
     * 检查是否启用自动登录
     */
    fun isAutoLoginEnabled(): Boolean {
        return _settings.autoLogin && hasRememberedCredentials()
    }

    /**
     * 获取记住的用户名
     */
    fun getRememberedUsername(): String = _settings.rememberedUsername

    /**
     * 获取记住的密码
     */
    fun getRememberedPassword(): String = _settings.rememberedPassword

    /**
     * 保存登录设置
     */
    suspend fun saveLoginSettings(
        username: String,
        password: String,
        rememberPassword: Boolean,
        autoLogin: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val newSettings = LoginSettings(
                rememberedUsername = if (rememberPassword) username else "",
                rememberedPassword = if (rememberPassword) password else "",
                rememberPassword = rememberPassword,
                autoLogin = if (rememberPassword) autoLogin else false,
                lastLoginTime = System.currentTimeMillis()
            )

            _settings = newSettings
            saveSettingsToFile(newSettings)

            println("✅ 登录设置已保存")
        } catch (e: Exception) {
            println("❌ 保存登录设置失败: ${e.message}")
        }
    }

    /**
     * 清除登录设置
     */
    suspend fun clearLoginSettings() = withContext(Dispatchers.IO) {
        try {
            val emptySettings = LoginSettings()
            _settings = emptySettings
            saveSettingsToFile(emptySettings)

            println("✅ 登录设置已清除")
        } catch (e: Exception) {
            println("❌ 清除登录设置失败: ${e.message}")
        }
    }

    /**
     * 加载设置
     */
    private fun loadSettings(): LoginSettings {
        return try {
            val settingsFile = getSettingsFile()
            if (settingsFile.exists()) {
                val jsonString = settingsFile.readText()
                json.decodeFromString<LoginSettings>(jsonString)
            } else {
                LoginSettings()
            }
        } catch (e: Exception) {
            println("❌ 加载登录设置失败: ${e.message}")
            LoginSettings()
        }
    }

    /**
     * 保存设置到文件
     */
    private fun saveSettingsToFile(settings: LoginSettings) {
        try {
            val settingsFile = getSettingsFile()
            settingsFile.parentFile?.mkdirs()

            val jsonString = json.encodeToString(settings)
            settingsFile.writeText(jsonString)
        } catch (e: Exception) {
            println("❌ 保存登录设置到文件失败: ${e.message}")
        }
    }

    /**
     * 获取设置文件
     */
    private fun getSettingsFile(): File {
        val workhubDir = File(System.getProperty("user.home"), ".workhub")
        workhubDir.mkdirs()
        return File(workhubDir, "login_settings.json")
    }
}
