package service

import data.SSHConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import utils.Logger
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * SSH 会话状态
 */
sealed class SSHSessionState {
    object Disconnected : SSHSessionState()
    object Connecting : SSHSessionState()
    object Connected : SSHSessionState()
    data class Error(val message: String) : SSHSessionState()
}

/**
 * SSH 客户端管理器
 * 负责单个 SSH 连接的管理
 */
class SSHClientManager(private val config: SSHConfig) {
    private var sshClient: SSHClient? = null
    private var session: Session? = null
    private var sftpManager: SFTPFileManager? = null

    private val _state = MutableStateFlow<SSHSessionState>(SSHSessionState.Disconnected)
    val state: StateFlow<SSHSessionState> = _state

    // 端口转发管理
    private val activeForwards = mutableMapOf<String, Any?>()
    private val portForwardingRules = mutableListOf<data.PortForwardingRuleData>()

    // 命令执行和日志监控的Job管理
    private var commandExecutionJob: kotlinx.coroutines.Job? = null
    private var logMonitoringJob: kotlinx.coroutines.Job? = null

    /**
     * 获取 SFTP 管理器
     */
    fun getSftpManager(): SFTPFileManager? = sftpManager

    /**
     * 连接到 SSH 服务器
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _state.value = SSHSessionState.Connecting

            // 创建 SSH 客户端
            val client = SSHClient()
            client.addHostKeyVerifier(PromiscuousVerifier()) // 跳过主机密钥验证
            // 注意：已设置PromiscuousVerifier跳过主机密钥验证，无需加载known_hosts文件

            // 连接到服务器
            client.connect(config.host, config.port)

            // 认证
            when {
                // 优先使用指定的私钥
                config.privateKeyPath.isNotEmpty() -> {
                    // 检查是否是私钥内容（以 -----BEGIN 开头）
                    val isKeyContent = config.privateKeyPath.trim().startsWith("-----BEGIN")
                    
                    if (isKeyContent) {
                        // 使用私钥内容，创建临时文件
                        println("✓ 使用私钥内容进行认证")
                        var tempKeyFile: File? = null
                        try {
                            // 创建临时文件
                            tempKeyFile = File.createTempFile("ssh_key_", ".key")
                            tempKeyFile.writeText(config.privateKeyPath, Charsets.UTF_8)
                            // 设置临时文件权限（仅所有者可读）
                            tempKeyFile.setReadable(false, false)
                            tempKeyFile.setReadable(true, true)
                            tempKeyFile.setWritable(false, false)
                            
                            val keyPath = tempKeyFile.absolutePath
                            
                            if (config.privateKeyPassphrase.isNotEmpty()) {
                                // 使用带密码的私钥
                                client.authPublickey(config.username, keyPath, config.privateKeyPassphrase)
                            } else {
                                // 使用无密码的私钥
                                client.authPublickey(config.username, keyPath)
                            }
                            
                            // 认证成功后，删除临时文件
                            tempKeyFile.delete()
                            tempKeyFile = null
                        } catch (e: Exception) {
                            // 确保临时文件被删除
                            tempKeyFile?.delete()
                            throw Exception("加载私钥内容失败: ${e.message}", e)
                        }
                    } else {
                        // 使用私钥文件路径
                        val keyFile = File(config.privateKeyPath)
                        if (keyFile.exists()) {
                            println("✓ 使用指定私钥文件: ${config.privateKeyPath}")
                            if (config.privateKeyPassphrase.isNotEmpty()) {
                                // 使用带密码的私钥
                                client.authPublickey(config.username, config.privateKeyPath, config.privateKeyPassphrase)
                            } else {
                                // 使用无密码的私钥
                                client.authPublickey(config.username, config.privateKeyPath)
                            }
                        } else {
                            throw Exception("私钥文件不存在: ${config.privateKeyPath}")
                        }
                    }
                }
                // 如果有密码，使用密码认证
                config.password.isNotEmpty() -> {
                    println("✓ 使用密码认证")
                    client.authPassword(config.username, config.password)
                }
                // 尝试使用默认私钥
                else -> {
                    val keyProvider = findDefaultKey(client)
                    if (keyProvider != null) {
                        if (config.privateKeyPassphrase.isNotEmpty()) {
                            // 使用带密码的默认私钥
                            val homeDir = System.getProperty("user.home")
                            val defaultKeys = listOf(
                                "$homeDir/.ssh/id_ed25519",
                                "$homeDir/.ssh/id_rsa",
                                "$homeDir/.ssh/id_ecdsa",
                                "$homeDir/.ssh/id_dsa"
                            )

                            var authenticated = false
                            for (keyPath in defaultKeys) {
                                val keyFile = File(keyPath)
                                if (keyFile.exists()) {
                                    try {
                                        client.authPublickey(config.username, keyPath, config.privateKeyPassphrase)
                                        println("✓ 使用密码认证默认私钥: $keyPath")
                                        authenticated = true
                                        break
                                    } catch (e: Exception) {
                                        println("✗ 密码认证私钥 $keyPath 失败: ${e.message}")
                                        continue
                                    }
                                }
                            }

                            if (!authenticated) {
                                throw Exception("所有默认私钥认证失败")
                            }
                        } else {
                            // 使用无密码的默认私钥
                            client.authPublickey(config.username, keyProvider)
                        }
                    } else {
                        throw Exception("未找到可用的私钥，且未提供密码")
                    }
                }
            }

            // 打开会话
            val sess = client.startSession()

            sshClient = client
            session = sess

            // 初始化 SFTP 管理器
            sftpManager = SFTPFileManager(client)

            _state.value = SSHSessionState.Connected

            Logger.info("SSH连接成功: ${config.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMessage = "连接失败: ${e.message}"
            _state.value = SSHSessionState.Error(errorMessage)
            Logger.error("SSH连接失败: ${config.name} - $errorMessage")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 查找默认私钥
     */
    private fun findDefaultKey(client: SSHClient): KeyProvider? {
        val homeDir = System.getProperty("user.home")
        val defaultKeys = listOf(
            "$homeDir/.ssh/id_ed25519",
            "$homeDir/.ssh/id_rsa",
            "$homeDir/.ssh/id_ecdsa",
            "$homeDir/.ssh/id_dsa"
        )

        for (keyPath in defaultKeys) {
            val keyFile = File(keyPath)
            if (keyFile.exists()) {
                try {
                    val keyProvider = client.loadKeys(keyPath)
                    println("✓ 成功加载默认私钥: $keyPath")
                    return keyProvider
                } catch (e: Exception) {
                    println("✗ 加载私钥 $keyPath 失败: ${e.message}")
                    continue
                }
            }
        }

        println("⚠️ 未找到可用的默认私钥")
        return null
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        try {
            // 停止所有命令执行和日志监控
            stopAllStreams()

            // 停止所有端口转发
            stopAllPortForwarding()

            // 清理 SFTP 管理器
            sftpManager?.disconnect()
            sftpManager = null

            session?.close()
            sshClient?.disconnect()
            sshClient = null
            session = null
            _state.value = SSHSessionState.Disconnected
            Logger.info("SSH连接已断开: ${config.name}")
        } catch (e: Exception) {
            Logger.error("SSH断开连接时出错: ${config.name} - ${e.message}", e)
        }
    }

    /**
     * 停止所有端口转发
     */
    private fun stopAllPortForwarding() {
        activeForwards.values.forEach { forward ->
            try {
                if (forward is java.net.ServerSocket) {
                    forward.close()
                }
                // RemotePortForwarder.Forward 不需要手动关闭
            } catch (e: Exception) {
                println("✗ 关闭端口转发失败: ${e.message}")
            }
        }
        activeForwards.clear()

        // 更新所有规则状态为未激活
        portForwardingRules.replaceAll { it.copy(autoStart = false) }
    }

    /**
     * 停止所有命令执行和日志监控流
     */
    private fun stopAllStreams() {
        commandExecutionJob?.cancel()
        commandExecutionJob = null
        logMonitoringJob?.cancel()
        logMonitoringJob = null
    }

    /**
     * 停止当前正在运行的命令执行和日志监控
     * 用于在对话框关闭时清理资源
     */
    fun stopCurrentStreams() {
        commandExecutionJob?.cancel()
        commandExecutionJob = null
        logMonitoringJob?.cancel()
        logMonitoringJob = null
    }

    /**
     * 执行命令
     */
    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (_state.value != SSHSessionState.Connected) {
                return@withContext Result.failure(Exception("未连接到服务器"))
            }

            val client = sshClient ?: return@withContext Result.failure(Exception("SSH客户端未初始化"))
            val cmdSession = client.startSession()

            try {
                val cmd = cmdSession.exec(command)

                // 读取输出
                val output = cmd.inputStream.bufferedReader(Charsets.UTF_8).readText()
                val errorOutput = cmd.errorStream.bufferedReader(Charsets.UTF_8).readText()

                // 等待命令完成
                cmd.join(10, TimeUnit.SECONDS) // 最多等待10秒

                cmdSession.close()

                val fullOutput = if (errorOutput.isNotEmpty()) {
                    "$output\n$errorOutput"
                } else {
                    output
                }

                Result.success(fullOutput.trim())
            } catch (e: Exception) {
                cmdSession.close()
                throw e
            }
        } catch (e: Exception) {
            println("执行命令失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 执行多行命令（按行顺序执行）
     */
    suspend fun executeMultiLineCommandStream(
        script: String,
        workingDirectory: String = "",
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (_state.value != SSHSessionState.Connected) {
                return@withContext Result.failure(Exception("未连接到服务器"))
            }

            val client = sshClient ?: return@withContext Result.failure(Exception("SSH客户端未初始化"))

            // 取消之前的命令执行任务
            commandExecutionJob?.cancel()
            commandExecutionJob = null

            // 创建新的命令执行任务
            commandExecutionJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 按行分割脚本
                    val lines = script.lines().filter { it.trim().isNotEmpty() }

                    for (line in lines) {
                        try {
                            // 跳过注释行（以#开头的行）
                            if (line.trim().startsWith("#")) {
                                onOutput("# $line (跳过注释)\n")
                                continue
                            }

                            // 显示正在执行的命令
                            onOutput("$ $line\n")

                            // 构造执行命令，如果有工作目录则先切换目录
                            val finalCommand = if (workingDirectory.isNotBlank()) {
                                "cd \"${workingDirectory}\" && $line"
                            } else {
                                line
                            }

                            // 创建新的会话执行单行命令
                            val cmdSession = client.startSession()
                            val cmd = cmdSession.exec(finalCommand)

                            // 异步读取标准输出
                            val outputJob = launch {
                                try {
                                    val reader = cmd.inputStream.bufferedReader(Charsets.UTF_8)
                                    reader.useLines { outputLines ->
                                        outputLines.forEach { outputLine ->
                                            onOutput("$outputLine\n")
                                        }
                                    }
                                } catch (e: Exception) {
                                    onError("读取输出失败: ${e.message}\n")
                                }
                            }

                            // 异步读取错误输出
                            val errorJob = launch {
                                try {
                                    val reader = cmd.errorStream.bufferedReader(Charsets.UTF_8)
                                    reader.useLines { errorLines ->
                                        errorLines.forEach { errorLine ->
                                            onError("$errorLine\n")
                                        }
                                    }
                                } catch (e: Exception) {
                                    onError("读取错误输出失败: ${e.message}\n")
                                }
                            }

                            // 等待命令完成，最多30秒
                            val completed = withTimeoutOrNull(30000) {
                                cmd.join()
                                true
                            } ?: false

                            if (!completed) {
                                onError("命令执行超时: $line\n")
                            }

                            // 等待输出读取完成
                            outputJob.join()
                            errorJob.join()

                            cmdSession.close()

                            // 添加一行分隔符
                            onOutput("---\n")

                        } catch (e: Exception) {
                            onError("执行异常 [$line]: ${e.message}\n")
                        }
                    }

                    onComplete()
                } catch (e: Exception) {
                    onError("多行命令执行异常: ${e.message}\n")
                    onComplete()
                }
            }

            // 等待命令执行任务完成
            commandExecutionJob?.join()
            commandExecutionJob = null

            Result.success(Unit)
        } catch (e: Exception) {
            println("执行多行命令失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 执行命令并返回实时输出流
     */
    suspend fun executeCommandStream(
        command: String,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: () -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (_state.value != SSHSessionState.Connected) {
                return@withContext Result.failure(Exception("未连接到服务器"))
            }

            val client = sshClient ?: return@withContext Result.failure(Exception("SSH客户端未初始化"))
            val cmdSession = client.startSession()

            try {
                val cmd = cmdSession.exec(command)

                // 取消之前的命令执行任务
                commandExecutionJob?.cancel()
                commandExecutionJob = null

                // 创建新的命令执行任务
                commandExecutionJob = CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // 异步读取标准输出
                        val outputJob = launch {
                            try {
                                val reader = cmd.inputStream.bufferedReader(Charsets.UTF_8)
                                reader.useLines { lines ->
                                    lines.forEach { line ->
                                        onOutput("$line\n")
                                    }
                                }
                            } catch (e: Exception) {
                                onError("读取输出失败: ${e.message}\n")
                            }
                        }

                        // 异步读取错误输出
                        val errorJob = launch {
                            try {
                                val reader = cmd.errorStream.bufferedReader(Charsets.UTF_8)
                                reader.useLines { lines ->
                                    lines.forEach { line ->
                                        onError("$line\n")
                                    }
                                }
                            } catch (e: Exception) {
                                onError("读取错误输出失败: ${e.message}\n")
                            }
                        }

                        // 等待命令完成，最多30秒
                        val completed = withTimeoutOrNull(30000) {
                            cmd.join()
                            true
                        } ?: false

                        if (!completed) {
                            onError("命令执行超时\n")
                        }

                        // 等待输出读取完成
                        outputJob.join()
                        errorJob.join()

                        cmdSession.close()
                        onComplete()
                    } catch (e: Exception) {
                        cmdSession.close()
                        throw e
                    }
                }

                // 等待命令执行任务完成
                commandExecutionJob?.join()
                commandExecutionJob = null

                Result.success(Unit)
            } catch (e: Exception) {
                cmdSession.close()
                commandExecutionJob?.cancel()
                commandExecutionJob = null
                throw e
            }
        } catch (e: Exception) {
            println("执行命令失败: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * 执行tail命令来监控日志文件
     */
    suspend fun tailLogFile(
        logFile: String,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (_state.value != SSHSessionState.Connected) {
                return@withContext Result.failure(Exception("未连接到服务器"))
            }

            val client = sshClient ?: return@withContext Result.failure(Exception("SSH客户端未初始化"))

            // 先检查文件是否存在
            val checkResult = executeCommand("test -f \"$logFile\" && echo 'exists' || echo 'not exists'")
            if (checkResult.getOrNull()?.trim() != "exists") {
                return@withContext Result.failure(Exception("日志文件不存在: $logFile"))
            }

            // 取消之前的日志监控任务
            logMonitoringJob?.cancel()
            logMonitoringJob = null

            // 执行tail命令监控日志
            val tailCommand = "tail -f -n 200 \"$logFile\""
            logMonitoringJob = CoroutineScope(Dispatchers.IO).launch {
                val result = executeCommandStream(
                    command = tailCommand,
                    onOutput = onOutput,
                    onError = onError,
                    onComplete = { /* 日志监控通常不会自然结束 */ }
                )
                if (result.isFailure) {
                    onError("日志监控失败: ${result.exceptionOrNull()?.message}\n")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            println("监控日志文件失败: ${e.message}")
            logMonitoringJob?.cancel()
            logMonitoringJob = null
            Result.failure(e)
        }
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean {
        return _state.value == SSHSessionState.Connected
    }

    /**
     * 获取SSH客户端（仅在已连接状态下使用）
     */
    fun getSSHClient(): SSHClient? {
        return if (isConnected()) sshClient else null
    }

    /**
     * 获取SFTP管理器（仅在已连接状态下使用）
     */
    fun getSFTPManager(): SFTPFileManager? {
        return if (isConnected()) sftpManager else null
    }

    /**
     * 添加端口转发规则
     */
    fun addPortForwardingRule(rule: data.PortForwardingRuleData) {
        portForwardingRules.add(rule)
        println("➕ 添加端口转发规则: ${rule.description} (${rule.localPort} -> ${rule.remoteHost}:${rule.remotePort})")
    }

    /**
     * 移除端口转发规则
     */
    fun removePortForwardingRule(ruleId: String) {
        // 如果规则正在运行，先停止
        if (activeForwards.containsKey(ruleId)) {
            try {
                // 直接停止转发，不使用suspend函数
                val forward = activeForwards[ruleId]
                if (forward is java.net.ServerSocket) {
                    forward.close()
                }
                activeForwards.remove(ruleId)
                println("✓ 端口转发已停止: $ruleId")
            } catch (e: Exception) {
                println("✗ 停止端口转发失败: ${e.message}")
            }
        }
        portForwardingRules.removeAll { it.id == ruleId }
        println("➖ 移除端口转发规则: $ruleId")
    }

    /**
     * 获取端口转发规则
     */
    fun getPortForwardingRules(): List<data.PortForwardingRuleData> {
        return portForwardingRules.toList()
    }

    /**
     * 获取当前活跃的端口转发规则ID列表
     */
    fun getActivePortForwardingRuleIds(): Set<String> {
        return activeForwards.keys
    }

    /**
     * 启动端口转发
     */
    suspend fun startPortForwarding(rule: data.PortForwardingRuleData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = sshClient ?: return@withContext Result.failure(Exception("SSH未连接"))

            // 检查是否已经在运行
            if (activeForwards.containsKey(rule.id)) {
                println("⚠️ 端口转发已在运行: ${rule.description}")
                data.PortManager.setPortRuleStatus(rule.id, true)
                return@withContext Result.success(Unit)
            }

            when (rule.type) {
                "LOCAL" -> {
                    try {
                        // 检查本地端口是否可用
                        val testSocket = java.net.ServerSocket()
                        testSocket.setReuseAddress(true)
                        testSocket.bind(java.net.InetSocketAddress("127.0.0.1", rule.localPort))
                        testSocket.close()

                        // 本地转发: 本地端口 -> 远程主机:远程端口
                        val serverSocket = java.net.ServerSocket(rule.localPort)
                        val params = net.schmizz.sshj.connection.channel.direct.Parameters(
                            "127.0.0.1",
                            rule.localPort,
                            rule.remoteHost,
                            rule.remotePort
                        )
                        val forwarder = client.newLocalPortForwarder(params, serverSocket)

                        // 启动转发
                        Thread {
                            try {
                                forwarder.listen()
                            } catch (e: Exception) {
                                println("✗ 本地转发异常: ${e.message}")
                                // 转发失败时清理资源
                                try {
                                    serverSocket.close()
                                } catch (closeException: Exception) {
                                    println("✗ 关闭ServerSocket异常: ${closeException.message}")
                                }
                            }
                        }.start()

                        activeForwards[rule.id] = serverSocket
                        println("✓ 本地转发启动: 127.0.0.1:${rule.localPort} -> ${rule.remoteHost}:${rule.remotePort}")
                    } catch (e: java.net.BindException) {
                        return@withContext Result.failure(Exception("本地端口 ${rule.localPort} 被占用或无权限绑定"))
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("本地转发启动失败: ${e.message}"))
                    }
                }

                "REMOTE" -> {
                    try {
                        // 远程转发: 远程端口 -> 本地主机:本地端口
                        val forward = client.remotePortForwarder.bind(
                            net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder.Forward(rule.remotePort),
                            net.schmizz.sshj.connection.channel.forwarded.SocketForwardingConnectListener(
                                java.net.InetSocketAddress(rule.remoteHost, rule.localPort)
                            )
                        )

                        activeForwards[rule.id] = forward
                        println("✓ 远程转发启动: remote:${rule.remotePort} -> ${rule.remoteHost}:${rule.localPort}")
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("远程转发启动失败: ${e.message}"))
                    }
                }

                "DYNAMIC" -> {
                    try {
                        // 检查本地端口是否可用
                        val testSocket = java.net.ServerSocket()
                        testSocket.setReuseAddress(true)
                        testSocket.bind(java.net.InetSocketAddress("127.0.0.1", rule.localPort))
                        testSocket.close()

                        // 动态转发: SOCKS代理
                        val serverSocket = java.net.ServerSocket(rule.localPort)
                        val params = net.schmizz.sshj.connection.channel.direct.Parameters(
                            "127.0.0.1",
                            rule.localPort,
                            "localhost",
                            0
                        )
                        val forwarder = client.newLocalPortForwarder(params, serverSocket)

                        Thread {
                            try {
                                forwarder.listen()
                            } catch (e: Exception) {
                                println("✗ 动态转发异常: ${e.message}")
                                // 转发失败时清理资源
                                try {
                                    serverSocket.close()
                                } catch (closeException: Exception) {
                                    println("✗ 关闭ServerSocket异常: ${closeException.message}")
                                }
                            }
                        }.start()

                        activeForwards[rule.id] = serverSocket
                        println("✓ SOCKS代理启动: 127.0.0.1:${rule.localPort}")
                    } catch (e: java.net.BindException) {
                        return@withContext Result.failure(Exception("本地端口 ${rule.localPort} 被占用或无权限绑定"))
                    } catch (e: Exception) {
                        return@withContext Result.failure(Exception("SOCKS代理启动失败: ${e.message}"))
                    }
                }

                else -> {
                    return@withContext Result.failure(Exception("不支持的转发类型: ${rule.type}"))
                }
            }

            // 通知PortManager状态更新
            data.PortManager.setPortRuleStatus(rule.id, true)

            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ 启动端口转发失败: ${e.message}")
            Result.failure(Exception("启动端口转发失败: ${e.message}", e))
        }
    }

    /**
     * 停止端口转发
     */
    suspend fun stopPortForwarding(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            println("🔄 开始停止端口转发: $ruleId")
            val forward = activeForwards[ruleId]
            println("📋 活跃转发中的条目: ${activeForwards.keys}")

            if (forward is java.net.ServerSocket) {
                println("🔌 关闭ServerSocket: $ruleId")
                forward.close()
            } else if (forward != null) {
                println("📝 其他类型的转发对象: ${forward::class.java.simpleName}")
            } else {
                println("⚠️ 未找到转发对象: $ruleId")
            }

            // RemotePortForwarder.Forward 不需要手动关闭，会在 SSH 连接关闭时自动清理
            val removed = activeForwards.remove(ruleId)
            println("🗑️ 从活跃转发中移除: $ruleId (成功: ${removed != null})")

            // 通知PortManager状态更新
            data.PortManager.setPortRuleStatus(ruleId, false)
            println("✅ 端口转发已停止并更新状态: $ruleId")

            Result.success(Unit)
        } catch (e: Exception) {
            println("❌ 停止端口转发异常: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("停止端口转发失败: ${e.message}", e))
        }
    }

    /**
     * 加载端口转发规则
     */
    fun loadPortForwardingRules(rules: List<data.PortForwardingRuleData>) {
        portForwardingRules.clear()
        portForwardingRules.addAll(rules)

        // 自动启动标记为true的规则
        rules.filter { it.autoStart }.forEach { rule ->
            CoroutineScope(Dispatchers.IO).launch {
                startPortForwarding(rule)
            }
        }

        println("✓ 加载了 ${rules.size} 个端口转发规则")
    }
}

/**
 * SSH 会话管理器
 * 管理所有 SSH 连接会话
 */
object SSHSessionManager {
    private val sessions = mutableMapOf<String, SSHClientManager>()

    /**
     * 创建或获取会话
     */
    fun getOrCreateSession(config: SSHConfig): SSHClientManager {
        return sessions.getOrPut(config.name) {
            SSHClientManager(config)
        }
    }

    /**
     * 获取会话
     */
    fun getSession(name: String): SSHClientManager? {
        return sessions[name]
    }

    /**
     * 移除会话
     */
    fun removeSession(name: String) {
        sessions[name]?.disconnect()
        sessions.remove(name)
    }

    /**
     * 获取所有会话
     */
    fun getAllSessions(): Map<String, SSHClientManager> {
        return sessions.toMap()
    }

    /**
     * 断开所有连接
     */
    fun disconnectAll() {
        sessions.values.forEach { it.disconnect() }
        sessions.clear()
    }
}
