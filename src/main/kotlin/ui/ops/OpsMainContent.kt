package ui.ops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import theme.*
import ui.common.OpsDrawerTab
import java.io.File

/**
 * 运维主操作区内容（两个卡片）
 */
@Composable
fun OpsMainContent(
    config: SSHConfigData,
    selectedOpsTab: OpsDrawerTab,
    sshConnectionStates: Map<String, Boolean>,
    sshConnectionTimes: Map<String, Long>,
    showPortDialog: Boolean,
    editingPortRule: data.PortForwardingRuleData?,
    showCommandDialog: Boolean,
    editingCommandRule: data.CommandRuleData?,
    executingCommandRule: data.CommandRuleData?,
    autoReconnectEnabled: Map<String, Boolean> = emptyMap(),
    reconnectingStates: Map<String, Boolean> = emptyMap(),
    onTabSelected: (OpsDrawerTab) -> Unit = {},
    onConnect: (String) -> Unit = {},
    onDisconnect: (String) -> Unit = {},
    onShowPortDialog: () -> Unit = {},
    onHidePortDialog: () -> Unit = {},
    onEditingPortRule: (data.PortForwardingRuleData?) -> Unit = {},
    onShowCommandDialog: () -> Unit = {},
    onHideCommandDialog: () -> Unit = {},
    onEditingCommandRule: (data.CommandRuleData?) -> Unit = {},
    onExecutingCommandRule: (data.CommandRuleData?) -> Unit = {},
    onAutoReconnectChanged: (String, Boolean) -> Unit = { _, _ -> },
    onStatusMessage: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val isConnected = sshConnectionStates[config.id] == true

    // 定时更新连接时间显示
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isConnected) {
        if (isConnected) {
            while (true) {
                kotlinx.coroutines.delay(1000) // 每秒更新一次
                currentTime = System.currentTimeMillis()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimensions.PaddingScreen),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL)
    ) {

        // 状态面板
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp,
            shape = RoundedCornerShape(AppDimensions.RadiusL)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.PaddingCard)
            ) {
                // 卡片标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "状态",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text(
                        text = "连接状态",
                        style = AppTypography.BodyLarge,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                // 同一行显示：自动重连开关、连接状态和时长、连接/断开按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左侧：自动重连开关
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                    ) {
                        Text(
                            text = "自动重连",
                            style = AppTypography.BodySmall,
                            color = AppColors.TextPrimary
                        )
                        Switch(
                            checked = autoReconnectEnabled[config.id] ?: true, // 默认开启
                            onCheckedChange = { enabled ->
                                onAutoReconnectChanged(config.id, enabled)
                            },
                            modifier = Modifier.scale(0.8f) // 稍微缩小开关尺寸
                        )
                    }

                    // 中间：连接状态指示器
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 状态圆点
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = if (isConnected) AppColors.Success else AppColors.TextDisabled
                        ) {}

                        // 状态文本和时间
                        val statusText = if (isConnected) {
                            val connectionTime = sshConnectionTimes[config.id]
                            if (connectionTime != null) {
                                val duration = (currentTime - connectionTime) / 1000
                                val minutes = duration / 60
                                val seconds = duration % 60
                                "已连接 ${minutes}:${String.format("%02d", seconds)}"
                            } else {
                                "已连接"
                            }
                        } else if (reconnectingStates[config.id] == true) {
                            "重连中"
                        } else {
                            "未连接"
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = statusText,
                                style = AppTypography.Caption,
                                color = AppColors.TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            if (reconnectingStates[config.id] == true) {
                                Spacer(modifier = Modifier.width(2.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.dp,
                                    color = AppColors.Primary
                                )
                            }
                        }
                    }

                    // 右侧：连接/断开按钮和终端按钮
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isConnected) {
                            OutlinedButton(
                                onClick = { onDisconnect(config.id) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "断开",
                                    modifier = Modifier.size(14.dp),
                                    tint = AppColors.Error
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("断开连接", style = AppTypography.Caption)
                            }
                        } else {
                            Button(
                                onClick = { onConnect(config.id) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "连接",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("连接", style = AppTypography.Caption)
                            }
                        }

                        // 终端按钮 - 随时可用，不依赖连接状态
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    // 检查认证方式并提供相应的用户提示
                                    val sshConfig = SSHConfig.fromSSHConfigData(config)
                                    val keyPathOrContent = sshConfig.privateKeyPath.trim()
                                    val isKeyContent = keyPathOrContent.startsWith("-----BEGIN") && keyPathOrContent.contains("PRIVATE KEY-----")

                                    val authType = when {
                                        isKeyContent -> {
                                            if (sshConfig.privateKeyPassphrase.isNotEmpty()) "key_with_passphrase" else "key"
                                        }
                                        sshConfig.password.isNotEmpty() -> "password"
                                        else -> "none"
                                    }

                                    // 根据认证方式显示不同的提示信息
                                    when (authType) {
                                        "password" -> {
                                            onStatusMessage("密码已拷贝到剪贴板，请在终端中粘贴使用")
                                        }
                                        "key_with_passphrase" -> {
                                            onStatusMessage("密钥密码短语已拷贝到剪贴板，请在终端中粘贴使用")
                                        }
                                        "key" -> {
                                            onStatusMessage("正在打开终端连接...")
                                        }
                                        else -> {
                                            onStatusMessage("正在打开终端连接...")
                                        }
                                    }

                                    val result = openTerminalWithSSH(config)
                                    if (result.isFailure) {
                                        val errorMessage = result.exceptionOrNull()?.message ?: "打开终端失败"
                                        onStatusMessage("终端连接失败: $errorMessage")
                                        println("❌ 打开终端失败: $errorMessage")
                                    } else {
                                        if (authType == "password" || authType == "key_with_passphrase") {
                                            onStatusMessage("终端已打开，密码已在剪贴板中，请在SSH提示时粘贴")
                                        } else {
                                            onStatusMessage("终端连接成功")
                                        }
                                        println("✅ 正在打开终端连接到 ${config.name}")
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Terminal,
                                contentDescription = "打开终端",
                                modifier = Modifier.size(14.dp),
                                tint = AppColors.Primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("终端", style = AppTypography.Caption)
                        }
                    }
                }
            }
        }

        // 命令面板
        Card(
            modifier = Modifier.fillMaxWidth(), // 改为fillMaxWidth，不使用weight
            elevation = 4.dp,
            shape = RoundedCornerShape(AppDimensions.RadiusL)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.PaddingCard)
            ) {
                // 卡片标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "命令",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text(
                        text = "命令执行",
                        style = AppTypography.BodyLarge,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                // 命令按钮列表
                val commandRules by data.CommandManager.commandRules.collectAsState()

                if (commandRules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp), // 为空状态设置固定高度
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AppColors.TextDisabled
                            )
                            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                            Text(
                                text = "暂无命令配置",
                                style = AppTypography.BodyMedium,
                                color = AppColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                            Text(
                                text = "请在右侧工具栏中添加",
                                style = AppTypography.Caption,
                                color = AppColors.TextDisabled
                            )
                        }
                    }
                } else {
                    // 使用自适应网格布局，根据窗口宽度自动排列按钮
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(), // 根据内容自适应高度
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS),
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS),
                        maxItemsInEachRow = Int.MAX_VALUE
                    ) {
                        commandRules.forEach { rule ->
                            CommandButton(
                                commandRule = rule,
                                isConnected = isConnected,
                                onExecuteCommand = { executedRule ->
                                    // 使用二次确认和详细执行对话框
                                    println("CommandButton: 执行命令 ${executedRule.name}")
                                    onExecutingCommandRule(executedRule)
                                },
                                onStatusMessage = onStatusMessage
                            )
                        }
                    }
                }
            }
        }

                // 端口转发面板
        Card(
            modifier = Modifier.weight(1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(AppDimensions.RadiusL)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.PaddingCard)
            ) {
                // 卡片标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Cable,
                        contentDescription = "端口转发",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text(
                        text = "端口转发",
                        style = AppTypography.BodyLarge,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                // 端口转发列表
                val portRules by data.PortManager.portRules.collectAsState()
                val portStatuses by data.PortManager.portStatuses.collectAsState()

                if (portRules.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Cable,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = AppColors.TextDisabled
                            )
                            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                            Text(
                                text = "暂无端口转发规则",
                                style = AppTypography.BodyMedium,
                                color = AppColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                            Text(
                                text = "请在右侧工具栏中添加",
                                style = AppTypography.Caption,
                                color = AppColors.TextDisabled
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                    ) {
                        items(portRules) { rule ->
                            PortForwardingListItem(
                                rule = rule,
                                isActive = portStatuses[rule.id] ?: false,
                                isConnected = isConnected,
                                onToggleStatus = { ruleId ->
                                    scope.launch {
                                        val result = data.PortManager.togglePortRuleStatus(ruleId)
                                        if (result.isFailure) {
                                            val errorMessage = result.exceptionOrNull()?.message ?: "操作失败"
                                            onStatusMessage("端口转发操作失败: $errorMessage")
                                            println("❌ 端口转发操作失败: $errorMessage")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 根据操作系统自动选择终端并打开SSH连接
 */
private suspend fun openTerminalWithSSH(config: SSHConfigData): Result<Unit> {
    return withContext(Dispatchers.IO) {
        var tempKeyFile: File? = null
        var scriptFile: File? = null

        try {
            val osName = System.getProperty("os.name").lowercase()

            // 处理密钥内容（如果是应用维护的密钥，需要创建临时文件）
            val sshConfig = SSHConfig.fromSSHConfigData(config)

            // 如果privateKeyPath看起来像是密钥内容（而不是文件路径），创建临时文件
            val keyPathOrContent = sshConfig.privateKeyPath.trim()
            println("🔍 密钥字段内容预览: ${keyPathOrContent.take(50)}${if (keyPathOrContent.length > 50) "..." else ""}")

            // 改进的密钥格式检测
            val isKeyContent = when {
                // PEM格式私钥（传统格式）
                keyPathOrContent.startsWith("-----BEGIN ") && keyPathOrContent.contains(" PRIVATE KEY-----") -> {
                    println("🔑 检测到PEM格式私钥内容")
                    true
                }
                // OpenSSH新格式私钥
                keyPathOrContent.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----") -> {
                    println("🔑 检测到OpenSSH格式私钥内容")
                    true
                }
                // 检查是否是公钥格式（如果是公钥，则不是我们要用的私钥）
                keyPathOrContent.startsWith("ssh-rsa") ||
                keyPathOrContent.startsWith("ssh-ed25519") ||
                keyPathOrContent.startsWith("ssh-dss") ||
                keyPathOrContent.startsWith("ecdsa-sha2-nistp") -> {
                    println("⚠️ 检测到公钥内容，请确保使用私钥内容")
                    println("💡 提示：私钥通常以'-----BEGIN'开头，公钥以'ssh-'开头")
                    false
                }
                // 检查是否包含私钥特征
                keyPathOrContent.contains("BEGIN ") && keyPathOrContent.contains("PRIVATE KEY") -> {
                    println("🔑 检测到私钥内容（通用格式）")
                    true
                }
                // 如果内容很长且包含换行符，可能是密钥内容
                keyPathOrContent.length > 500 && keyPathOrContent.contains("\n") -> {
                    println("🔑 检测到长文本内容，视为密钥内容")
                    true
                }
                // 其他情况认为是文件路径
                else -> {
                    println("📁 视为文件路径: $keyPathOrContent")
                    false
                }
            }

            val actualKeyPath = if (isKeyContent) {
                // 这看起来像是密钥内容，创建临时文件
                tempKeyFile = File(System.getProperty("java.io.tmpdir"), "ssh_key_${System.currentTimeMillis()}_${Thread.currentThread().id}.pem")

                try {
                    // 确保密钥内容格式正确，特别是换行符
                    val normalizedKeyContent = if (keyPathOrContent.contains("\\n")) {
                        // 如果内容包含转义的换行符，转换回来
                        keyPathOrContent.replace("\\n", "\n")
                    } else {
                        // 确保以换行符结尾
                        keyPathOrContent.trimEnd() + "\n"
                    }

                    // 写入密钥内容
                    tempKeyFile.writeText(normalizedKeyContent, Charsets.UTF_8)
                    println("📝 已写入密钥内容到临时文件: ${tempKeyFile.absolutePath}")
                    println("📏 密钥内容长度: ${normalizedKeyContent.length} 字符")

                    // 在Unix-like系统上尝试设置文件权限
                    if (!osName.contains("windows")) {
                        try {
                            // 使用Runtime执行chmod命令设置权限
                            val chmodProcess = Runtime.getRuntime().exec(arrayOf("chmod", "600", tempKeyFile.absolutePath))
                            val chmodExitCode = chmodProcess.waitFor()
                            if (chmodExitCode == 0) {
                                println("🔒 已设置密钥文件权限为600")
                            } else {
                                println("⚠️ 设置文件权限失败，使用Java方法")
                                tempKeyFile.setReadable(true, true)   // 只给自己读取权限
                                tempKeyFile.setWritable(true, true)   // 只给自己写入权限
                                tempKeyFile.setExecutable(false)      // 不允许执行
                            }
                        } catch (e: Exception) {
                            println("⚠️ chmod命令执行失败: ${e.message}")
                            // 回退到Java方法
                            tempKeyFile.setReadable(true, true)
                            tempKeyFile.setWritable(true, true)
                            tempKeyFile.setExecutable(false)
                        }
                    } else {
                        // Windows上使用Java方法
                        tempKeyFile.setReadable(true, true)
                        tempKeyFile.setWritable(true, true)
                        tempKeyFile.setExecutable(false)
                    }

                    // 验证文件是否成功创建
                    if (tempKeyFile.exists() && tempKeyFile.length() > 0) {
                        println("✅ 临时密钥文件创建成功，大小: ${tempKeyFile.length()} 字节")

                        // 验证文件内容是否正确写入
                        try {
                            val writtenContent = tempKeyFile.readText(Charsets.UTF_8)
                            if (writtenContent.trim() == normalizedKeyContent.trim()) {
                                println("✅ 文件内容验证通过")
                            } else {
                                println("⚠️ 文件内容可能有差异")
                                println("规范化内容长度: ${normalizedKeyContent.length}")
                                println("写入内容长度: ${writtenContent.length}")
                                println("前50字符对比:")
                                println("原始: ${normalizedKeyContent.take(50)}")
                                println("写入: ${writtenContent.take(50)}")
                            }

                            // 输出文件的前几行用于调试
                            val lines = writtenContent.lines()
                            println("📄 文件前3行预览:")
                            lines.take(3).forEachIndexed { index, line ->
                                println("  ${index + 1}: $line")
                            }

                            // 验证密钥格式
                            val isValidKeyFormat = when {
                                writtenContent.startsWith("-----BEGIN OPENSSH PRIVATE KEY-----") -> {
                                    println("✅ 检测到OpenSSH格式私钥")
                                    true
                                }
                                writtenContent.startsWith("-----BEGIN ") && writtenContent.contains(" PRIVATE KEY-----") -> {
                                    println("✅ 检测到PEM格式私钥")
                                    true
                                }
                                writtenContent.startsWith("ssh-rsa") ||
                                writtenContent.startsWith("ssh-ed25519") ||
                                writtenContent.startsWith("ssh-dss") ||
                                writtenContent.startsWith("ecdsa-sha2-nistp") -> {
                                    println("❌ 错误：检测到公钥内容，请提供私钥内容")
                                    false
                                }
                                else -> {
                                    println("⚠️ 密钥格式无法识别")
                                    println("文件开头: ${writtenContent.take(100)}")
                                    false
                                }
                            }

                            if (!isValidKeyFormat) {
                                throw Exception("无效的密钥格式：请确保提供的是私钥内容而不是公钥内容")
                            }

                        } catch (e: Exception) {
                            println("⚠️ 无法验证文件内容: ${e.message}")
                        }
                    } else {
                        throw Exception("临时密钥文件创建失败")
                    }

                    tempKeyFile.absolutePath
                } catch (e: Exception) {
                    println("❌ 创建临时密钥文件失败: ${e.message}")
                    throw e
                }
            } else {
                // 这是文件路径，直接使用
                keyPathOrContent
            }

            // 检查认证方式并处理密码
            val authType = when {
                isKeyContent -> {
                    val hasPassphrase = sshConfig.privateKeyPassphrase.isNotEmpty()
                    if (hasPassphrase) {
                        // 密钥认证 + 密码短语
                        println("🔐 密钥认证 + 密码短语")
                        "key_with_passphrase"
                    } else {
                        // 密钥认证（无密码）
                        println("🔑 密钥认证（无密码）")
                        "key"
                    }
                }
                sshConfig.password.isNotEmpty() -> {
                    // 密码认证
                    println("🔒 密码认证")
                    "password"
                }
                else -> {
                    // 无认证信息
                    println("❓ 无认证信息")
                    "none"
                }
            }

            // 如果是密码认证，自动拷贝密码到剪贴板
            if (authType == "password" && sshConfig.password.isNotEmpty()) {
                copyPasswordToClipboard(sshConfig.password)
                println("📋 密码已自动拷贝到剪贴板")
            } else if (authType == "key_with_passphrase" && sshConfig.privateKeyPassphrase.isNotEmpty()) {
                copyPasswordToClipboard(sshConfig.privateKeyPassphrase)
                println("📋 密钥密码短语已自动拷贝到剪贴板")
            }

            // 生成SSH命令
            val sshCommand = buildSSHCommand(config, actualKeyPath)

            // 根据操作系统选择终端
            val terminalCommand = when {
                osName.contains("windows") -> {
                    // Windows: 直接启动cmd并执行SSH命令
                    listOf("cmd.exe", "/c", "start", "cmd.exe", "/k", sshCommand)
                }
                osName.contains("mac") -> {
                    // macOS: 直接使用open命令打开Terminal并执行SSH
                    // 先创建包含SSH命令的脚本文件，然后用Terminal打开
                    val scriptContent = """
                        #!/bin/bash
                        $sshCommand
                        echo "按任意键退出..."
                        read -n 1
                    """.trimIndent()

                    scriptFile = File(System.getProperty("java.io.tmpdir"), "ssh_terminal_${System.currentTimeMillis()}.sh")
                    scriptFile.writeText(scriptContent, Charsets.UTF_8)

                    // 设置执行权限
                    try {
                        Runtime.getRuntime().exec(arrayOf("chmod", "+x", scriptFile.absolutePath)).waitFor()
                        println("📜 创建执行脚本: ${scriptFile.absolutePath}")
                        listOf("open", "-a", "Terminal", scriptFile.absolutePath)
                    } catch (e: Exception) {
                        // 回退到AppleScript方法
                        val escapedCommand = sshCommand.replace("\"", "\\\"").replace("'", "\\'")
                        val appleScript = """
                            tell application "Terminal"
                                activate
                                do script "$escapedCommand"
                            end tell
                        """.trimIndent()

                        val appleScriptFile = File(System.getProperty("java.io.tmpdir"), "ssh_terminal_${System.currentTimeMillis()}.scpt")
                        appleScriptFile.writeText(appleScript)
                        println("⚠️ chmod失败，使用AppleScript: ${e.message}")
                        listOf("osascript", appleScriptFile.absolutePath)
                    }
                }
                osName.contains("linux") -> {
                    // Linux: 尝试多种终端，按优先级
                    val terminals = listOf(
                        "gnome-terminal" to listOf("--", "bash", "-c", sshCommand),
                        "konsole" to listOf("--hold", "-e", sshCommand),
                        "xterm" to listOf("-hold", "-e", sshCommand),
                        "xfce4-terminal" to listOf("--command=$sshCommand", "--hold"),
                        "mate-terminal" to listOf("--command=$sshCommand", "--hold"),
                        "terminator" to listOf("-e", sshCommand),
                        "rxvt" to listOf("-hold", "-e", sshCommand)
                    )

                    val availableTerminal = terminals.firstOrNull { (cmd, _) -> isCommandAvailable(cmd) }
                    if (availableTerminal != null) {
                        listOf(availableTerminal.first) + availableTerminal.second
                    } else {
                        // 最后尝试 xterm
                        listOf("xterm", "-hold", "-e", sshCommand)
                    }
                }
                else -> {
                    throw IllegalStateException("不支持的操作系统: $osName")
                }
            }

            // 执行终端命令
            println("🔧 执行终端命令: ${terminalCommand.joinToString(" ")}")
            val processBuilder = ProcessBuilder(terminalCommand)

            // 设置工作目录和环境变量
            processBuilder.directory(File(System.getProperty("user.home")))
            processBuilder.environment()["PATH"] = System.getenv("PATH")

            val process = processBuilder.start()

            // 等待一小段时间确保进程启动
            try {
                Thread.sleep(500)
                if (process.isAlive) {
                    println("✅ 终端进程已启动")
                    if (tempKeyFile != null) {
                        println("🗝️ 已创建临时密钥文件: ${tempKeyFile.absolutePath}")
                    }
                    if (scriptFile != null) {
                        println("📜 已创建临时脚本文件: ${scriptFile.absolutePath}")
                    }
                } else {
                    val exitCode = process.exitValue()
                    println("⚠️ 终端进程已退出，退出码: $exitCode")
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // 清理临时文件（延迟删除，让SSH和脚本有时间执行）
            val filesToClean = mutableListOf<File>()
            tempKeyFile?.let { filesToClean.add(it) }
            scriptFile?.let { filesToClean.add(it) }

            if (filesToClean.isNotEmpty()) {
                kotlinx.coroutines.delay(20000) // 等待20秒，确保终端有足够时间启动和执行
                filesToClean.forEach { file ->
                    try {
                        if (file.exists()) {
                            val deleted = file.delete()
                            if (deleted) {
                                println("🗑️ 已清理临时文件: ${file.absolutePath}")
                            } else {
                                println("⚠️ 临时文件删除失败: ${file.absolutePath}")
                            }
                        }
                    } catch (e: Exception) {
                        println("⚠️ 清理临时文件失败: ${e.message}")
                    }
                }
            }
        }
    }
}

/**
 * 构建SSH连接命令
 */
private fun buildSSHCommand(config: SSHConfigData, actualKeyPath: String = ""): String {
    val sshConfig = SSHConfig.fromSSHConfigData(config)
    val commandParts = mutableListOf<String>()

    // 基础SSH命令
    commandParts.add("ssh")

    // SSH选项：禁用主机密钥检查（用于便捷连接），但这在生产环境中不推荐
    commandParts.add("-o")
    commandParts.add("StrictHostKeyChecking=no")

    // SSH选项：自动添加新主机密钥
    commandParts.add("-o")
    commandParts.add("UserKnownHostsFile=/dev/null")

    // SSH选项：只使用明确指定的密钥，不使用系统默认密钥
    commandParts.add("-o")
    commandParts.add("IdentitiesOnly=yes")

    // 端口设置
    if (sshConfig.port != 22) {
        commandParts.add("-p")
        commandParts.add(sshConfig.port.toString())
    }

    // 认证方式
    val finalKeyPath = actualKeyPath.ifEmpty { sshConfig.privateKeyPath.trim() }
    if (finalKeyPath.isNotEmpty()) {
        // 密钥认证 - 确保路径正确
        commandParts.add("-i")
        // 在Windows上，路径可能需要特殊处理
        val osName = System.getProperty("os.name").lowercase()
        val formattedPath = if (osName.contains("windows")) {
            // Windows路径处理：将反斜杠转换为正斜杠，并用引号包围
            finalKeyPath.replace("\\", "/").let { if (it.contains(" ")) "\"$it\"" else it }
        } else {
            // Unix-like系统：用引号包围包含空格的路径
            if (finalKeyPath.contains(" ")) "\"$finalKeyPath\"" else finalKeyPath
        }
        commandParts.add(formattedPath)
        println("🔑 使用密钥文件: $formattedPath")
    } else {
        // 没有指定密钥文件，禁用公钥认证，只使用密码认证
        commandParts.add("-o")
        commandParts.add("PubkeyAuthentication=no")
        println("🔐 使用密码认证，请在终端中手动输入密码")
    }

    // 用户和主机
    commandParts.add("${sshConfig.username}@${sshConfig.host}")

    return commandParts.joinToString(" ")
}

/**
 * 检查命令是否可用
 */
private fun isCommandAvailable(command: String): Boolean {
    return try {
        val process = ProcessBuilder("which", command).start()
        process.waitFor() == 0
    } catch (e: Exception) {
        false
    }
}

/**
 * 将密码拷贝到系统剪贴板
 */
private suspend fun copyPasswordToClipboard(password: String) {
    withContext(Dispatchers.IO) {
        try {
            val osName = System.getProperty("os.name").lowercase()
            val process = when {
                osName.contains("mac") -> {
                    // macOS 使用 pbcopy
                    val pb = ProcessBuilder("pbcopy")
                    pb.start().apply {
                        outputStream.writer().use { it.write(password) }
                    }
                }
                osName.contains("linux") -> {
                    // Linux 尝试 xclip，如果失败则尝试 xsel
                    try {
                        val pb = ProcessBuilder("xclip", "-selection", "clipboard")
                        pb.start().apply {
                            outputStream.writer().use { it.write(password) }
                        }
                    } catch (e: Exception) {
                        println("⚠️ xclip 不可用，尝试 xsel")
                        val pb = ProcessBuilder("xsel", "--clipboard", "--input")
                        pb.start().apply {
                            outputStream.writer().use { it.write(password) }
                        }
                    }
                }
                osName.contains("windows") -> {
                    // Windows 使用 clip
                    val pb = ProcessBuilder("cmd.exe", "/c", "echo|$password|clip")
                    pb.start()
                }
                else -> {
                    println("⚠️ 不支持的操作系统，无法自动拷贝密码")
                    return@withContext
                }
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                println("✅ 密码已成功拷贝到剪贴板")
            } else {
                println("⚠️ 拷贝密码到剪贴板失败，退出码: $exitCode")
            }
        } catch (e: Exception) {
            println("⚠️ 拷贝密码到剪贴板时出错: ${e.message}")
        }
    }
}

/**
 * 简单的终端功能测试
 */
private suspend fun testTerminalFunctionality(): Result<String> {
    return withContext(Dispatchers.IO) {
        try {
            val osName = System.getProperty("os.name").lowercase()
            val testCommand = when {
                osName.contains("windows") -> listOf("cmd.exe", "/c", "echo", "Terminal test successful")
                osName.contains("mac") -> listOf("echo", "Terminal test successful")
                osName.contains("linux") -> listOf("echo", "Terminal test successful")
                else -> throw IllegalStateException("不支持的操作系统: $osName")
            }

            println("🧪 测试终端功能: ${testCommand.joinToString(" ")}")
            val process = ProcessBuilder(testCommand).start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success("终端功能正常: $output".trim())
            } else {
                Result.failure(Exception("终端测试失败，退出码: $exitCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
