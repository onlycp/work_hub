package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.Result

/**
 * 命令管理器
 * 负责命令规则的管理和状态维护
 */
object CommandManager {
    // 当前选中的SSH配置ID
    private var currentConfigId: String? = null

    // 命令规则状态流
    private val _commandRules = MutableStateFlow<List<CommandRuleData>>(emptyList())
    val commandRules: StateFlow<List<CommandRuleData>> = _commandRules.asStateFlow()

    // 命令执行状态（规则ID -> 是否正在执行）
    private val _commandStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val commandStatuses: StateFlow<Map<String, Boolean>> = _commandStatuses.asStateFlow()

    /**
     * 设置当前SSH配置
     */
    fun setCurrentConfig(configId: String?) {
        currentConfigId = configId
        if (configId != null) {
            val config = SSHConfigManager.getConfigById(configId)
            _commandRules.value = config?.commandRules ?: emptyList()
        } else {
            _commandRules.value = emptyList()
        }
        // 重置所有命令状态
        _commandStatuses.value = emptyMap()
    }

    /**
     * 获取当前配置ID
     */
    fun getCurrentConfigId(): String? {
        return currentConfigId
    }

    /**
     * 获取当前配置的所有命令规则
     */
    fun getCurrentCommandRules(): List<CommandRuleData> {
        return _commandRules.value
    }

    /**
     * 添加命令规则
     */
    suspend fun addCommandRule(rule: CommandRuleData): Result<CommandRuleData> = withContext(Dispatchers.IO) {
        try {
            val currentConfig = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
                ?: return@withContext Result.failure(Exception("未选择SSH配置"))

            // 权限检查：如果SSH配置是共享的且不是当前用户创建的，则不允许操作
            if (!PermissionManager.canEditCommandRules(currentConfig)) {
                return@withContext Result.failure(Exception("无权限操作此主机的命令规则"))
            }

            val updatedRules = _commandRules.value + rule
            val result = SSHConfigManager.updateCommandRules(currentConfig.name, updatedRules)

            if (result.isSuccess) {
                _commandRules.value = updatedRules
                Result.success(rule)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新命令规则
     */
    suspend fun updateCommandRule(ruleId: String, updatedRule: CommandRuleData): Result<CommandRuleData> = withContext(Dispatchers.IO) {
        try {
            val currentConfig = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
                ?: return@withContext Result.failure(Exception("未选择SSH配置"))

            // 权限检查：如果SSH配置是共享的且不是当前用户创建的，则不允许操作
            if (!PermissionManager.canEditCommandRules(currentConfig)) {
                return@withContext Result.failure(Exception("无权限操作此主机的命令规则"))
            }

            val updatedRules = _commandRules.value.map { if (it.id == ruleId) updatedRule else it }
            val result = SSHConfigManager.updateCommandRules(currentConfig.name, updatedRules)

            if (result.isSuccess) {
                _commandRules.value = updatedRules
                // 如果规则状态有变化，需要更新状态
                val currentStatuses = _commandStatuses.value
                if (currentStatuses.containsKey(ruleId)) {
                    // 保持原有状态，除非规则有重大变化
                    _commandStatuses.value = currentStatuses + (ruleId to (currentStatuses[ruleId] ?: false))
                }
                Result.success(updatedRule)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除命令规则
     */
    suspend fun deleteCommandRule(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentConfig = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
                ?: return@withContext Result.failure(Exception("未选择SSH配置"))

            // 权限检查：如果SSH配置是共享的且不是当前用户创建的，则不允许操作
            if (!PermissionManager.canEditCommandRules(currentConfig)) {
                return@withContext Result.failure(Exception("无权限操作此主机的命令规则"))
            }

            val updatedRules = _commandRules.value.filter { it.id != ruleId }
            val result = SSHConfigManager.updateCommandRules(currentConfig.name, updatedRules)

            if (result.isSuccess) {
                _commandRules.value = updatedRules
                // 移除对应的状态
                _commandStatuses.value = _commandStatuses.value - ruleId
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取命令执行状态
     */
    fun getCommandRuleStatus(ruleId: String): Boolean {
        return _commandStatuses.value[ruleId] ?: false
    }

    /**
     * 设置命令执行状态
     */
    fun setCommandRuleStatus(ruleId: String, isExecuting: Boolean) {
        _commandStatuses.value = _commandStatuses.value + (ruleId to isExecuting)
    }

    /**
     * 停止所有命令执行
     */
    suspend fun stopAllCommandExecution(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // TODO: 实现停止所有命令执行的逻辑
            _commandStatuses.value = emptyMap()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
