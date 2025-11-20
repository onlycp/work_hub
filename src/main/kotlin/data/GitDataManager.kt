package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
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
                memberDir.mkdirs()

                // 切换到分支并拉取内容
                git.checkout().setName(branchName).call()
                git.pull()
                    .setRemote("origin")
                    .setRemoteBranchName(branchName)
                    .call()

                // 将分支内容复制到homehub/repo目录
                copyBranchContentToMemberDir(branchName, memberDir)

                // 记录成员名称（分支名对应成员姓名）
                if (branchName != "HEAD" && !branchName.contains("HEAD")) {
                    memberNames.add(branchName)
                }
            }

            // 切换回主分支
            git.checkout().setName("main").call()

            Result.success(memberNames)
        } catch (e: Exception) {
            Result.failure(e)
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
     * 8. 按用户名作为分支推送上去
     */
    suspend fun createUserBranch(userName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 创建用户目录
            val userLocalDir = File(usersDir, userName)
            userLocalDir.mkdirs()

            // 2. 在用户目录中创建独立的Git仓库
            val userGit = Git.init().setDirectory(userLocalDir).call()

            try {
                // 3. 创建初始化数据文件
                createInitialUserDataFiles(userLocalDir, userName)

                // 4. 添加所有文件到Git
                userGit.add().addFilepattern(".").call()

                // 5. 创建初始提交
                userGit.commit().setMessage("Initial commit for user: $userName").call()

                // 6. 配置远程仓库（使用主仓库地址）
                val repoSettings = RepositorySettingsManager.getCurrentSettings()
                if (repoSettings.enabled && repoSettings.repositoryUrl.isNotBlank()) {
                    userGit.remoteAdd().setName("origin").setUri(org.eclipse.jgit.transport.URIish(repoSettings.repositoryUrl)).call()

                    // 7. 设置上游分支为用户名
                    userGit.checkout().setCreateBranch(true).setName(userName).call()

                    // 8. 推送到远程仓库
                    val pushCommand = userGit.push().setRemote("origin")
                    if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                        pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                    }
                    pushCommand.call()

                    println("✅ 用户 $userName 的独立Git仓库已创建并推送到远程分支 $userName")
                } else {
                    println("⚠️ 仓库未配置，无法推送用户分支到远程")
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
     * 同步所有用户数据
     * 现在每个用户目录都是独立的Git仓库，需要分别更新
     */
    suspend fun syncAllBranches(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val repoSettings = RepositorySettingsManager.getCurrentSettings()
            if (!repoSettings.enabled || repoSettings.repositoryUrl.isBlank()) {
                println("仓库未配置，只合并本地数据")
                // 即使仓库未配置，也要合并本地数据
                mergeAllUserData()
                return@withContext Result.success(Unit)
            }

            // 获取所有用户目录
            val userDirs: Array<File> = usersDir.listFiles()?.filter { it.isDirectory }?.toTypedArray() ?: emptyArray()

            // 为每个用户仓库执行拉取操作
            for (userDir in userDirs) {
                val userName = userDir.name
                try {
                    val userGitDir = File(userDir, ".git")
                    if (userGitDir.exists()) {
                        val userGit = Git.open(userDir)
                        try {
                            val pullCommand = userGit.pull()
                            if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                                pullCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                            }
                            pullCommand.call()
                            println("✅ 已更新用户 $userName 的仓库")
                        } finally {
                            userGit.close()
                        }
                    }
                } catch (e: Exception) {
                    println("⚠️ 同步用户 $userName 仓库失败: ${e.message}")
                }
            }

            // 合并数据到本地缓存
            mergeAllUserData()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    /**
     * 推送当前用户的数据
     * 现在在用户的独立Git仓库中进行操作
     */
    suspend fun pushCurrentUserData(userName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userLocalDir = File(usersDir, userName)
            val userGitDir = File(userLocalDir, ".git")

            if (!userGitDir.exists()) {
                return@withContext Result.failure(Exception("用户 $userName 的Git仓库不存在"))
            }

            // 打开用户的Git仓库
            val userGit = Git.open(userLocalDir)
            try {
                // 添加所有更改
                userGit.add().addFilepattern(".").call()

                // 检查是否有更改需要提交
                val status = userGit.status().call()
                if (status.hasUncommittedChanges() || !status.untracked.isEmpty()) {
                    // 创建提交
                    userGit.commit().setMessage("Update data for user: $userName").call()
                }

                // 推送到远程仓库
                val repoSettings = RepositorySettingsManager.getCurrentSettings()
                val pushCommand = userGit.push().setRemote("origin")
                if (repoSettings.username.isNotBlank() && repoSettings.password.isNotBlank()) {
                    pushCommand.setCredentialsProvider(UsernamePasswordCredentialsProvider(repoSettings.username, repoSettings.password))
                }
                pushCommand.call()

                println("✅ 用户 $userName 的数据已推送到远程仓库")
                Result.success(Unit)
            } finally {
                userGit.close()
            }
        } catch (e: Exception) {
            println("❌ 推送用户 $userName 数据失败: ${e.message}")
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

    private suspend fun mergeAllUserData() = withContext(Dispatchers.IO) {
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
