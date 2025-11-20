package data

/**
 * 当前用户信息管理器
 * 管理当前登录用户的信息
 */
object CurrentUserManager {
    // 当前用户ID，从成员数据中选择
    private var currentUserId: String = ""
    private var currentUserName: String = ""

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String, userName: String = "") {
        currentUserId = userId
        currentUserName = userName.ifEmpty { userId }
    }

    /**
     * 获取当前用户ID
     */
    fun getCurrentUserId(): String = currentUserId

    /**
     * 获取当前用户名（用于显示）
     */
    fun getCurrentUserName(): String = currentUserName

    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean = currentUserId.isNotEmpty()
}

/**
 * 基础数据接口
 */
interface BaseData {
    val id: String
    val createdBy: String
    val createdAt: Long
    val lastModified: Long
}

/**
 * 可共享数据接口
 */
interface ShareableData : BaseData {
    val isShared: Boolean
}

/**
 * 权限管理器单例
 * 实现基于创建者的权限控制：谁创建的只允许谁编辑
 */
object PermissionManager {

    private val currentUserId: String
        get() = CurrentUserManager.getCurrentUserId()

    // 判断用户是否能编辑该数据
    fun canEdit(data: BaseData): Boolean {
        // 如果是共享数据，只有创建者才能编辑
        if (data is ShareableData && data.isShared) {
            return data.createdBy == currentUserId
        }
        // 非共享数据，只有创建者才能编辑
        return data.createdBy == currentUserId
    }

    // 判断用户是否能查看该数据
    fun canView(data: ShareableData): Boolean {
        return data.createdBy == currentUserId || data.isShared
    }

    // 判断用户是否能查看成员数据（成员默认共享）
    fun canViewMember(data: BaseData): Boolean {
        // 成员数据默认所有用户可见（但在单用户桌面应用中，主要是为了Git同步考虑）
        return true
    }

    // 判断用户是否能删除该数据
    fun canDelete(data: BaseData): Boolean {
        // 如果是共享数据，只有创建者才能删除
        if (data is ShareableData && data.isShared) {
            return data.createdBy == currentUserId
        }
        // 非共享数据，只有创建者才能删除
        return data.createdBy == currentUserId
    }

    // 判断用户是否能共享该数据
    fun canShare(data: ShareableData): Boolean {
        return data.createdBy == currentUserId
    }

    // 获取数据的所有者信息
    fun getOwnerInfo(data: BaseData): String {
        return if (data.createdBy == currentUserId) {
            "我创建的"
        } else {
            "由 ${data.createdBy} 创建"
        }
    }

    // 获取共享状态描述
    fun getShareStatus(data: ShareableData): String {
        return when {
            !data.isShared -> "私有"
            data.createdBy == currentUserId -> "已共享"
            else -> "共享的"
        }
    }

    // 判断是否显示编辑按钮
    fun shouldShowEditButton(data: BaseData): Boolean = canEdit(data)

    // 判断是否显示删除按钮
    fun shouldShowDeleteButton(data: BaseData): Boolean = canDelete(data)

    // 判断是否显示共享按钮
    fun shouldShowShareButton(data: ShareableData): Boolean = canShare(data)

    // 判断是否显示共享状态指示器
    fun shouldShowShareIndicator(data: ShareableData): Boolean = data.isShared

    /**
     * 判断用户是否可以操作SSH配置下的端口转发规则
     * 如果SSH配置是共享的且不是当前用户创建的，则不允许操作
     */
    fun canEditPortRules(sshConfig: SSHConfigData): Boolean {
        return !sshConfig.isShared || sshConfig.createdBy == currentUserId
    }

    /**
     * 判断用户是否可以操作SSH配置下的命令规则
     * 如果SSH配置是共享的且不是当前用户创建的，则不允许操作
     */
    fun canEditCommandRules(sshConfig: SSHConfigData): Boolean {
        return !sshConfig.isShared || sshConfig.createdBy == currentUserId
    }

    /**
     * 判断是否显示端口规则的添加按钮
     */
    fun shouldShowAddPortButton(sshConfig: SSHConfigData?): Boolean {
        return sshConfig?.let { canEditPortRules(it) } ?: true
    }

    /**
     * 判断是否显示端口规则的编辑按钮
     */
    fun shouldShowEditPortButton(sshConfig: SSHConfigData?): Boolean {
        return sshConfig?.let { canEditPortRules(it) } ?: true
    }

    /**
     * 判断是否显示端口规则的删除按钮
     */
    fun shouldShowDeletePortButton(sshConfig: SSHConfigData?): Boolean {
        return sshConfig?.let { canEditPortRules(it) } ?: true
    }

    /**
     * 判断是否显示命令规则的添加按钮
     */
    fun shouldShowAddCommandButton(sshConfig: SSHConfigData?): Boolean {
        return sshConfig?.let { canEditCommandRules(it) } ?: true
    }

    /**
     * 判断是否显示命令规则的编辑按钮
     */
    fun shouldShowEditCommandButton(sshConfig: SSHConfigData?): Boolean {
        return sshConfig?.let { canEditCommandRules(it) } ?: true
    }

    /**
     * 判断是否显示命令规则的删除按钮
     */
    fun shouldShowDeleteCommandButton(sshConfig: SSHConfigData?): Boolean {
        return sshConfig?.let { canEditCommandRules(it) } ?: true
    }
}

// 扩展函数，让数据类实现接口
val SSHConfigData.baseData: BaseData
    get() = object : BaseData {
        override val id: String = this@baseData.id
        override val createdBy: String = this@baseData.createdBy
        override val createdAt: Long = this@baseData.createdAt
        override val lastModified: Long = this@baseData.lastModified
    }

val SSHConfigData.shareableData: ShareableData
    get() = object : ShareableData {
        override val id: String = this@shareableData.id
        override val createdBy: String = this@shareableData.createdBy
        override val createdAt: Long = this@shareableData.createdAt
        override val lastModified: Long = this@shareableData.lastModified
        override val isShared: Boolean = this@shareableData.isShared
    }

val KeyData.baseData: BaseData
    get() = object : BaseData {
        override val id: String = this@baseData.id
        override val createdBy: String = this@baseData.createdBy
        override val createdAt: Long = this@baseData.createdAt
        override val lastModified: Long = this@baseData.lastModified
    }

val KeyData.shareableData: ShareableData
    get() = object : ShareableData {
        override val id: String = this@shareableData.id
        override val createdBy: String = this@shareableData.createdBy
        override val createdAt: Long = this@shareableData.createdAt
        override val lastModified: Long = this@shareableData.lastModified
        override val isShared: Boolean = this@shareableData.isShared
    }

val CursorRuleData.baseData: BaseData
    get() = object : BaseData {
        override val id: String = this@baseData.id
        override val createdBy: String = this@baseData.createdBy
        override val createdAt: Long = this@baseData.createdAt
        override val lastModified: Long = this@baseData.lastModified
    }

val CursorRuleData.shareableData: ShareableData
    get() = object : ShareableData {
        override val id: String = this@shareableData.id
        override val createdBy: String = this@shareableData.createdBy
        override val createdAt: Long = this@shareableData.createdAt
        override val lastModified: Long = this@shareableData.lastModified
        override val isShared: Boolean = this@shareableData.isShared
    }

val MemberData.baseData: BaseData
    get() = object : BaseData {
        override val id: String = this@baseData.id
        override val createdBy: String = this@baseData.createdBy
        override val createdAt: Long = this@baseData.createdAt
        override val lastModified: Long = this@baseData.lastModified
    }
