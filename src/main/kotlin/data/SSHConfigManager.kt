package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * SSH配置管理器
 * 通过Git管理多用户分支数据
 */
object SSHConfigManager {
    private var currentUserId: String = ""
    private var _configs = mutableListOf<SSHConfigData>()

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
        loadUserConfigs()
    }

    /**
     * 加载当前用户的配置
     */
    private fun loadUserConfigs() {
        if (currentUserId.isEmpty()) return

        try {
            val mergedData = GitDataManager.getAllMergedData()
            _configs = mergedData.sshConfigs
                .filter { PermissionManager.canView(it.shareableData) }
                .toMutableList()
        } catch (e: Exception) {
            println("加载SSH配置失败: ${e.message}")
            _configs = mutableListOf()
        }
    }

    /**
     * 获取所有配置（带权限过滤）
     */
    fun getAllConfigs(): List<SSHConfigData> {
        return _configs.filter { PermissionManager.canView(it.shareableData) }
    }

    /**
     * 根据ID获取配置
     */
    fun getConfigById(id: String): SSHConfigData? {
        return _configs.find { it.id == id }?.takeIf { PermissionManager.canView(it.shareableData) }
    }

    /**
     * 根据名称获取配置
     */
    fun getConfigByName(name: String): SSHConfigData? {
        return _configs.find { it.name == name }?.takeIf { PermissionManager.canView(it.shareableData) }
    }

    /**
     * 获取所有分组名称
     */
    fun getAllGroups(): List<String> {
        return getAllConfigs().map { it.group }.distinct().sorted()
    }

    /**
     * 根据分组获取配置
     */
    fun getConfigsByGroup(group: String): List<SSHConfigData> {
        return getAllConfigs().filter { it.group == group }.sortedBy { it.name }
    }

    /**
     * 按分组组织的配置
     */
    fun getConfigsGrouped(): Map<String, List<SSHConfigData>> {
        return getAllConfigs().groupBy { it.group }.toSortedMap()
    }

    /**
     * 添加新配置
     */
    suspend fun addConfig(config: SSHConfigData): Result<SSHConfigData> = withContext(Dispatchers.IO) {
        try {
            // 创建时自动设置创建者为当前用户
            val newConfig = config.copy(createdBy = CurrentUserManager.getCurrentUserId())

            // 检查名称是否重复
            if (getAllConfigs().any { it.name == newConfig.name }) {
                return@withContext Result.failure(Exception("配置名称已存在"))
            }

            _configs.add(newConfig)
            saveConfigs()
            Result.success(newConfig)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新配置 - 只允许创建者修改
     */
    suspend fun updateConfig(id: String, updatedConfig: SSHConfigData): Result<SSHConfigData> = withContext(Dispatchers.IO) {
        try {
            val existing = _configs.find { it.id == id }
            if (existing == null) {
                return@withContext Result.failure(Exception("配置不存在"))
            }

            // 检查编辑权限
            if (!PermissionManager.canEdit(existing.baseData)) {
                return@withContext Result.failure(Exception("无权限编辑此配置"))
            }

            // 检查名称是否重复
            if (getAllConfigs().any { it.id != id && it.name == updatedConfig.name }) {
                return@withContext Result.failure(Exception("配置名称已存在"))
            }

            val updated = updatedConfig.copy(
                id = id,
                createdBy = existing.createdBy, // 保持创建者不变
                lastModified = System.currentTimeMillis()
            )
            val index = _configs.indexOfFirst { it.id == id }
            _configs[index] = updated
            saveConfigs()
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除配置 - 只允许创建者删除
     */
    suspend fun deleteConfig(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _configs.find { it.id == id }
            if (existing == null) {
                return@withContext Result.failure(Exception("配置不存在"))
            }

            // 检查删除权限
            if (!PermissionManager.canDelete(existing.baseData)) {
                return@withContext Result.failure(Exception("无权限删除此配置"))
            }

            val removed = _configs.removeIf { it.id == id }
            if (!removed) {
                return@withContext Result.failure(Exception("配置不存在"))
            }
            saveConfigs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 共享配置 - 只允许创建者共享
     */
    suspend fun shareConfig(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _configs.find { it.id == id }
            if (existing == null) {
                return@withContext Result.failure(Exception("配置不存在"))
            }

            // 检查共享权限
            if (!PermissionManager.canShare(existing.shareableData)) {
                return@withContext Result.failure(Exception("无权限共享此配置"))
            }

            val updated = existing.copy(isShared = true)
            val index = _configs.indexOfFirst { it.id == id }
            _configs[index] = updated
            saveConfigs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 取消共享配置 - 只允许创建者操作
     */
    suspend fun unshareConfig(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _configs.find { it.id == id }
            if (existing == null) {
                return@withContext Result.failure(Exception("配置不存在"))
            }

            // 检查权限
            if (!PermissionManager.canShare(existing.shareableData)) {
                return@withContext Result.failure(Exception("无权限取消共享此配置"))
            }

            val updated = existing.copy(isShared = false)
            val index = _configs.indexOfFirst { it.id == id }
            _configs[index] = updated
            saveConfigs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 转换为SSHConfig
     */
    fun SSHConfigData.toSSHConfig(): SSHConfig {
        return SSHConfig(
            name = name,
            host = host,
            port = port,
            username = username,
            password = password,
            privateKeyPath = privateKeyPath,
            privateKeyPassphrase = privateKeyPassphrase
        )
    }

    /**
     * 从SSHConfig创建SSHConfigData
     */
    fun createConfigData(config: SSHConfig, createdBy: String): SSHConfigData {
        return SSHConfigData(
            id = java.util.UUID.randomUUID().toString(),
            name = config.name,
            host = config.host,
            port = config.port,
            username = config.username,
            password = config.password,
            privateKeyPath = config.privateKeyPath,
            group = "默认分组",
            portForwardingRules = emptyList(),
            createdBy = createdBy,
            isShared = false
        )
    }

    /**
     * 更新配置的端口转发规则
     */
    suspend fun updatePortForwardingRules(configName: String, rules: List<PortForwardingRuleData>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val index = _configs.indexOfFirst { it.name == configName }
            if (index < 0) {
                return@withContext Result.failure(Exception("配置不存在: $configName"))
            }

            val oldConfig = _configs[index]
            val newConfig = oldConfig.copy(portForwardingRules = rules)
            _configs[index] = newConfig
            saveConfigs()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新配置的命令规则
     */
    suspend fun updateCommandRules(configName: String, rules: List<CommandRuleData>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val index = _configs.indexOfFirst { it.name == configName }
            if (index < 0) {
                return@withContext Result.failure(Exception("配置不存在: $configName"))
            }

            val oldConfig = _configs[index]
            val newConfig = oldConfig.copy(commandRules = rules)
            _configs[index] = newConfig
            saveConfigs()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 保存配置到Git
     */
    private suspend fun saveConfigs() {
        try {
            // 获取当前用户的所有数据
            val currentUserData = GitDataManager.getUserData(currentUserId)

            // 更新SSH配置
            val updatedUserData = currentUserData.copy(sshConfigs = _configs)

            // 保存到Git
            GitDataManager.saveCurrentUserData(currentUserId, updatedUserData)

            println("SSH配置已保存到Git")
        } catch (e: Exception) {
            println("保存SSH配置失败: ${e.message}")
            e.printStackTrace()
        }
    }
}

