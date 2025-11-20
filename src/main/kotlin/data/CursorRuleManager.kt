package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Cursor规则管理器
 * 通过Git管理多用户分支数据
 */
object CursorRuleManager {
    private var currentUserId: String = ""
    private val _rules = MutableStateFlow<List<CursorRuleData>>(emptyList())
    val rules: StateFlow<List<CursorRuleData>> = _rules.asStateFlow()

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
        loadRules()
    }

    /**
     * 加载规则数据
     */
    private fun loadRules() {
        if (currentUserId.isEmpty()) return

        try {
            val mergedData = GitDataManager.getAllMergedData()
            _rules.value = mergedData.cursorRules.filter { PermissionManager.canView(it.shareableData) }
            println("✅ 加载Cursor规则数据成功，共 ${_rules.value.size} 个规则")
        } catch (e: Exception) {
            println("❌ 加载Cursor规则数据失败: ${e.message}")
            _rules.value = emptyList()
        }
    }

    /**
     * 保存规则数据
     */
    private suspend fun saveRules() {
        try {
            // 获取当前用户的所有数据
            val currentUserData = GitDataManager.getUserData(currentUserId)

            // 更新规则
            val updatedUserData = currentUserData.copy(cursorRules = _rules.value)

            // 保存到Git
            GitDataManager.saveCurrentUserData(currentUserId, updatedUserData)

            println("✅ 保存Cursor规则数据成功")
        } catch (e: Exception) {
            println("❌ 保存Cursor规则数据失败: ${e.message}")
        }
    }

    /**
     * 添加规则
     */
    suspend fun addRule(rule: CursorRuleData): Result<CursorRuleData> = withContext(Dispatchers.IO) {
        try {
            // 创建时自动设置创建者为当前用户
            val newRule = rule.copy(createdBy = CurrentUserManager.getCurrentUserId())

            // 检查名称是否重复
            if (isRuleNameExists(newRule.name)) {
                return@withContext Result.failure(Exception("规则名称已存在"))
            }

            val currentRules = _rules.value
            val newRules = currentRules + newRule
            _rules.value = newRules
            saveRules()
            Result.success(newRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新规则 - 只允许创建者修改
     */
    suspend fun updateRule(updatedRule: CursorRuleData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _rules.value.find { it.id == updatedRule.id }
            if (existing == null) {
                return@withContext Result.failure(Exception("规则不存在"))
            }

            // 检查编辑权限
            if (!PermissionManager.canEdit(existing.baseData)) {
                return@withContext Result.failure(Exception("无权限编辑此规则"))
            }

            // 检查名称是否重复
            if (isRuleNameExists(updatedRule.name, updatedRule.id)) {
                return@withContext Result.failure(Exception("规则名称已存在"))
            }

            val currentRules = _rules.value
            val finalRule = updatedRule.copy(
                createdBy = existing.createdBy, // 保持创建者不变
                lastModified = System.currentTimeMillis()
            )
            val newRules = currentRules.map { if (it.id == updatedRule.id) finalRule else it }
            _rules.value = newRules
            saveRules()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除规则 - 只允许创建者删除
     */
    suspend fun deleteRule(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _rules.value.find { it.id == ruleId }
            if (existing == null) {
                return@withContext Result.failure(Exception("规则不存在"))
            }

            // 检查删除权限
            if (!PermissionManager.canDelete(existing.baseData)) {
                return@withContext Result.failure(Exception("无权限删除此规则"))
            }

            val currentRules = _rules.value
            val newRules = currentRules.filter { it.id != ruleId }
            _rules.value = newRules
            saveRules()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据ID获取规则（需要有查看权限）
     */
    fun getRuleById(ruleId: String): CursorRuleData? {
        return _rules.value.find { it.id == ruleId }
    }

    /**
     * 检查规则名称是否已存在（在用户可访问的规则中）
     */
    fun isRuleNameExists(name: String, excludeId: String? = null): Boolean {
        return _rules.value.any { it.name == name && it.id != excludeId }
    }

    /**
     * 发布新版本 - 只允许创建者发布
     */
    suspend fun publishNewVersion(ruleId: String, content: String, publishedBy: String = "未知用户"): Result<CursorRuleData> = withContext(Dispatchers.IO) {
        try {
            val rule = getRuleById(ruleId)
            if (rule == null) {
                return@withContext Result.failure(Exception("规则不存在"))
            }

            // 检查编辑权限
            if (!PermissionManager.canEdit(rule.baseData)) {
                return@withContext Result.failure(Exception("无权限编辑此规则"))
            }

            val updatedRule = rule.publishNewVersion(content, publishedBy)
            updateRule(updatedRule)
            Result.success(updatedRule)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 共享规则 - 只允许创建者共享
     */
    suspend fun shareRule(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _rules.value.find { it.id == ruleId }
            if (existing == null) {
                return@withContext Result.failure(Exception("规则不存在"))
            }

            // 检查共享权限
            if (!PermissionManager.canShare(existing.shareableData)) {
                return@withContext Result.failure(Exception("无权限共享此规则"))
            }

            val updatedRule = existing.copy(isShared = true)
            val currentRules = _rules.value
            val newRules = currentRules.map { if (it.id == ruleId) updatedRule else it }
            _rules.value = newRules
            saveRules()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 取消共享规则 - 只允许创建者操作
     */
    suspend fun unshareRule(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _rules.value.find { it.id == ruleId }
            if (existing == null) {
                return@withContext Result.failure(Exception("规则不存在"))
            }

            // 检查权限
            if (!PermissionManager.canShare(existing.shareableData)) {
                return@withContext Result.failure(Exception("无权限取消共享此规则"))
            }

            val updatedRule = existing.copy(isShared = false)
            val currentRules = _rules.value
            val newRules = currentRules.map { if (it.id == ruleId) updatedRule else it }
            _rules.value = newRules
            saveRules()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取可编辑的规则列表
     */
    fun getEditableRules(): List<CursorRuleData> {
        return _rules.value.filter { PermissionManager.canEdit(it.baseData) }
    }
}


