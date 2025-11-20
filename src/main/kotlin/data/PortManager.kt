package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import service.SSHSessionManager
import kotlin.Result

/**
 * 端口管理器
 * 负责端口转发规则的管理和状态维护
 */
object PortManager {
    // 当前选中的SSH配置ID
    private var currentConfigId: String? = null

    // 端口转发规则状态流
    private val _portRules = MutableStateFlow<List<PortForwardingRuleData>>(emptyList())
    val portRules: StateFlow<List<PortForwardingRuleData>> = _portRules.asStateFlow()

    // 端口转发状态（规则ID -> 是否激活）
    private val _portStatuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val portStatuses: StateFlow<Map<String, Boolean>> = _portStatuses.asStateFlow()

    /**
     * 设置当前SSH配置
     */
    fun setCurrentConfig(configId: String?) {
        currentConfigId = configId
        if (configId != null) {
            val config = SSHConfigManager.getConfigById(configId)
            _portRules.value = config?.portForwardingRules ?: emptyList()

            // 从SSH客户端获取实际的端口转发状态，而不是重置为空
            val sshClient = SSHSessionManager.getSession(config?.name ?: "")
            if (sshClient != null && sshClient.isConnected()) {
                val activeRuleIds = sshClient.getActivePortForwardingRuleIds()
                // 为当前配置的规则设置状态
                val newStatuses = _portRules.value.associate { rule ->
                    rule.id to activeRuleIds.contains(rule.id)
                }
                _portStatuses.value = newStatuses
                println("✓ 同步端口转发状态: ${activeRuleIds.size} 个活跃转发")
            } else {
                // 如果没有SSH连接，则所有状态为false
                _portStatuses.value = emptyMap()
            }
        } else {
            _portRules.value = emptyList()
            _portStatuses.value = emptyMap()
        }
    }

    /**
     * 获取当前配置ID
     */
    fun getCurrentConfigId(): String? {
        return currentConfigId
    }

    /**
     * 获取当前配置的所有端口规则
     */
    fun getCurrentPortRules(): List<PortForwardingRuleData> {
        return _portRules.value
    }

    /**
     * 添加端口转发规则
     */
    suspend fun addPortRule(rule: PortForwardingRuleData): Result<PortForwardingRuleData> = withContext(Dispatchers.IO) {
        try {
            val currentConfig = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
                ?: return@withContext Result.failure(Exception("未选择SSH配置"))

            // 权限检查：如果SSH配置是共享的且不是当前用户创建的，则不允许操作
            if (!PermissionManager.canEditPortRules(currentConfig)) {
                return@withContext Result.failure(Exception("无权限操作此主机的端口规则"))
            }

            // 检查端口是否已被使用
            if (_portRules.value.any { it.localPort == rule.localPort }) {
                return@withContext Result.failure(Exception("本地端口 ${rule.localPort} 已被使用"))
            }

            val updatedRules = _portRules.value + rule
            val result = SSHConfigManager.updatePortForwardingRules(currentConfig.name, updatedRules)

            if (result.isSuccess) {
                _portRules.value = updatedRules
                Result.success(rule)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("未知错误"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新端口转发规则
     */
    suspend fun updatePortRule(ruleId: String, updatedRule: PortForwardingRuleData): Result<PortForwardingRuleData> = withContext(Dispatchers.IO) {
        try {
            val currentConfig = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
                ?: return@withContext Result.failure(Exception("未选择SSH配置"))

            // 权限检查：如果SSH配置是共享的且不是当前用户创建的，则不允许操作
            if (!PermissionManager.canEditPortRules(currentConfig)) {
                return@withContext Result.failure(Exception("无权限操作此主机的端口规则"))
            }

            // 检查端口冲突（排除当前规则）
            if (_portRules.value.any { it.id != ruleId && it.localPort == updatedRule.localPort }) {
                return@withContext Result.failure(Exception("本地端口 ${updatedRule.localPort} 已被使用"))
            }

            val updatedRules = _portRules.value.map { if (it.id == ruleId) updatedRule else it }
            val result = SSHConfigManager.updatePortForwardingRules(currentConfig.name, updatedRules)

            if (result.isSuccess) {
                _portRules.value = updatedRules
                // 如果端口状态有变化，需要更新状态
                val currentStatuses = _portStatuses.value
                if (currentStatuses.containsKey(ruleId)) {
                    // 保持原有状态，除非规则有重大变化
                    _portStatuses.value = currentStatuses + (ruleId to (currentStatuses[ruleId] ?: false))
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
     * 删除端口转发规则
     */
    suspend fun deletePortRule(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentConfig = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
                ?: return@withContext Result.failure(Exception("未选择SSH配置"))

            // 权限检查：如果SSH配置是共享的且不是当前用户创建的，则不允许操作
            if (!PermissionManager.canEditPortRules(currentConfig)) {
                return@withContext Result.failure(Exception("无权限操作此主机的端口规则"))
            }

            // 检查端口是否正在运行，如果是则先停止
            val isRunning = _portStatuses.value[ruleId] ?: false
            if (isRunning) {
                println("🛑 端口转发正在运行，先停止: $ruleId")
                val sshClient = SSHSessionManager.getSession(currentConfig.name)
                if (sshClient != null) {
                    val stopResult = sshClient.stopPortForwarding(ruleId)
                    if (stopResult.isFailure) {
                        println("⚠️ 停止端口转发失败，继续删除: ${stopResult.exceptionOrNull()?.message}")
                    }
                    // 同时从SSHClientManager的缓存中移除
                    sshClient.removePortForwardingRule(ruleId)
                }
                // 清除状态
                _portStatuses.value = _portStatuses.value - ruleId
            }

            val updatedRules = _portRules.value.filter { it.id != ruleId }
            val result = SSHConfigManager.updatePortForwardingRules(currentConfig.name, updatedRules)

            if (result.isSuccess) {
                _portRules.value = updatedRules
                println("✅ 端口转发规则已删除: $ruleId")
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("未知错误"))
            }
        } catch (e: Exception) {
            println("❌ 删除端口转发规则失败: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 切换端口转发状态
     */
    suspend fun togglePortRuleStatus(ruleId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val rule = _portRules.value.find { it.id == ruleId }
                ?: return@withContext Result.failure(Exception("端口规则不存在: $ruleId"))

            val currentStatus = _portStatuses.value[ruleId] ?: false
            val newStatus = !currentStatus

            println("🔄 切换端口转发状态: ${rule.description} (${rule.localPort}) -> ${if (newStatus) "启动" else "停止"}")

            // 获取SSH客户端并执行端口转发操作
            val config = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
                ?: return@withContext Result.failure(Exception("未找到SSH配置: $currentConfigId"))

            val sshClient = SSHSessionManager.getSession(config.name)
                ?: return@withContext Result.failure(Exception("未找到SSH连接: ${config.name}"))

            val result = if (newStatus) {
                println("▶️ 启动端口转发: ${rule.localPort} -> ${rule.remoteHost}:${rule.remotePort}")
                sshClient.startPortForwarding(rule)
            } else {
                println("⏹️ 停止端口转发: $ruleId")
                sshClient.stopPortForwarding(ruleId)
            }

            if (result.isSuccess) {
                _portStatuses.value = _portStatuses.value + (ruleId to newStatus)
                println("✅ 端口转发状态更新成功: $ruleId -> $newStatus")
                Result.success(newStatus)
            } else {
                val error = result.exceptionOrNull()
                println("❌ 端口转发操作失败: ${error?.message}")
                Result.failure(error ?: Exception("端口转发操作失败"))
            }
        } catch (e: Exception) {
            println("❌ 切换端口转发状态异常: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取端口转发状态
     */
    fun getPortRuleStatus(ruleId: String): Boolean {
        return _portStatuses.value[ruleId] ?: false
    }

    /**
     * 设置端口转发状态
     */
    fun setPortRuleStatus(ruleId: String, isActive: Boolean) {
        _portStatuses.value = _portStatuses.value + (ruleId to isActive)
    }

    /**
     * 停止所有端口转发
     */
    suspend fun stopAllPortForwarding(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // TODO: 实现停止所有端口转发的逻辑
            _portStatuses.value = emptyMap()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
