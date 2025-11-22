package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/**
 * 首页显示配置数据类
 */
@Serializable
data class IndexConfig(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,                           // 用户ID
    val visibleProxyIds: Set<String> = emptySet(), // 显示的代理ID列表
    val visibleHostIds: Set<String> = emptySet(),  // 显示的主机ID列表
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * 首页配置管理器
 */
object IndexConfigManager {
    private var currentUserId: String = ""
    private val _config = MutableStateFlow<IndexConfig?>(null)
    val config: StateFlow<IndexConfig?> = _config.asStateFlow()

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
        loadConfig()
    }

    /**
     * 加载配置
     */
    private fun loadConfig() {
        if (currentUserId.isEmpty()) return

        try {
            // 使用GitDataManager的方式获取用户目录
            val workhubDir = File(System.getProperty("user.home"), ".workhub")
            val userDir = File(workhubDir, "users/${currentUserId}")
            val configFile = File(userDir, "index.json")

            if (configFile.exists()) {
                val json = configFile.readText()
                val loadedConfig = Json.decodeFromString<IndexConfig>(json)
                _config.value = loadedConfig
                println("✅ 加载首页配置成功")
            } else {
                // 创建默认配置
                val defaultConfig = IndexConfig(userId = currentUserId)
                _config.value = defaultConfig
                saveConfig(defaultConfig)
            }
        } catch (e: Exception) {
            println("❌ 加载首页配置失败: ${e.message}")
            // 创建默认配置
            val defaultConfig = IndexConfig(userId = currentUserId)
            _config.value = defaultConfig
        }
    }

    /**
     * 保存配置
     */
    private fun saveConfig(config: IndexConfig) {
        try {
            // 使用GitDataManager的方式获取用户目录
            val workhubDir = File(System.getProperty("user.home"), ".workhub")
            val userDir = File(workhubDir, "users/${currentUserId}")
            userDir.mkdirs() // 确保目录存在
            val configFile = File(userDir, "index.json")

            val json = Json.encodeToString(config)
            configFile.writeText(json)
            _config.value = config
            println("✅ 保存首页配置成功")
        } catch (e: Exception) {
            println("❌ 保存首页配置失败: ${e.message}")
        }
    }

    /**
     * 更新显示的代理列表
     */
    fun updateVisibleProxies(proxyIds: Set<String>) {
        val currentConfig = _config.value ?: return
        val updatedConfig = currentConfig.copy(
            visibleProxyIds = proxyIds,
            lastModified = System.currentTimeMillis()
        )
        saveConfig(updatedConfig)
    }

    /**
     * 更新显示的主机列表
     */
    fun updateVisibleHosts(hostIds: Set<String>) {
        val currentConfig = _config.value ?: return
        val updatedConfig = currentConfig.copy(
            visibleHostIds = hostIds,
            lastModified = System.currentTimeMillis()
        )
        saveConfig(updatedConfig)
    }

    /**
     * 获取当前显示的代理ID列表
     */
    fun getVisibleProxyIds(): Set<String> {
        return _config.value?.visibleProxyIds ?: emptySet()
    }

    /**
     * 获取当前显示的主机ID列表
     */
    fun getVisibleHostIds(): Set<String> {
        return _config.value?.visibleHostIds ?: emptySet()
    }

    /**
     * 检查代理是否应该显示
     */
    fun shouldShowProxy(proxyId: String): Boolean {
        val visibleIds = getVisibleProxyIds()
        return visibleIds.isEmpty() || visibleIds.contains(proxyId)
    }

    /**
     * 检查主机是否应该显示
     */
    fun shouldShowHost(hostId: String): Boolean {
        val visibleIds = getVisibleHostIds()
        return visibleIds.isEmpty() || visibleIds.contains(hostId)
    }
}
