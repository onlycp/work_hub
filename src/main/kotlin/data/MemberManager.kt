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
 * 成员数据类
 */
@Serializable
data class MemberData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                          // 姓名
    val password: String,                      // 密码
    val number: String,                        // 编号
    val introduction: String = "",             // 个人介绍
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    // 创建者ID - 只有创建者能编辑，成员默认共享
    val createdBy: String
)

/**
 * 成员管理器
 * 成员数据默认共享，所有用户都能查看，但只有创建者能编辑
 */
object MemberManager {
    private var currentUserId: String = ""
    private val _members = MutableStateFlow<List<MemberData>>(emptyList())
    val members: StateFlow<List<MemberData>> = _members.asStateFlow()

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
        loadMembers()
    }

    /**
     * 加载成员数据
     */
    private fun loadMembers() {
        if (currentUserId.isEmpty()) return

        try {
            // 成员数据默认共享，所有用户都能查看
            val mergedData = GitDataManager.getAllMergedData()
            _members.value = mergedData.members
            println("✅ 加载成员数据成功，共 ${_members.value.size} 个成员")
        } catch (e: Exception) {
            println("❌ 加载成员数据失败: ${e.message}")
            _members.value = emptyList()
        }
    }

    /**
     * 保存成员数据
     */
    private suspend fun saveMembers() {
        try {
            // 获取当前用户的所有数据
            val currentUserData = GitDataManager.getUserData(currentUserId)

            // 更新成员
            val updatedUserData = currentUserData.copy(members = _members.value)

            // 保存到Git
            GitDataManager.saveCurrentUserData(currentUserId, updatedUserData)

            println("✅ 保存成员数据成功")
        } catch (e: Exception) {
            println("❌ 保存成员数据失败: ${e.message}")
        }
    }

    /**
     * 添加成员
     */
    suspend fun addMember(member: MemberData): Result<MemberData> = withContext(Dispatchers.IO) {
        try {
            // 创建时自动设置创建者为当前用户
            val newMember = member.copy(createdBy = CurrentUserManager.getCurrentUserId())

            // 检查编号是否重复
            if (isMemberNumberExists(newMember.number)) {
                return@withContext Result.failure(Exception("成员编号已存在"))
            }

            val currentMembers = _members.value
            val newMembers = currentMembers + newMember
            _members.value = newMembers
            saveMembers()

            // 检查该成员是否已经有用户目录和Git仓库，如果没有则自动创建（参考登录时的处理）
            val userDir = File(System.getProperty("user.home"), ".workhub/users/${newMember.name}")
            val userGitDir = File(userDir, ".git")
            
            if (!userDir.exists() || !userGitDir.exists()) {
                println("成员 ${newMember.name} 还没有用户目录和Git仓库，自动创建...")
                
                // 创建用户分支（独立的Git仓库），参考登录时的处理
                val branchResult = GitDataManager.createUserBranch(newMember.name)
                if (branchResult.isFailure) {
                    println("⚠️ 创建用户分支失败，但成员已添加: ${branchResult.exceptionOrNull()?.message}")
                    // 成员已添加成功，这里不返回失败，只记录警告
                } else {
                    println("✅ 成员 ${newMember.name} 的用户分支已创建")
                    
                    // 保存成员数据到新创建的用户仓库中（参考登录时的处理）
                    val userData = UserData(members = listOf(newMember))
                    val saveResult = GitDataManager.saveCurrentUserData(newMember.name, userData)
                    if (saveResult.isFailure) {
                        println("⚠️ 保存成员数据到用户仓库失败: ${saveResult.exceptionOrNull()?.message}")
                    }
                }
            } else {
                println("✅ 成员 ${newMember.name} 已添加（用户目录已存在）")
            }

            Result.success(newMember)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新成员 - 只允许创建者修改
     */
    suspend fun updateMember(updatedMember: MemberData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _members.value.find { it.id == updatedMember.id }
            if (existing == null) {
                return@withContext Result.failure(Exception("成员不存在"))
            }

            // 检查编辑权限
            if (!PermissionManager.canEdit(existing.baseData)) {
                return@withContext Result.failure(Exception("无权限编辑此成员"))
            }

            // 检查编号是否重复
            if (isMemberNumberExists(updatedMember.number, updatedMember.id)) {
                return@withContext Result.failure(Exception("成员编号已存在"))
            }

            val currentMembers = _members.value
            val finalMember = updatedMember.copy(
                createdBy = existing.createdBy, // 保持创建者不变
                lastModified = System.currentTimeMillis()
            )
            val newMembers = currentMembers.map { if (it.id == updatedMember.id) finalMember else it }
            _members.value = newMembers
            saveMembers()

            println("✅ 成员 ${finalMember.name} 已更新")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除成员 - 只允许创建者删除
     */
    suspend fun deleteMember(memberId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val existing = _members.value.find { it.id == memberId }
            if (existing == null) {
                return@withContext Result.failure(Exception("成员不存在"))
            }

            // 检查删除权限
            if (!PermissionManager.canDelete(existing.baseData)) {
                return@withContext Result.failure(Exception("无权限删除此成员"))
            }

            val currentMembers = _members.value
            val newMembers = currentMembers.filter { it.id != memberId }
            _members.value = newMembers
            saveMembers()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据ID获取成员
     */
    fun getMemberById(memberId: String): MemberData? {
        return _members.value.find { it.id == memberId }
    }

    /**
     * 检查成员姓名是否已存在
     */
    fun isMemberNameExists(name: String, excludeId: String? = null): Boolean {
        return _members.value.any { it.name == name && it.id != excludeId }
    }

    /**
     * 检查成员编号是否已存在
     */
    fun isMemberNumberExists(number: String, excludeId: String? = null): Boolean {
        return _members.value.any { it.number == number && it.id != excludeId }
    }

    /**
     * 获取可编辑的成员列表
     */
    fun getEditableMembers(): List<MemberData> {
        return _members.value.filter { PermissionManager.canEdit(it.baseData) }
    }
}

