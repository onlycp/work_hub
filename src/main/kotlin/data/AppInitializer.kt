package data

import kotlinx.coroutines.*

/**
 * 应用初始化器
 * 负责应用启动时的Git同步和用户初始化
 */
object AppInitializer {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 应用启动初始化
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            println("开始应用初始化...")

            // 1. 初始化Git仓库
            val gitResult = GitDataManager.initializeRepository()
            if (gitResult.isFailure) {
                println("Git仓库初始化失败: ${gitResult.exceptionOrNull()?.message}")
                // Git初始化失败不影响应用启动，继续
            }

            // 2. 验证远程仓库配置
            val repoValidationResult = validateRepositorySettings()
            if (repoValidationResult.isFailure) {
                println("远程仓库配置验证失败: ${repoValidationResult.exceptionOrNull()?.message}")
                // 配置验证失败不影响应用启动，继续
            }

            // 3. 同步所有分支数据
            val syncResult = GitDataManager.syncAllBranches()
            if (syncResult.isFailure) {
                println("数据同步失败: ${syncResult.exceptionOrNull()?.message}")
                // 同步失败不影响应用启动，继续
            }

            println("应用初始化完成")
            Result.success(Unit)
        } catch (e: Exception) {
            println("应用初始化失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 验证远程仓库配置
     * 现在我们使用独立的用户仓库，不再需要克隆主仓库
     */
    suspend fun validateRepositorySettings(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val repoSettings = RepositorySettingsManager.getCurrentSettings()

            if (!repoSettings.enabled || repoSettings.repositoryUrl.isBlank()) {
                println("仓库同步未启用或未配置仓库地址")
                return@withContext Result.success(Unit)
            }

            println("验证远程仓库配置...")

            // 测试仓库连接
            val testResult = GitDataManager.testRepositoryConnection(
                repositoryUrl = repoSettings.repositoryUrl,
                username = repoSettings.username.takeIf { it.isNotBlank() },
                password = repoSettings.password.takeIf { it.isNotBlank() }
            )

            if (testResult.isFailure) {
                return@withContext Result.failure(Exception("仓库连接测试失败: ${testResult.exceptionOrNull()?.message}"))
            }

            println("仓库配置验证成功: ${testResult.getOrNull()}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 用户登录
     * 只有在没有成员时，才自动创建用户
     * 当已经有了用户之后，就不能自动创建用户并登录了
     */
    suspend fun loginUser(userName: String): Result<Unit> = withContext(Dispatchers.IO) {
        // 在登录前再次确保 JMX 被禁用（Windows 兼容性）
        try {
            System.setProperty("org.eclipse.jgit.internal.storage.file.WindowCache.mxBeanDisabled", "true")
            System.setProperty("com.sun.management.jmxremote", "false")
            System.setProperty("com.sun.management.jmxremote.port", "")
            System.setProperty("java.lang.management.ManagementFactory.createPlatformMXBean", "false")
            System.setProperty("javax.management.builder.initial", "")
            System.setProperty("org.eclipse.jgit.util.FS.DETECTED", "false")
        } catch (e: Exception) {
            // 忽略设置异常
            println("⚠️ 登录前 JMX 禁用失败: ${e.message}")
        }

        try {
            // 获取所有成员数据
            val mergedData = GitDataManager.getAllMergedData()
            val existingMembers = mergedData.members
            val member = existingMembers.find { it.name == userName }

            if (member == null) {
                // 检查是否允许自动创建用户（只有在没有成员时才允许）
                if (existingMembers.isNotEmpty()) {
                    // 如果已有成员但没有找到匹配的用户，提示用户不存在
                    return@withContext Result.failure(Exception("用户不存在，请联系管理员"))
                }

                // 如果没有成员，自动创建第一个用户
                println("系统中没有成员，自动创建用户: $userName")

                val newMember = MemberData(
                    name = userName,
                    number = generateUserNumber(),
                    password = "", // 新用户默认空密码
                    createdBy = userName // 自己创建自己
                )

                // 创建用户分支（独立的Git仓库）
                val branchResult = GitDataManager.createUserBranch(userName)
                if (branchResult.isFailure) {
                    println("创建用户分支失败: ${branchResult.exceptionOrNull()?.message}")
                    return@withContext Result.failure(Exception("创建用户分支失败: ${branchResult.exceptionOrNull()?.message}"))
                }

                // 保存新用户数据到独立的Git仓库中
                val userData = UserData(members = listOf(newMember))
                val saveResult = GitDataManager.saveCurrentUserData(userName, userData)
                if (saveResult.isFailure) {
                    println("保存用户数据失败: ${saveResult.exceptionOrNull()?.message}")
                    return@withContext Result.failure(Exception("保存用户数据失败: ${saveResult.exceptionOrNull()?.message}"))
                }

                println("成功创建系统第一个用户: $userName")
            } else {
                println("用户 $userName 已存在于系统中，开始登录")
            }

            // 设置当前用户
            CurrentUserManager.setCurrentUser(userName, userName)

            // 同步数据以确保用户数据被正确合并
            val syncResult = GitDataManager.syncAllBranches()
            if (syncResult.isFailure) {
                println("数据同步失败: ${syncResult.exceptionOrNull()?.message}")
            }

            // 更新所有管理器
            updateAllManagers(userName)

            println("用户 $userName 登录成功")
            Result.success(Unit)
        } catch (e: Exception) {
            println("用户登录失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 同步数据
     */
    suspend fun syncData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 推送当前用户数据
            val currentUser = CurrentUserManager.getCurrentUserId()
            if (currentUser.isNotEmpty()) {
                GitDataManager.pushCurrentUserData(currentUser)
            }

            // 拉取所有分支数据
            GitDataManager.syncAllBranches()

            // 重新加载数据
            updateAllManagers(currentUser)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 应用退出时的清理工作
     */
    fun shutdown() {
        scope.cancel()
    }

    // 私有方法
    private fun generateUserNumber(): String {
        // 生成用户编号，如 U001, U002 等
        val mergedData = GitDataManager.getAllMergedData()
        val existingNumbers = mergedData.members.map { it.number }.toSet()
        var number = 1
        while (true) {
            val userNumber = "U${number.toString().padStart(3, '0')}"
            if (userNumber !in existingNumbers) {
                return userNumber
            }
            number++
        }
    }

    private fun updateAllManagers(userId: String) {
        SSHConfigManager.setCurrentUser(userId)
        KeyManager.setCurrentUser(userId)
        CursorRuleManager.setCurrentUser(userId)
        MemberManager.setCurrentUser(userId)
        ExpenseManager.setCurrentUser(userId)
        // SettingsManager 可能需要特殊处理，因为它是用户个人的
    }
}
