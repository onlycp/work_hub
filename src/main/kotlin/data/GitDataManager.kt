package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import utils.Logger
import java.io.File

/**
 * Git数据管理器
 * 负责多用户分支的数据同步和管理
 */
object GitDataManager {
    // 在类加载时初始化 JMX 禁用器
    init {
        // 确保 JMX 被禁用
        JMXDisabler
    }
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // 本地数据目录结构
    private val workhubDir = File(System.getProperty("user.home"), ".workhub")
    private val gitDir = File(workhubDir, "git")
    private val usersDir = File(workhubDir, "users")
    private val membersDir = File(workhubDir, "members")
    private val mergedDir = File(workhubDir, "merged")
    // 新增：homehub/repo 目录结构
    private val homehubDir = File(System.getProperty("user.home"), "homehub")
    private val repoDir = File(homehubDir, "repo")

    // JGit相关
    private var git: Git? = null
    private var repository: Repository? = null

    init {
        // 在初始化时设置系统属性，禁用 JMX 以避免 Windows 上的 MalformedObjectNameException
        // 这必须在任何 JGit 操作之前设置
        try {
            System.setProperty("java.awt.headless", "false")
            System.setProperty("com.sun.management.jmxremote", "false")
            // 禁用 JGit 的 JMX 监控（这是最关键的设置）
            System.setProperty("org.eclipse.jgit.internal.storage.file.WindowCache.mxBeanDisabled", "true")
            // 额外禁用 MBeanServer 创建（Windows 兼容性）
            System.setProperty("java.lang.management.ManagementFactory.createPlatformMXBean", "false")
        } catch (e: Exception) {
            // 忽略设置系统属性时的异常
            Logger.warn("GitDataManager: 设置系统属性时出现异常: ${e.message}")
        }

        // 确保目录存在
        workhubDir.mkdirs()
        usersDir.mkdirs()
        mergedDir.mkdirs()
        homehubDir.mkdirs()
        repoDir.mkdirs()
    }

    /**
     * 获取或创建Git仓库实例
     */
    private fun getGit(): Git {
        if (git == null) {
            val gitDirFile = File(gitDir, ".git")
            if (gitDirFile.exists()) {
                // 打开现有仓库
                repository = FileRepositoryBuilder()
                    .setGitDir(gitDirFile)
                    .build()
                git = Git(repository)
            } else {
                // 创建新仓库
                git = Git.init().setDirectory(gitDir).call()
                repository = git!!.repository
            }
        }
        return git!!
    }

    /**
     * 测试仓库连接
     */
    suspend fun testRepositoryConnection(repositoryUrl: String, username: String? = null, password: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 使用 ls-remote 命令测试连接
            val lsRemote = org.eclipse.jgit.api.LsRemoteCommand(null)
                .setRemote(repositoryUrl)
                .setHeads(true)
                .setTags(true)

            if (username != null && password != null) {
                lsRemote.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
            }

            val remoteRefs = lsRemote.call()
            val branchCount = remoteRefs.count { it.name.startsWith("refs/heads/") }
            val tagCount = remoteRefs.count { it.name.startsWith("refs/tags/") }

            Result.success("连接成功！发现 ${branchCount} 个分支，${tagCount} 个标签")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 初始化Git仓库
     */
    suspend fun initializeRepository(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val git = getGit()

            // 配置用户信息
            git.repository.config.setString("user", null, "name", "WorkHub")
            git.repository.config.setString("user", null, "email", "workhub@localhost")
            git.repository.config.save()

            // 创建初始提交
            createInitialCommit(git)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 克隆或更新远程仓库
     */
    suspend fun cloneOrUpdateRemoteRepository(repositoryUrl: String, username: String? = null, password: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val git = getGit()

            // 如果目录不存在或为空，克隆仓库
            if (!gitDir.exists() || gitDir.listFiles()?.isEmpty() == true) {
                gitDir.mkdirs()

                // 关闭当前git实例，因为我们要重新克隆
                git.close()
                GitDataManager.git = null
                GitDataManager.repository = null

                // 克隆仓库
                val cloneCommand = Git.cloneRepository()
                    .setURI(repositoryUrl)
                    .setDirectory(gitDir)

                if (username != null && password != null) {
                    cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
                }

                GitDataManager.git = cloneCommand.call()
                GitDataManager.repository = GitDataManager.git!!.repository
            } else {
                // 如果已存在，拉取最新内容
                val fetchCommand = git.fetch()
                if (username != null && password != null) {
                    fetchCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(username, password))
                }
                fetchCommand.call()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 拉取所有分支到homehub/repo目录
     */
    suspend fun pullAllBranchesToHomehubRepo(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val git = getGit()

            // 获取所有远程分支
            val remoteBranches = getRemoteBranches()
            val memberNames = mutableListOf<String>()

            for (branch in remoteBranches) {
                val branchName = branch.removePrefix("origin/")

                // 创建对应的homehub/repo目录
                val memberDir = File(repoDir, branchName)

                // 处理单个分支，包含容错和重试机制
                val branchResult = pullBranchWithRetry(git, branchName, memberDir, maxRetries = 3)

                if (branchResult.isSuccess) {
                    // 记录成员名称（分支名对应成员姓名）
                    if (branchName != "HEAD" && !branchName.contains("HEAD")) {
                        memberNames.add(branchName)
                    }
                    Logger.info("成功处理分支: $branchName")
                } else {
                    Logger.warn("分支 $branchName 处理失败: ${branchResult.exceptionOrNull()?.message ?: "未知错误"}")
                    // 继续处理其他分支，不中断整个流程
                }
            }

                // 切换回主分支
                try {
                    git.checkout().setName("main").call()
                } catch (e: Exception) {
                    Logger.warn("切换回主分支失败: ${e.message}")
                    // 尝试切换到master分支
                try {
                    git.checkout().setName("master").call()
                } catch (e2: Exception) {
                    Logger.warn("切换回master分支也失败: ${e2.message}")
                }
            }

            Result.success(memberNames)
        } catch (e: Exception) {
            Logger.error("拉取所有分支时出现严重错误: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 带重试机制的单个分支拉取方法
     */
    private suspend fun pullBranchWithRetry(
        git: Git,
        branchName: String,
        memberDir: File,
        maxRetries: Int = 3
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        var success = false

        for (attempt in 1..maxRetries) {
            try {
                Logger.debug("正在处理分支 $branchName (尝试 $attempt/$maxRetries)")

                // 如果是重试，或者第一次尝试时目录已存在且可能有问题，先删除成员目录
                if (attempt > 1 || (attempt == 1 && memberDir.exists() && isMemberDirectoryEmpty(memberDir))) {
                    Logger.debug("${if (attempt > 1) "重试前" else "检测到空的成员目录"}删除成员目录: ${memberDir.absolutePath}")
                    try {
                        memberDir.deleteRecursively()
                        memberDir.mkdirs()
                    } catch (e: Exception) {
                        Logger.warn("删除成员目录失败: ${e.message}")
                    }
                }

                // 创建目录
                memberDir.mkdirs()

                // 切换到分支并拉取内容
                try {
                    git.checkout().setName(branchName).call()
                } catch (e: Exception) {
                    // 如果checkout失败，先删除成员目录，然后重新创建跟踪分支
                    Logger.debug("checkout失败，删除成员目录并重新尝试: $branchName")
                    try {
                        memberDir.deleteRecursively()
                        memberDir.mkdirs()
                        Logger.debug("已删除并重新创建成员目录: ${memberDir.absolutePath}")
                    } catch (deleteException: Exception) {
                        Logger.warn("删除成员目录失败: ${deleteException.message}")
                    }

                    // 重新尝试创建跟踪分支
                    try {
                        git.checkout()
                            .setCreateBranch(true)
                            .setName(branchName)
                            .setStartPoint("origin/$branchName")
                            .call()
                        Logger.debug("成功创建跟踪分支: $branchName")
                    } catch (createException: Exception) {
                        Logger.warn("创建跟踪分支也失败: ${createException.message}")
                        throw createException
                    }
                }

                // 拉取内容
                git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(branchName)
                    .call()

                // 将分支内容复制到homehub/repo目录
                copyBranchContentToMemberDir(branchName, memberDir)

                // 成功完成
                success = true
                break

            } catch (e: Exception) {
                lastException = e
                Logger.warn("分支 $branchName 处理失败 (尝试 $attempt/$maxRetries): ${e.message}")

                // 检查是否是 JMX 相关异常
                if (e.javaClass.name.contains("MalformedObjectNameException") ||
                    e.message?.contains("JMX") == true ||
                    e.message?.contains("MBean") == true) {
                    Logger.debug("检测到 JMX 相关异常，尝试禁用更多 JMX 功能")

                    // 额外禁用 JMX 设置
                    try {
                        System.setProperty("org.eclipse.jgit.internal.storage.file.WindowCache.mxBeanDisabled", "true")
                        System.setProperty("com.sun.management.jmxremote", "false")
                        System.setProperty("com.sun.management.jmxremote.port", "")
                        System.setProperty("java.lang.management.ManagementFactory.createPlatformMXBean", "false")
                        System.setProperty("javax.management.builder.initial", "")
                        System.setProperty("javax.management.MBeanServerBuilder", "")
                    } catch (jmxException: Exception) {
                        Logger.warn("额外 JMX 禁用失败: ${jmxException.message}")
                    }
                }

                // 如果不是最后一次尝试，继续重试
                if (attempt < maxRetries) {
                    Logger.debug("等待1秒后重试...")
                    kotlinx.coroutines.delay(1000)
                    continue
                }
            }
        }

        // 返回结果
        if (success) {
            Logger.debug("分支 $branchName 拉取成功")
            Result.success(Unit)
        } else {
            Logger.error("分支 $branchName 拉取失败: ${lastException?.message}", lastException)
            Result.failure(lastException ?: Exception("未知错误"))
        }
    }

    /**
     * 从分支自动创建成员
     */
    suspend fun createMembersFromBranches(memberNames: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 如果只有一个主分支，自动创建成员
            if (memberNames.size == 1 && (memberNames[0] == "main" || memberNames[0] == "master")) {
                // 从主分支创建默认成员
                val defaultMemberName = "默认成员" // 或者从配置中获取
                createMemberForBranch(defaultMemberName, memberNames[0])
            } else {
                // 为每个分支创建对应的成员
                for (memberName in memberNames) {
                    if (memberName != "main" && memberName != "master") {
                        createMemberForBranch(memberName, memberName)
                    }
                }
            }

            // 确保所有新创建的成员分支都被推送到远程仓库
            val git = getGit()
            for (memberName in memberNames) {
                if (memberName != "main" && memberName != "master") {
                    try {
                        // 检查分支是否存在于远程
                        val remoteBranches = getRemoteBranches()
                        if (!remoteBranches.contains("origin/$memberName")) {
                            // 如果远程没有这个分支，推送到远程
                            git.checkout().setName(memberName).call()
                            git.push().setRemote("origin").add(memberName).call()
                            git.checkout().setName("main").call()
                            println("✅ 成员分支 $memberName 已推送到远程仓库")
                        }
                    } catch (e: Exception) {
                        println("⚠️ 推送成员分支 $memberName 失败: ${e.message}")
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 为分支创建成员
     */
    private suspend fun createMemberForBranch(memberName: String, branchName: String) = withContext(Dispatchers.IO) {
        // 检查成员是否已存在
        val existingMembers = MemberManager.members.value
        if (existingMembers.any { it.name == memberName }) {
            return@withContext // 成员已存在，跳过
        }

        // 创建新成员
        val newMember = MemberData(
            id = memberName.hashCode().toString(),
            name = memberName,
            password = "", // 默认空密码
            number = ((existingMembers.maxOfOrNull { it.number.toIntOrNull() ?: 0 } ?: 0) + 1).toString(),
            createdBy = "system" // 系统自动创建
        )

        // 添加到成员管理器
        MemberManager.addMember(newMember)

        // 为成员创建对应的分支（如果不存在）
        try {
            val git = getGit()
            git.checkout().setCreateBranch(true).setName(memberName).call()
            git.push().setRemote("origin").add(memberName).call()
            git.checkout().setName("main").call()
        } catch (e: Exception) {
            // 分支可能已存在，忽略错误
            println("分支 $memberName 可能已存在: ${e.message}")
        }
    }

    /**
     * 为用户创建独立Git仓库
     * 流程：
     * 1. cd workhub/users/
     * 2. mkdir 用户名
     * 3. cd 用户名
     * 4. 加入初始化数据文件
     * 5. git init
     * 6. git add 及 commit
     * 7. 配置远程仓库
     * 8. 检查分支是否存在，如果不存在则创建并推送
     */
    suspend fun createUserBranch(userName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 创建用户目录
            val userLocalDir = File(usersDir, userName)
            userLocalDir.mkdirs()

            // 2. 检查是否已有Git仓库
            val gitDir = File(userLocalDir, ".git")
            val userGit = if (gitDir.exists()) {
                // 如果已存在，打开现有仓库
                Git.open(userLocalDir)
            } else {
                // 创建新的Git仓库
                Git.init().setDirectory(userLocalDir).call()
            }

            try {
                // 3. 配置远程仓库（使用主仓库地址）
                val repoSettings = RepositorySettingsManager.getCurrentSettings()
                if (repoSettings.enabled && repoSettings.repositoryUrl.isNotBlank()) {
                    // 检查是否已配置远程仓库
                    val remotes = userGit.remoteList().call()
                    val hasOrigin = remotes.any { it.name == "origin" }

                    if (!hasOrigin) {
                        userGit.remoteAdd().setName("origin").setUri(org.eclipse.jgit.transport.URIish(repoSettings.repositoryUrl)).call()
                    }

                    // 4. 拉取远程分支信息
                    val fetchCommand = userGit.fetch()
                    if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                        fetchCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                    }
                    fetchCommand.call()

                    // 5. 检查分支是否已存在于远程
                    val remoteBranches = userGit.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call()
                    val branchExists = remoteBranches.any { ref ->
                        ref.name == "refs/remotes/origin/$userName" || ref.name == "origin/$userName"
                    }

                    // 6. 如果分支不存在，尝试查找默认分支（main或master）
                    var defaultBranch = "main"
                    if (!branchExists) {
                        val hasMain = remoteBranches.any { ref ->
                            ref.name == "refs/remotes/origin/main" || ref.name == "origin/main"
                        }
                        val hasMaster = remoteBranches.any { ref ->
                            ref.name == "refs/remotes/origin/master" || ref.name == "origin/master"
                        }
                        defaultBranch = when {
                            hasMain -> "main"
                            hasMaster -> "master"
                            else -> "main" // 默认使用main
                        }
                    }

                    if (branchExists) {
                        // 分支已存在，拉取分支内容
                        println("✅ 用户分支 $userName 已存在于远程，切换到该分支")
                        try {
                            // 尝试切换到现有分支
                            userGit.checkout().setName(userName).call()
                        } catch (e: Exception) {
                            // 如果切换失败，创建本地分支跟踪远程分支
                            println("本地分支不存在，创建跟踪分支: $userName")
                            userGit.checkout()
                                .setCreateBranch(true)
                                .setName(userName)
                                .setStartPoint("origin/$userName")
                                .call()
                        }

                        // 拉取最新内容
                        userGit.pull()
                            .setRemote("origin")
                            .setRemoteBranchName(userName)
                            .call()
                    } else {
                        // 分支不存在，需要创建
                        // 7. 创建初始化数据文件
                        if (!gitDir.exists()) {
                            createInitialUserDataFiles(userLocalDir, userName)

                            // 8. 添加所有文件到Git
                            userGit.add().addFilepattern(".").call()

                            // 9. 创建初始提交
                            userGit.commit().setMessage("Initial commit for user: $userName").call()
                        }

                        // 10. 创建并切换到用户分支（基于默认分支）
                        try {
                            userGit.checkout()
                                .setCreateBranch(true)
                                .setName(userName)
                                .setStartPoint("origin/$defaultBranch")
                                .call()
                        } catch (e: Exception) {
                            // 如果默认分支不存在，从当前HEAD创建
                            println("⚠️ 默认分支 $defaultBranch 不存在，从当前HEAD创建分支")
                            userGit.checkout().setCreateBranch(true).setName(userName).call()
                        }

                        // 11. 推送到远程仓库
                        val pushCommand = userGit.push().setRemote("origin")
                        if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                            pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                        }
                        pushCommand.call()

                        println("✅ 用户 $userName 的独立Git仓库已创建并推送到远程分支 $userName")
                    }
                } else {
                    // 仓库未配置，只创建本地仓库
                    if (!gitDir.exists()) {
                        createInitialUserDataFiles(userLocalDir, userName)
                        userGit.add().addFilepattern(".").call()
                        userGit.commit().setMessage("Initial commit for user: $userName").call()
                    }
                    println("⚠️ 仓库未配置，仅创建本地用户仓库")
                }

            } finally {
                // 关闭用户Git仓库
                userGit.close()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ 创建用户分支失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 为用户创建初始数据文件
     */
    private fun createInitialUserDataFiles(userDir: File, userName: String) {
        // 创建README文件
        val readme = File(userDir, "README.md")
        readme.writeText("""
            # User: $userName

            This directory contains configuration data for user: $userName.

            Files:
            - ssh_configs.json: SSH connection configurations
            - keys.json: SSH key configurations
            - cursor_rules.json: Cursor rule configurations
            - members.json: Member data
        """.trimIndent())

        // 创建空的配置文件
        val sshConfigs = File(userDir, "ssh_configs.json")
        sshConfigs.writeText("[]")

        val keys = File(userDir, "keys.json")
        keys.writeText("[]")

        val cursorRules = File(userDir, "cursor_rules.json")
        cursorRules.writeText("[]")

        val members = File(userDir, "members.json")
        members.writeText("[]")
    }

    /**
     * 为成员创建初始数据文件
     */
    private fun createInitialMemberDataFiles(memberDir: File, memberName: String) {
        // 创建README文件
        val readme = File(memberDir, "README.md")
        readme.writeText("""
            # Member: $memberName

            This directory contains configuration data for member: $memberName.

            Member data is shared across all users, but each member can have their own
            configuration and workspace settings.

            Files:
            - member_config.json: Member-specific configuration
            - workspace.json: Workspace preferences
        """.trimIndent())

        // 创建空的配置文件
        val memberConfig = File(memberDir, "member_config.json")
        memberConfig.writeText("{}")

        val workspace = File(memberDir, "workspace.json")
        workspace.writeText("{}")
    }

    /**
     * 发现并同步所有远程用户分支
     * 为不存在的远程用户分支创建本地用户仓库
     */
    suspend fun discoverAndSyncUserBranches(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val repoSettings = RepositorySettingsManager.getCurrentSettings()
            if (!repoSettings.enabled || repoSettings.repositoryUrl.isBlank()) {
                return@withContext Result.success(Unit)
            }

            // 1. 获取远程仓库的所有分支
            val testConnectionResult = testRepositoryConnection(
                repositoryUrl = repoSettings.repositoryUrl,
                username = repoSettings.username.takeIf { it.isNotBlank() },
                password = repoSettings.password.takeIf { it.isNotBlank() }
            )

            if (testConnectionResult.isFailure) {
                println("⚠️ 无法连接远程仓库，跳过用户分支发现: ${testConnectionResult.exceptionOrNull()?.message}")
                return@withContext Result.success(Unit)
            }

            // 2. 从连接测试结果中解析分支信息（这里需要修改testRepositoryConnection来返回分支列表）
            // 暂时使用ls-remote获取分支列表
            val lsRemote = org.eclipse.jgit.api.LsRemoteCommand(null)
                .setRemote(repoSettings.repositoryUrl)
                .setHeads(true)

            if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                lsRemote.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
            }

            val remoteRefs = lsRemote.call()
            val userBranches = remoteRefs.map { ref ->
                ref.name.removePrefix("refs/heads/")
            }.filter { branchName ->
                // 过滤出用户分支（非main/master的分支可以认为是用户分支）
                branchName != "main" && branchName != "master" && !branchName.contains("/")
            }

            println("发现 ${userBranches.size} 个用户分支: ${userBranches.joinToString(", ")}")

            // 3. 为每个用户分支创建本地仓库（如果不存在）
            for ((index, userName) in userBranches.withIndex()) {
                println("正在处理用户分支 ${index + 1}/${userBranches.size}: $userName")

                val userLocalDir = File(usersDir, userName)
                val userGitDir = File(userLocalDir, ".git")

                if (!userGitDir.exists()) {
                    // 创建用户分支的本地仓库
                    println("  创建用户 $userName 的本地仓库...")
                    val createResult = createUserBranch(userName)
                    if (createResult.isFailure) {
                        println("  ⚠️ 为用户 $userName 创建本地仓库失败: ${createResult.exceptionOrNull()?.message}")
                    } else {
                        println("  ✅ 为用户 $userName 创建了本地仓库")
                    }
                } else {
                    // 检查并修复空的Git仓库
                    if (checkAndRepairEmptyUserRepository(userName, userLocalDir)) {
                        continue // 已修复，跳过后续处理
                    }

                    // 如果已存在，同步数据
                    println("  同步用户 $userName 的仓库...")
                    try {
                        val userGit = Git.open(userLocalDir)
                        try {
                            val pullCommand = userGit.pull()
                            if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                                pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                            }
                            pullCommand.call()
                            println("  ✅ 已同步用户 $userName 的仓库")
                        } finally {
                            userGit.close()
                        }
                    } catch (e: Exception) {
                        println("  ⚠️ 同步用户 $userName 仓库失败: ${e.message}")
                    }
                }
            }

            // 4. 合并所有用户数据
            mergeAllUserData()

            Result.success(Unit)
        } catch (e: OutOfMemoryError) {
            // 处理内存不足错误
            println("❌ 内存不足错误: ${e.message}")
            Result.failure(RuntimeException("内存不足，无法执行Git操作", e))
        } catch (e: StackOverflowError) {
            // 处理栈溢出错误
            println("❌ 栈溢出错误: ${e.message}")
            Result.failure(RuntimeException("栈溢出，无法执行Git操作", e))
        } catch (e: Error) {
            // 处理所有其他 Error 类型异常
            println("❌ 系统错误: ${e.message}")
            Result.failure(RuntimeException("系统错误，无法执行Git操作: ${e.javaClass.simpleName}", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 同步所有用户数据
     * 现在每个用户目录都是独立的Git仓库，需要分别更新
     */
    suspend fun syncAllBranches(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Logger.info("开始同步所有用户分支数据")
            val repoSettings = RepositorySettingsManager.getCurrentSettings()
            if (!repoSettings.enabled || repoSettings.repositoryUrl.isBlank()) {
                Logger.warn("Git仓库未配置，只执行本地数据合并")
                // 即使仓库未配置，也要合并本地数据
                mergeAllUserData()
                Logger.info("本地数据合并完成")
                return@withContext Result.success(Unit)
            }

            // 获取所有用户目录
            val userDirs: Array<File> = usersDir.listFiles()?.filter { it.isDirectory }?.toTypedArray() ?: emptyArray()

            // 为每个用户仓库执行拉取操作
            for (userDir in userDirs) {
                val userName = userDir.name
                Logger.debug("开始同步用户 $userName 的数据")
                try {
                    val userGitDir = File(userDir, ".git")
                    if (userGitDir.exists()) {
                        // 检查并修复空的Git仓库
                        if (checkAndRepairEmptyUserRepository(userName, userDir)) {
                            continue // 已修复，跳过后续处理
                        }

                        val userGit = Git.open(userDir)
                        try {
                            // 检查当前分支和远程跟踪分支
                            val currentBranch = userGit.repository.fullBranch
                            val branchName = currentBranch.substringAfter("refs/heads/")

                            // 设置上游分支（如果还没有设置）
                            try {
                                val config = userGit.repository.config
                                val remoteRef = config.getString("branch", branchName, "remote")
                                val mergeRef = config.getString("branch", branchName, "merge")

                                if (remoteRef.isNullOrBlank() || mergeRef.isNullOrBlank()) {
                                    // 设置上游分支
                                    userGit.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call()
                                    val remoteBranches = userGit.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call()
                                    val matchingRemote = remoteBranches.find { ref ->
                                        ref.name.endsWith("/$branchName")
                                    }

                                    if (matchingRemote != null) {
                                        userGit.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call()
                                        val remoteName = matchingRemote.name.substringAfter("refs/remotes/")
                                        userGit.branchList().setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.REMOTE).call()
                                        val remote = remoteName.substringBefore("/")
                                        val remoteBranch = remoteName.substringAfter("/")

                                        config.setString("branch", branchName, "remote", remote)
                                        config.setString("branch", branchName, "merge", "refs/heads/$remoteBranch")
                                        config.save()
                                    }
                                }
                            } catch (e: Exception) {
                                // 忽略上游分支设置错误
                                println("⚠️ 设置上游分支失败，继续拉取: ${e.message}")
                            }

                            // 执行拉取操作
                            val pullCommand = userGit.pull()
                            if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                                pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                            }
                            pullCommand.call()
                            Logger.info("用户 $userName 的数据同步成功")
                        } finally {
                            userGit.close()
                        }
                    }
                } catch (e: Exception) {
                    Logger.error("用户 $userName 的数据同步失败: ${e.message}", e)
                }
            }

            // 合并数据到本地缓存
            mergeAllUserData()
            Logger.info("所有用户分支数据同步完成")

            Result.success(Unit)
        } catch (e: OutOfMemoryError) {
            // 处理内存不足错误
            Logger.error("数据同步失败 - 内存不足错误: ${e.message}", e)
            Result.failure(RuntimeException("内存不足，无法执行Git操作", e))
        } catch (e: StackOverflowError) {
            // 处理栈溢出错误
            Logger.error("数据同步失败 - 栈溢出错误: ${e.message}", e)
            Result.failure(RuntimeException("栈溢出，无法执行Git操作", e))
        } catch (e: Error) {
            // 处理所有其他 Error 类型异常
            Logger.error("数据同步失败 - 系统错误: ${e.message}", e)
            Result.failure(RuntimeException("系统错误，无法执行Git操作: ${e.javaClass.simpleName}", e))
        } catch (e: Exception) {
            Logger.error("数据同步失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 检查并修复空的Git仓库目录（只包含.git目录的情况）
     * 如果用户目录只有.git目录，则删除并从远程重新checkout分支
     */
    private suspend fun checkAndRepairEmptyUserRepository(userName: String, userDir: File): Boolean = withContext(Dispatchers.IO) {
        val userFiles = userDir.listFiles()
        val hasOnlyGit = userFiles?.size == 1 && userFiles[0].name == ".git"

        if (!hasOnlyGit) return@withContext false

        println("⚠️ 检测到用户 $userName 的目录只有.git目录，删除并从远程checkout")
        try {
            userDir.deleteRecursively()
            userDir.mkdirs()

            // 从远程checkout用户分支
            val repoSettings = RepositorySettingsManager.getCurrentSettings()
            if (repoSettings.enabled && repoSettings.repositoryUrl.isNotBlank()) {
                val cloneCommand = Git.cloneRepository()
                    .setURI(repoSettings.repositoryUrl)
                    .setDirectory(userDir)
                    .setBranch(userName) // 指定checkout的分支

                if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                    cloneCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                }

                val userGit = cloneCommand.call()
                userGit.close()
                println("✅ 已从远程checkout用户 $userName 的分支")
                return@withContext true
            } else {
                println("⚠️ 仓库未配置，无法从远程checkout用户分支")
                return@withContext false
            }
        } catch (checkoutException: Exception) {
            println("⚠️ 从远程checkout用户 $userName 分支失败: ${checkoutException.message}")
            return@withContext false
        }
    }

    /**
     * 检查成员目录是否实质为空（只包含.git目录或其他临时文件）
     */
    private fun isMemberDirectoryEmpty(memberDir: File): Boolean {
        if (!memberDir.exists()) return false

        val files = memberDir.listFiles()
        if (files == null || files.isEmpty()) return true

        // 如果只有一个.git目录，也认为是空目录
        if (files.size == 1 && files[0].name == ".git") return true

        // 检查是否所有文件都是临时文件或.git相关的文件
        val nonTempFiles = files.filter { file ->
            !file.name.startsWith(".") && // 不是隐藏文件
            file.name != ".git" && // 不是.git目录
            !file.name.endsWith(".tmp") && // 不是临时文件
            !file.name.endsWith(".lock") // 不是锁文件
        }

        return nonTempFiles.isEmpty()
    }

    /**
     * 查找可用的默认分支
     */
    private suspend fun findAvailableDefaultBranch(
        repoSettings: RepositorySettings,
        userName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 直接从主仓库配置获取远程分支列表，不依赖用户的本地仓库
            val lsRemote = org.eclipse.jgit.api.LsRemoteCommand(null)
                .setRemote(repoSettings.repositoryUrl)
                .setHeads(true)

            if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                lsRemote.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
            }

            val remoteRefs = lsRemote.call()
            val remoteBranchNames = remoteRefs.map { ref ->
                ref.name.removePrefix("refs/heads/")
            }

            println("远程仓库可用分支: ${remoteBranchNames.joinToString(", ")}")

            // 检查用户分支是否存在
            if (userName in remoteBranchNames) {
                println("用户 $userName 的分支存在于远程")
                userName
            }

            // 如果用户分支不存在，查找默认分支
            when {
                "main" in remoteBranchNames -> {
                    println("使用默认分支: main")
                    "main"
                }
                "master" in remoteBranchNames -> {
                    println("使用默认分支: master")
                    "master"
                }
                remoteBranchNames.isNotEmpty() -> {
                    val defaultBranch = remoteBranchNames.first()
                    println("使用第一个可用分支作为默认: $defaultBranch")
                    defaultBranch
                }
                else -> {
                    println("⚠️ 未找到任何远程分支")
                    null
                }
            }
        } catch (e: Exception) {
            println("⚠️ 获取远程分支列表失败: ${e.message}")
            null
        }
    }

    /**
     * 推送当前用户的数据
     * 现在在用户的独立Git仓库中进行操作
     */
    suspend fun pushCurrentUserData(userName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Logger.info("开始推送用户 $userName 的数据")
            val userLocalDir = File(usersDir, userName)
            val userGitDir = File(userLocalDir, ".git")

            if (!userGitDir.exists()) {
                Logger.error("推送失败: 用户 $userName 的Git仓库不存在")
                return@withContext Result.failure(Exception("用户 $userName 的Git仓库不存在"))
            }

            // 打开用户的Git仓库
            val userGit = Git.open(userLocalDir)
            try {
                // 添加所有更改
                Logger.debug("为用户 $userName 添加文件更改")
                userGit.add().addFilepattern(".").call()

                // 检查是否有更改需要提交
                val status = userGit.status().call()
                if (status.hasUncommittedChanges() || !status.untracked.isEmpty()) {
                    Logger.debug("用户 $userName 有更改需要提交，创建提交")
                    // 创建提交
                    userGit.commit().setMessage("Update data for user: $userName").call()
                } else {
                    Logger.debug("用户 $userName 没有需要提交的更改")
                }

                // 推送到远程仓库
                Logger.debug("开始推送用户 $userName 的数据到远程仓库")
                val repoSettings = RepositorySettingsManager.getCurrentSettings()
                val pushCommand = userGit.push().setRemote("origin")
                if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                    pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                }
                pushCommand.call()

                Logger.info("用户 $userName 的数据推送成功")
                Result.success(Unit)
            } finally {
                userGit.close()
            }
        } catch (e: Exception) {
            Logger.error("推送用户 $userName 数据失败: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 获取指定用户的数据
     */
    fun getUserData(userName: String): UserData {
        val userDir = File(mergedDir, userName)
        return loadUserDataFromDirectory(userDir)
    }

    /**
     * 获取所有用户的合并数据
     */
    fun getAllMergedData(): MergedData {
        return MergedData(
            sshConfigs = getAllMergedSSHConfigs(),
            keys = getAllMergedKeys(),
            cursorRules = getAllMergedCursorRules(),
            members = getAllMergedMembers()
        )
    }

    /**
     * 保存当前用户的数据
     * 现在保存到用户的独立Git仓库中
     */
    suspend fun saveCurrentUserData(userName: String, data: UserData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userDir = File(usersDir, userName)
            userDir.mkdirs()

            // 保存各类型数据
            saveDataToFile(userDir, "ssh_configs.json", data.sshConfigs)
            saveDataToFile(userDir, "keys.json", data.keys)
            saveDataToFile(userDir, "cursor_rules.json", data.cursorRules)
            saveDataToFile(userDir, "members.json", data.members)

            // 如果用户目录是Git仓库，自动提交更改
            val userGitDir = File(userDir, ".git")
            if (userGitDir.exists()) {
                val userGit = Git.open(userDir)
                try {
                    // 添加更改
                    userGit.add().addFilepattern(".").call()

                    // 检查是否有更改需要提交
                    val status = userGit.status().call()
                    if (status.hasUncommittedChanges() || !status.untracked.isEmpty()) {
                        userGit.commit().setMessage("Update data for user: $userName").call()
                        println("✅ 用户 $userName 的数据已自动提交到本地仓库")
                    }
                } finally {
                    userGit.close()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ 保存用户 $userName 数据失败: ${e.message}")
            Result.failure(e)
        }
    }

    // 私有辅助方法
    private fun getAllBranches(): List<String> {
        return try {
            val git = getGit()
            val branches = git.branchList().call()
            branches.map { ref ->
                ref.name.substringAfter("refs/remotes/origin/")
            }.filter { it.isNotEmpty() && !it.contains("HEAD") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 创建成员分支仓库
     * 成员分支在当前用户的users/xxx目录下创建，参考createUserBranch的实现
     */
    suspend fun createMemberBranch(memberName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 获取当前用户名
            val currentUserName = CurrentUserManager.getCurrentUserId()
            if (currentUserName.isEmpty()) {
                return@withContext Result.failure(Exception("未登录用户，无法创建成员分支"))
            }

            println("开始创建成员分支: $memberName (用户: $currentUserName)")

            // 1. 在当前用户的目录下创建成员目录
            val userLocalDir = File(usersDir, currentUserName)
            if (!userLocalDir.exists()) {
                return@withContext Result.failure(Exception("用户目录不存在: $currentUserName"))
            }

            val memberLocalDir = File(userLocalDir, memberName)
            memberLocalDir.mkdirs()
            println("创建成员目录: ${memberLocalDir.absolutePath}")

            // 2. 在成员目录中创建独立的Git仓库
            val memberGit = Git.init().setDirectory(memberLocalDir).call()

            try {
                // 3. 创建初始化数据文件
                createInitialMemberDataFiles(memberLocalDir, memberName)

                // 4. 添加所有文件到Git
                memberGit.add().addFilepattern(".").call()

                // 5. 创建初始提交
                memberGit.commit().setMessage("Initial commit for member: $memberName").call()

                // 6. 配置远程仓库（使用主仓库地址）
                val repoSettings = RepositorySettingsManager.getCurrentSettings()
                if (repoSettings.enabled && repoSettings.repositoryUrl.isNotBlank()) {
                    memberGit.remoteAdd().setName("origin").setUri(org.eclipse.jgit.transport.URIish(repoSettings.repositoryUrl)).call()

                    // 7. 设置上游分支为 用户名/成员名
                    val branchName = "$currentUserName/$memberName"
                    memberGit.checkout().setCreateBranch(true).setName(branchName).call()

                    // 8. 推送到远程仓库
                    val pushCommand = memberGit.push().setRemote("origin")
                    if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                        pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                    }
                    pushCommand.call()

                    println("✅ 成员 $memberName 的独立Git仓库已创建并推送到远程分支 $branchName")
                } else {
                    println("⚠️ 仓库未配置，无法推送成员分支到远程")
                }

            } finally {
                // 关闭成员Git仓库
                memberGit.close()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ 创建成员分支失败: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 获取远程分支列表
     */
    private fun getRemoteBranches(): List<String> {
        return try {
            val git = getGit()
            val branches = git.branchList().call()
            branches.map { ref ->
                ref.name.substringAfter("refs/remotes/")
            }.filter { it.isNotEmpty() && it.contains("origin/") && !it.contains("HEAD") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 将分支内容复制到成员目录
     */
    private fun copyBranchContentToMemberDir(branchName: String, memberDir: File) {
        // 清空目标目录
        memberDir.listFiles()?.forEach { it.deleteRecursively() }

        // 复制git目录的内容（排除.git文件夹）
        gitDir.listFiles()?.forEach { file ->
            if (file.name != ".git" && file.name != ".gitignore") {
                if (file.isDirectory) {
                    copyDirectory(file, File(memberDir, file.name))
                } else {
                    file.copyTo(File(memberDir, file.name), overwrite = true)
                }
            }
        }
    }

    private suspend fun createOrUpdateUserDirectory(userName: String, branch: String) = withContext(Dispatchers.IO) {
        try {
            val git = getGit()

            // 切换到分支
            git.checkout().setName(branch).call()

            // 从Git目录复制到本地用户目录
            val gitUserDir = File(gitDir, userName)
            val userLocalDir = File(usersDir, userName)
            if (gitUserDir.exists()) {
                copyDirectory(gitUserDir, userLocalDir)
            }
        } catch (e: Exception) {
            println("Failed to create/update directory for user $userName: ${e.message}")
        } finally {
            // 确保回到主分支
            try {
                val git = getGit()
                git.checkout().setName("main").call()
            } catch (e: Exception) {
                // 忽略切换分支的错误
            }
        }
    }

    suspend fun mergeAllUserData() = withContext(Dispatchers.IO) {
        // 清空合并目录
        mergedDir.listFiles()?.forEach { it.deleteRecursively() }

        // 获取所有用户目录（独立Git仓库）
        val userDirs: Array<File> = usersDir.listFiles()?.filter { it.isDirectory }?.toTypedArray() ?: emptyArray()

        // 为每个用户目录创建合并后的数据
        for (userDir in userDirs) {
            val userName = userDir.name
            val userMergedDir = File(mergedDir, userName)
            userMergedDir.mkdirs()

            // 从用户的独立Git仓库复制数据
            if (userDir.exists()) {
                copyDirectory(userDir, userMergedDir)
            }
        }
    }

    private fun copyDirectory(source: File, target: File) {
        if (!source.exists()) return

        source.listFiles()?.forEach { file ->
            val targetFile = File(target, file.name)
            if (file.isDirectory) {
                targetFile.mkdirs()
                copyDirectory(file, targetFile)
            } else {
                file.copyTo(targetFile, overwrite = true)
            }
        }
    }

    private fun createInitialCommit(git: Git) {
        val readme = File(gitDir, "README.md")
        readme.writeText("# WorkHub Data Repository\n\nThis repository contains shared configuration data for WorkHub application users.")

        git.add().addFilepattern("README.md").call()
        git.commit().setMessage("Initial commit").call()
    }

    private inline fun <reified T> saveDataToFile(dir: File, fileName: String, data: List<T>) {
        val file = File(dir, fileName)
        val jsonString = json.encodeToString(data)
        file.writeText(jsonString)
    }

    private fun loadUserDataFromDirectory(dir: File): UserData {
        return UserData(
            sshConfigs = loadDataFromFile(dir, "ssh_configs.json"),
            keys = loadDataFromFile(dir, "keys.json"),
            cursorRules = loadDataFromFile(dir, "cursor_rules.json"),
            members = loadDataFromFile(dir, "members.json")
        )
    }

    private inline fun <reified T> loadDataFromFile(dir: File, fileName: String): List<T> {
        return try {
            val file = File(dir, fileName)
            if (file.exists()) {
                val jsonString = file.readText()
                json.decodeFromString(jsonString)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getAllMergedSSHConfigs(): List<SSHConfigData> {
        return mergedDir.listFiles()
            ?.flatMap { userDir -> loadDataFromFile<SSHConfigData>(userDir, "ssh_configs.json") }
            ?.distinctBy { it.id }
            ?: emptyList()
    }

    private fun getAllMergedKeys(): List<KeyData> {
        return mergedDir.listFiles()
            ?.flatMap { userDir -> loadDataFromFile<KeyData>(userDir, "keys.json") }
            ?.distinctBy { it.id }
            ?: emptyList()
    }

    private fun getAllMergedCursorRules(): List<CursorRuleData> {
        return mergedDir.listFiles()
            ?.flatMap { userDir -> loadDataFromFile<CursorRuleData>(userDir, "cursor_rules.json") }
            ?.distinctBy { it.id }
            ?: emptyList()
    }

    private fun getAllMergedMembers(): List<MemberData> {
        return mergedDir.listFiles()
            ?.flatMap { userDir -> loadDataFromFile<MemberData>(userDir, "members.json") }
            ?.distinctBy { it.id }
            ?: emptyList()
    }
}

/**
 * 用户数据集合
 */
data class UserData(
    val sshConfigs: List<SSHConfigData> = emptyList(),
    val keys: List<KeyData> = emptyList(),
    val cursorRules: List<CursorRuleData> = emptyList(),
    val members: List<MemberData> = emptyList()
)

/**
 * 合并后的数据集合
 */
data class MergedData(
    val sshConfigs: List<SSHConfigData> = emptyList(),
    val keys: List<KeyData> = emptyList(),
    val cursorRules: List<CursorRuleData> = emptyList(),
    val members: List<MemberData> = emptyList()
)
