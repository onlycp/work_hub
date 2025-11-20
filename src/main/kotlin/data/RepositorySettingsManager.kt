package data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 仓库设置管理器
 * 管理GIT仓库配置信息
 */
object RepositorySettingsManager {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    // 仓库设置状态
    private val _settings = MutableStateFlow<RepositorySettings>(RepositorySettings())
    val settings: StateFlow<RepositorySettings> = _settings

    init {
        loadSettings()
    }

    /**
     * 获取当前设置
     */
    fun getCurrentSettings(): RepositorySettings = _settings.value

    /**
     * 检查仓库设置是否完整有效
     */
    fun isRepositoryConfigured(): Boolean {
        val settings = _settings.value
        return settings.enabled &&
               settings.repositoryUrl.isNotBlank() &&
               settings.username.isNotBlank() &&
               settings.password.isNotBlank()
    }

    /**
     * 更新设置
     */
    fun updateSettings(newSettings: RepositorySettings) {
        _settings.value = newSettings
        saveSettings(newSettings)
    }

    /**
     * 加载设置
     */
    private fun loadSettings() {
        try {
            val settingsFile = getSettingsFile()
            if (settingsFile.exists()) {
                val jsonString = settingsFile.readText()
                val loadedSettings = json.decodeFromString<RepositorySettings>(jsonString)
                _settings.value = loadedSettings
            }
        } catch (e: Exception) {
            println("❌ 加载仓库设置失败: ${e.message}")
        }
    }

    /**
     * 保存设置
     */
    private fun saveSettings(settings: RepositorySettings) {
        try {
            val settingsFile = getSettingsFile()
            settingsFile.parentFile?.mkdirs()

            val jsonString = json.encodeToString(settings)
            settingsFile.writeText(jsonString)

            println("✅ 仓库设置已保存")
        } catch (e: Exception) {
            println("❌ 保存仓库设置失败: ${e.message}")
        }
    }

    /**
     * 获取设置文件
     */
    private fun getSettingsFile(): File {
        val workhubDir = File(System.getProperty("user.home"), ".workhub")
        workhubDir.mkdirs()
        return File(workhubDir, "repository_settings.json")
    }
}

/**
 * 仓库设置数据类
 */
@Serializable
data class RepositorySettings(
    // 仓库地址
    val repositoryUrl: String = "",
    // 用户名
    val username: String = "",
    // 密码/Token
    val password: String = "",
    // 分支
    val branch: String = "main",
    // 是否启用
    val enabled: Boolean = false
)
