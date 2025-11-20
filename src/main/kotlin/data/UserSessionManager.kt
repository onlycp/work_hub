package data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 用户信息
 */
data class UserInfo(
    val userId: String,
    val userName: String = "",
    val displayName: String = ""
)

/**
 * 用户会话管理器
 * 管理当前登录用户和用户数据管理器实例
 */
object UserSessionManager {

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    /**
     * 登录用户
     */
    fun login(userId: String, userName: String = ""): Result<UserInfo> {
        return try {
            val userInfo = UserInfo(
                userId = userId,
                userName = userName,
                displayName = userName.ifEmpty { userId }
            )
            _currentUser.value = userInfo

            // 设置所有单例管理器的当前用户
            SSHConfigManager.setCurrentUser(userId)
            KeyManager.setCurrentUser(userId)
            MemberManager.setCurrentUser(userId)
            CursorRuleManager.setCurrentUser(userId)

            println("用户登录成功: $userId")
            Result.success(userInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 登出用户
     */
    fun logout() {
        _currentUser.value = null
        println("用户已登出")
    }

    /**
     * 获取当前用户
     */
    fun getCurrentUser(): UserInfo? = _currentUser.value

    /**
     * 获取当前用户ID
     */
    fun getCurrentUserId(): String? = _currentUser.value?.userId

    /**
     * 检查是否有用户登录
     */
    fun isLoggedIn(): Boolean = _currentUser.value != null
}

/**
 * 用户数据管理器集合
 * 提供对所有用户相关数据管理器的访问
 */
object UserDataManagers {

    val settingsManager = UserSettingsManager

    val sshConfigManager = SSHConfigManager

    val keyManager = KeyManager

    val memberManager = MemberManager

    val cursorRuleManager = CursorRuleManager

    /**
     * 初始化所有数据管理器
     */
    suspend fun initializeAll() {
        // SSH配置管理器如果需要初始化
        // 其他管理器如果需要初始化可以在这里添加
    }

    /**
     * 获取权限管理器
     */
    fun getPermissions(): PermissionManager {
        return PermissionManager
    }
}


/**
 * 数据类型枚举
 */
enum class DataType {
    SSH_CONFIG, KEY, MEMBER, CURSOR_RULE
}
