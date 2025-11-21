package ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import theme.*
import utils.Logger
import java.io.File

/**
 * 日志内容界面
 */
@Composable
fun LogsContent() {
    val scope = rememberCoroutineScope()
    var logContent by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isCleaning by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // 加载日志内容 - 只读取最近的日志以避免内存占用
    fun loadLogs() {
        isLoading = true
        scope.launch {
            try {
                val logFile = File(Logger.getLogFilePath())
                if (logFile.exists()) {
                    // 只读取文件末尾的最近内容，避免加载整个大文件
                    val maxBytes = 1024 * 1024 // 1MB 限制
                    val fileSize = logFile.length()

                    if (fileSize <= maxBytes) {
                        // 文件不大，直接读取全部
                        logContent = logFile.readText(Charsets.UTF_8)
                    } else {
                        // 文件很大，只读取末尾部分
                        val buffer = ByteArray(maxBytes.toInt())
                        logFile.inputStream().use { input ->
                            input.skip(fileSize - maxBytes)
                            val bytesRead = input.read(buffer)
                            val partialContent = String(buffer, 0, bytesRead, Charsets.UTF_8)

                            // 尝试找到第一个完整的日志行
                            val lines = partialContent.lines()
                            val completeLines = if (lines.isNotEmpty() && !partialContent.startsWith(lines.first())) {
                                // 如果第一行不完整，跳过它
                                lines.drop(1)
                            } else {
                                lines
                            }

                            logContent = "[显示最近 ${"%.1f".format(maxBytes / 1024.0 / 1024.0)}MB 日志内容]\n\n" +
                                       completeLines.joinToString("\n")
                        }
                    }

                    // 在开头添加文件信息
                    val fileSizeMB = "%.2f".format(logFile.length() / 1024.0 / 1024.0)
                    logContent = "日志文件: ${Logger.getLogFilePath()}\n" +
                               "文件大小: ${fileSizeMB} MB\n" +
                               "最后修改: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date(logFile.lastModified()))}\n" +
                               "=".repeat(80) + "\n\n" + logContent
                } else {
                    logContent = "日志文件不存在: ${Logger.getLogFilePath()}"
                }
            } catch (e: Exception) {
                logContent = "读取日志失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // 清理日志文件
    fun cleanLogs() {
        isCleaning = true
        scope.launch {
            try {
                val logFile = File(Logger.getLogFilePath())
                if (logFile.exists()) {
                    val originalSize = logFile.length()
                    // 保留最近的日志内容（比如保留最后100KB）
                    val keepBytes = 100 * 1024 // 100KB
                    if (logFile.length() > keepBytes) {
                        val buffer = ByteArray(keepBytes.toInt())
                        logFile.inputStream().use { input ->
                            input.skip(logFile.length() - keepBytes)
                            input.read(buffer)
                        }

                        // 重新写入文件，只保留最近的内容
                        logFile.writeText(String(buffer, Charsets.UTF_8))
                        val newSize = logFile.length()

                        // 记录清理操作到日志
                        utils.Logger.info("日志文件已清理: ${"%.2f".format(originalSize / 1024.0 / 1024.0)}MB -> ${"%.2f".format(newSize / 1024.0 / 1024.0)}MB")
                    } else {
                        utils.Logger.info("日志文件无需清理，当前大小: ${"%.2f".format(originalSize / 1024.0 / 1024.0)}MB")
                    }
                }
                // 重新加载日志
                loadLogs()
            } catch (e: Exception) {
                logContent = "清理日志失败: ${e.message}"
                utils.Logger.error("清理日志失败: ${e.message}", e)
            } finally {
                isCleaning = false
            }
        }
    }

    // 初始化时加载日志
    LaunchedEffect(Unit) {
        loadLogs()
    }

    // 当日志内容改变时，自动滚动到最后面
    LaunchedEffect(logContent) {
        if (logContent.isNotEmpty()) {
            scope.launch {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.PaddingScreen, vertical = AppDimensions.SpaceM),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "应用日志",
                style = AppTypography.TitleMedium,
                color = AppColors.TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Primary
                    )
                    Text("加载中...", style = AppTypography.Caption, color = AppColors.TextSecondary)
                } else if (isCleaning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Warning
                    )
                    Text("清理中...", style = AppTypography.Caption, color = AppColors.TextSecondary)
                }

                IconButton(
                    onClick = { cleanLogs() },
                    enabled = !isLoading && !isCleaning
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "清理日志",
                        tint = if (isCleaning) AppColors.Warning else AppColors.TextSecondary
                    )
                }

                IconButton(
                    onClick = { loadLogs() },
                    enabled = !isLoading && !isCleaning
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新日志",
                        tint = if (isLoading) AppColors.TextDisabled else AppColors.TextSecondary
                    )
                }
            }
        }

        // 日志内容区域
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimensions.PaddingScreen, vertical = AppDimensions.SpaceS),
            backgroundColor = AppColors.BackgroundSecondary,
            elevation = AppDimensions.ElevationXS
        ) {
            if (logContent.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无日志内容",
                        style = AppTypography.BodyMedium,
                        color = AppColors.TextSecondary
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    SelectionContainer {
                        Text(
                            text = logContent,
                            style = AppTypography.BodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = AppColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(AppDimensions.SpaceM)
                                .verticalScroll(scrollState)
                        )
                    }

                    // 垂直滚动条
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = AppDimensions.SpaceS)
                    )
                }
            }
        }
    }
}
