package ui.ops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import data.CommandRuleData
import data.SSHConfigManager
import kotlinx.coroutines.launch
import service.SSHSessionManager
import theme.AppColors
import theme.AppDimensions
import theme.AppTypography

/**
 * 命令执行对话框
 */
@Composable
fun CommandExecutionDialog(
    commandRule: CommandRuleData,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // 输出内容状态
    var commandOutput by remember { mutableStateOf("") }
    var logOutput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }
    var isMonitoringLog by remember { mutableStateOf(false) }
    var executionError by remember { mutableStateOf<String?>(null) }
    var logError by remember { mutableStateOf<String?>(null) }

    // 直接执行，不需要确认对话框
    var isCommandStarted by remember { mutableStateOf(false) }

    // 拷贝功能
    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
    }

    // 调试输出
    println("CommandExecutionDialog: 初始化，command=${commandRule.name}")

    // 执行命令
    fun executeCommand() {
        scope.launch {
            try {
                isExecuting = true
                commandOutput = ""
                executionError = null
                isCommandStarted = true

                val configId = data.CommandManager.getCurrentConfigId()
                    ?: throw Exception("未选择SSH配置")
                val config = SSHConfigManager.getConfigById(configId)
                    ?: throw Exception("未找到SSH配置")

                val sshClient = SSHSessionManager.getSession(config.name)
                    ?: throw Exception("SSH连接不存在")

                // 检查脚本是否包含换行符，如果有则使用多行执行
                val hasMultipleLines = commandRule.script.contains('\n') &&
                    commandRule.script.lines().any { it.trim().isNotEmpty() }

                val result = if (hasMultipleLines) {
                    // 多行命令：按行顺序执行
                    sshClient.executeMultiLineCommandStream(
                        script = commandRule.script,
                        workingDirectory = commandRule.workingDirectory,
                        onOutput = { line ->
                            commandOutput += line
                        },
                        onError = { line ->
                            commandOutput += line
                            executionError = "执行出错"
                        },
                        onComplete = {
                            isExecuting = false
                        }
                    )
                } else {
                    // 单行命令：使用原有的执行方式
                    val finalCommand = if (commandRule.workingDirectory.isNotBlank()) {
                        "cd \"${commandRule.workingDirectory}\" && ${commandRule.script}"
                    } else {
                        commandRule.script
                    }

                    sshClient.executeCommandStream(
                        command = finalCommand,
                        onOutput = { line ->
                            commandOutput += line
                        },
                        onError = { line ->
                            commandOutput += line
                            executionError = "执行出错"
                        },
                        onComplete = {
                            isExecuting = false
                        }
                    )
                }

                if (result.isFailure) {
                    executionError = result.exceptionOrNull()?.message ?: "执行失败"
                    isExecuting = false
                }
            } catch (e: Exception) {
                executionError = e.message ?: "执行异常"
                isExecuting = false
            }
        }
    }

    // 监控日志
    fun startLogMonitoring() {
        if (commandRule.logFile.isNotBlank()) {
            scope.launch {
                try {
                    isMonitoringLog = true
                    logOutput = ""
                    logError = null

                    val configId = data.CommandManager.getCurrentConfigId()
                        ?: throw Exception("未选择SSH配置")
                    val config = SSHConfigManager.getConfigById(configId)
                        ?: throw Exception("未找到SSH配置")

                    val sshClient = SSHSessionManager.getSession(config.name)
                        ?: throw Exception("SSH连接不存在")

                    val result = sshClient.tailLogFile(
                        logFile = commandRule.logFile,
                        onOutput = { line ->
                            logOutput += line
                        },
                        onError = { error ->
                            logError = error.trim()
                        }
                    )

                    if (result.isFailure) {
                        logError = result.exceptionOrNull()?.message ?: "日志监控失败"
                        isMonitoringLog = false
                    }
                } catch (e: Exception) {
                    logError = e.message ?: "日志监控异常"
                    isMonitoringLog = false
                }
            }
        }
    }

    // 主执行对话框 - 直接显示，无需确认
    Dialog(
        onDismissRequest = {
            // 关闭时停止所有流
            scope.launch {
                val configId = data.CommandManager.getCurrentConfigId()
                val config = configId?.let { SSHConfigManager.getConfigById(it) }
                val sshClient = config?.let { SSHSessionManager.getSession(it.name) }
                sshClient?.stopCurrentStreams()
            }
            onDismiss()
        }
    ) {
            Surface(
                modifier = Modifier
                    .size(800.dp, 800.dp),
                shape = RoundedCornerShape(AppDimensions.RadiusL),
                color = AppColors.BackgroundPrimary,
                elevation = AppDimensions.ElevationDialog
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 对话框标题栏
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.Surface,
                        elevation = AppDimensions.ElevationXS
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "执行命令: ${commandRule.name}",
                                    style = AppTypography.TitleMedium,
                                    color = AppColors.TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = commandRule.script,
                                    style = AppTypography.Caption,
                                    color = AppColors.TextSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                            ) {
                                if (!isExecuting) {
                                    Button(
                                        onClick = {
                                            executeCommand()
                                            // 只有在没有启动日志监控时才启动日志监控
                                            if (!isMonitoringLog && commandRule.logFile.isNotBlank()) {
                                                startLogMonitoring()
                                            }
                                        },
                                        enabled = !isExecuting,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = if (isCommandStarted) "重新执行" else "开始执行",
                                            style = AppTypography.Caption
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        // 关闭时停止所有流
                                        scope.launch {
                                            val configId = data.CommandManager.getCurrentConfigId()
                                            val config = configId?.let { SSHConfigManager.getConfigById(it) }
                                            val sshClient = config?.let { SSHSessionManager.getSession(it.name) }
                                            sshClient?.stopCurrentStreams()
                                        }
                                        onDismiss()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "关闭",
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 内容区域 - Tab布局
                    val tabTitles = listOf("命令输出", "日志监控")
                    var selectedTabIndex by remember { mutableStateOf(0) }

                    Column(modifier = Modifier.weight(1f)) {
                        // Tab栏
                        ScrollableTabRow(
                            selectedTabIndex = selectedTabIndex,
                            backgroundColor = AppColors.BackgroundSecondary,
                            contentColor = AppColors.Primary,
                            edgePadding = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            tabTitles.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = { selectedTabIndex = index },
                                    text = {
                                        Text(
                                            text = title,
                                            style = AppTypography.BodySmall,
                                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        // Tab内容
                        when (selectedTabIndex) {
                            0 -> {
                                // 命令输出Tab
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(AppDimensions.SpaceM)
                                ) {
                                    // 状态栏和操作按钮
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = AppColors.BackgroundSecondary,
                                        shape = RoundedCornerShape(AppDimensions.CornerSmall)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(AppDimensions.SpaceS),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "执行状态",
                                                style = AppTypography.BodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = AppColors.TextPrimary
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                                            ) {
                                                // 拷贝按钮
                                                if (commandOutput.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = { copyToClipboard(commandOutput) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "拷贝命令输出",
                                                            tint = AppColors.TextSecondary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                if (isExecuting) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                                                    Text(
                                                        text = "执行中...",
                                                        style = AppTypography.Caption,
                                                        color = AppColors.TextSecondary
                                                    )
                                                } else if (executionError != null) {
                                                    Text(
                                                        text = "执行失败",
                                                        style = AppTypography.Caption,
                                                        color = AppColors.Error
                                                    )
                                                } else if (isCommandStarted) {
                                                    Text(
                                                        text = "执行完成",
                                                        style = AppTypography.Caption,
                                                        color = AppColors.Success
                                                    )
                                                } else {
                                                    Text(
                                                        text = "等待执行",
                                                        style = AppTypography.Caption,
                                                        color = AppColors.TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 命令输出内容
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(top = AppDimensions.SpaceS),
                                        color = AppColors.TerminalBackground,
                                        shape = RoundedCornerShape(AppDimensions.CornerSmall)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .padding(AppDimensions.SpaceS)
                                            ) {
                                                val scrollState = rememberScrollState()
                                                Text(
                                                    text = when {
                                                        !isCommandStarted -> "请点击上方\"开始执行\"按钮"
                                                        commandOutput.isEmpty() -> "等待输出..."
                                                        else -> commandOutput
                                                    },
                                                    style = AppTypography.Caption,
                                                    color = if (executionError != null) AppColors.Error else AppColors.TerminalText,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .verticalScroll(scrollState)
                                                )

                                                // 自动滚动到底部 - 实时追踪最新输出
                                                LaunchedEffect(commandOutput) {
                                                    if (commandOutput.isNotEmpty()) {
                                                        scrollState.animateScrollTo(scrollState.maxValue)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // 日志监控Tab
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(AppDimensions.SpaceM)
                                ) {
                                    // 日志文件路径和操作按钮
                                    if (commandRule.logFile.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = AppColors.BackgroundSecondary,
                                            shape = RoundedCornerShape(AppDimensions.CornerSmall)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(AppDimensions.SpaceS),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "监控文件:",
                                                    style = AppTypography.BodySmall,
                                                    color = AppColors.TextSecondary
                                                )
                                                Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                                                Text(
                                                    text = commandRule.logFile,
                                                    style = AppTypography.BodySmall,
                                                    color = AppColors.TextPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                                                ) {
                                                    // 拷贝按钮
                                                    if (logOutput.isNotEmpty()) {
                                                        IconButton(
                                                            onClick = { copyToClipboard(logOutput) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = "拷贝日志内容",
                                                                tint = AppColors.TextSecondary,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                    if (isMonitoringLog) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                                                        Text(
                                                            text = "监控中...",
                                                            style = AppTypography.Caption,
                                                            color = AppColors.TextSecondary
                                                        )
                                                    } else if (logError != null) {
                                                        Text(
                                                            text = "监控失败",
                                                            style = AppTypography.Caption,
                                                            color = AppColors.Error
                                                        )
                                                    } else if (commandRule.logFile.isNotBlank()) {
                                                        Text(
                                                            text = "未启动",
                                                            style = AppTypography.Caption,
                                                            color = AppColors.TextSecondary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 日志输出内容
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                            .padding(top = AppDimensions.SpaceS),
                                        color = AppColors.TerminalBackground,
                                        shape = RoundedCornerShape(AppDimensions.CornerSmall)
                                    ) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .padding(AppDimensions.SpaceS)
                                            ) {
                                                val scrollState = rememberScrollState()
                                                val displayText = when {
                                                    commandRule.logFile.isBlank() -> "未配置日志文件，请在命令规则中设置日志文件路径"
                                                    logOutput.isEmpty() && !isMonitoringLog -> "等待日志输出..."
                                                    else -> logOutput
                                                }

                                                Text(
                                                    text = displayText,
                                                    style = AppTypography.Caption,
                                                    color = if (logError != null) AppColors.Error else AppColors.TerminalText,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .verticalScroll(scrollState)
                                                )

                                                // 自动滚动到底部 - 实时追踪最新日志
                                                LaunchedEffect(logOutput) {
                                                    if (logOutput.isNotEmpty()) {
                                                        scrollState.animateScrollTo(scrollState.maxValue)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}
