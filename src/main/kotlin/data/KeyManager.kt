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
 * 认证类型枚举
 */
@Serializable
enum class AuthType {
    PASSWORD, // 密码认证
    KEY       // 密钥认证
}

/**
 * 密钥数据类
 */
@Serializable
data class KeyData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                           // 密钥名称
    val username: String,                       // 用户名
    val authType: AuthType,                     // 认证类型
    val password: String = "",                  // 密码（当authType为PASSWORD时使用）
    val privateKeyContent: String = "",         // 私钥内容（当authType为KEY时使用）
    val privateKeyPassphrase: String = "",      // 私钥密码（当authType为KEY时使用）
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    // 创建者ID - 只有创建者能编辑
    val createdBy: String,
    // 是否共享给其他用户查看
    val isShared: Boolean = false
)

/**
 * 密钥管理器
 * 通过Git管理多用户分支数据
 */
object KeyManager {
    private var currentUserId: String = ""
    private val _keys = MutableStateFlow<List<KeyData>>(emptyList())
    val keys: StateFlow<List<KeyData>> = _keys.asStateFlow()

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
        loadKeys()
    }

    /**
     * 加载密钥数据
     */
    private fun loadKeys() {
        if (currentUserId.isEmpty()) return

        try {
            val mergedData = GitDataManager.getAllMergedData()
            _keys.value = mergedData.keys.filter { PermissionManager.canView(it.shareableData) }
            println("✅ 加载密钥数据成功，共 ${_keys.value.size} 个密钥")
        } catch (e: Exception) {
            println("❌ 加载密钥数据失败: ${e.message}")
            _keys.value = emptyList()
        }
    }

    /**
     * 保存密钥数据
     */
    private suspend fun saveKeys() {
        try {
            // 获取当前用户的所有数据
            val currentUserData = GitDataManager.getUserData(currentUserId)

            // 更新密钥
            val updatedUserData = currentUserData.copy(keys = _keys.value)

            // 保存到Git
            GitDataManager.saveCurrentUserData(currentUserId, updatedUserData)

            println("✅ 保存密钥数据成功")
        } catch (e: Exception) {
            println("❌ 保存密钥数据失败: ${e.message}")
        }
    }

    /**
     * 添加密钥
     */
    suspend fun addKey(key: KeyData): Result<KeyData> = withContext(Dispatchers.IO) {
        try {
            // 创建时自动设置创建者为当前用户
            val newKey = key.copy(createdBy = CurrentUserManager.getCurrentUserId())

            // 检查名称是否重复
            if (isKeyNameExists(newKey.name)) {
                return@withContext Result.failure(Exception("密钥名称已存在"))
            }

            val currentKeys = _keys.value
            val newKeys = currentKeys + newKey
            _keys.value = newKeys
            saveKeys()
            Result.success(newKey)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新密钥 - 只允许创建者修改
     */
    suspend fun updateKey(updatedKey: KeyData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _keys.value.find { it.id == updatedKey.id }
            if (existing == null) {
                return@withContext Result.failure(Exception("密钥不存在"))
            }

            // 检查编辑权限
            if (!PermissionManager.canEdit(existing.baseData)) {
                return@withContext Result.failure(Exception("无权限编辑此密钥"))
            }

            // 检查名称是否重复
            if (isKeyNameExists(updatedKey.name, updatedKey.id)) {
                return@withContext Result.failure(Exception("密钥名称已存在"))
            }

            val currentKeys = _keys.value
            val finalKey = updatedKey.copy(
                createdBy = existing.createdBy, // 保持创建者不变
                lastModified = System.currentTimeMillis()
            )
            val newKeys = currentKeys.map { if (it.id == updatedKey.id) finalKey else it }
            _keys.value = newKeys
            saveKeys()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除密钥 - 只允许创建者删除
     */
    suspend fun deleteKey(keyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _keys.value.find { it.id == keyId }
            if (existing == null) {
                return@withContext Result.failure(Exception("密钥不存在"))
            }

            // 检查删除权限
            if (!PermissionManager.canDelete(existing.baseData)) {
                return@withContext Result.failure(Exception("无权限删除此密钥"))
            }

            val currentKeys = _keys.value
            val newKeys = currentKeys.filter { it.id != keyId }
            _keys.value = newKeys
            saveKeys()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 共享密钥 - 只允许创建者共享
     */
    suspend fun shareKey(keyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _keys.value.find { it.id == keyId }
            if (existing == null) {
                return@withContext Result.failure(Exception("密钥不存在"))
            }

            // 检查共享权限
            if (!PermissionManager.canShare(existing.shareableData)) {
                return@withContext Result.failure(Exception("无权限共享此密钥"))
            }

            val updatedKey = existing.copy(isShared = true)
            val currentKeys = _keys.value
            val newKeys = currentKeys.map { if (it.id == keyId) updatedKey else it }
            _keys.value = newKeys
            saveKeys()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 取消共享密钥 - 只允许创建者操作
     */
    suspend fun unshareKey(keyId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _keys.value.find { it.id == keyId }
            if (existing == null) {
                return@withContext Result.failure(Exception("密钥不存在"))
            }

            // 检查权限
            if (!PermissionManager.canShare(existing.shareableData)) {
                return@withContext Result.failure(Exception("无权限取消共享此密钥"))
            }

            val updatedKey = existing.copy(isShared = false)
            val currentKeys = _keys.value
            val newKeys = currentKeys.map { if (it.id == keyId) updatedKey else it }
            _keys.value = newKeys
            saveKeys()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据ID获取密钥（需要有查看权限）
     */
    fun getKeyById(keyId: String): KeyData? {
        return _keys.value.find { it.id == keyId }
    }

    /**
     * 检查密钥名称是否已存在（在用户可访问的密钥中）
     */
    fun isKeyNameExists(name: String, excludeId: String? = null): Boolean {
        return _keys.value.any { it.name == name && it.id != excludeId }
    }

    /**
     * 获取可编辑的密钥列表
     */
    fun getEditableKeys(): List<KeyData> {
        return _keys.value.filter { PermissionManager.canEdit(it.baseData) }
    }
}
