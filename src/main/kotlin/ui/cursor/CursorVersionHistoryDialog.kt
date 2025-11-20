package ui.cursor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.CursorRuleData
import service.CursorRuleService
import theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 版本历史条目
 */
@Composable
private fun VersionHistoryItem(
    version: Int,
    content: String,
    publishedAt: Long,
    publishedBy: String,
    isCurrentVersion: Boolean,
    onDownload: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isCurrentVersion) AppColors.Primary.copy(alpha = 0.05f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrentVersion) AppColors.Primary.copy(alpha = 0.2f) else AppColors.BackgroundTertiary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.PaddingM)
        ) {
            // 版本头部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isCurrentVersion) AppColors.Primary else AppColors.BackgroundSecondary
                    ) {
                        Text(
                            text = "v$version",
                            style = AppTypography.Caption,
                            color = if (isCurrentVersion) Color.White else AppColors.TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isCurrentVersion) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppColors.Success.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "当前版本",
                                style = AppTypography.Caption,
                                color = AppColors.Success,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "发布者: $publishedBy",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = dateFormatter.format(Date(publishedAt)),
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }

                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "下载此版本",
                        tint = AppColors.Info,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

            // 内容预览
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = AppColors.BackgroundSecondary.copy(alpha = 0.5f)
            ) {
                Text(
                    text = content.take(200) + if (content.length > 200) "..." else "",
                    style = AppTypography.BodySmall,
                    color = AppColors.TextSecondary,
                        modifier = Modifier.padding(AppDimensions.PaddingM),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Cursor规则版本历史对话框
 */
@Composable
fun CursorVersionHistoryDialog(
    rule: CursorRuleData,
    onDismiss: () -> Unit,
    onDownloadVersion: (Int, String) -> Unit
) {
    var showDirectoryPicker by remember { mutableStateOf(false) }
    var versionToDownload by remember { mutableStateOf<Int?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(900.dp)
                .height(700.dp),
            shape = RoundedCornerShape(AppDimensions.RadiusL),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.PaddingL)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "版本历史 - ${rule.name}",
                            style = AppTypography.TitleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "共 ${rule.versions.size} 个版本",
                            style = AppTypography.Caption,
                            color = AppColors.TextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 版本列表
                if (rule.versions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = AppColors.TextDisabled
                            )
                            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                            Text(
                                text = "暂无版本历史",
                                style = AppTypography.BodyLarge,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 按版本号降序排列，最新的在前面
                        val sortedVersions = rule.versions.sortedByDescending { it.version }
                        items(sortedVersions) { versionData ->
                            VersionHistoryItem(
                                version = versionData.version,
                                content = versionData.content,
                                publishedAt = versionData.publishedAt,
                                publishedBy = versionData.publishedBy,
                                isCurrentVersion = versionData.version == rule.currentVersion,
                                onDownload = {
                                    versionToDownload = versionData.version
                                    showDirectoryPicker = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("关闭", style = AppTypography.Caption)
                    }
                }
            }
        }
    }

    // 目录选择对话框
    LaunchedEffect(showDirectoryPicker) {
        if (showDirectoryPicker && versionToDownload != null) {
            try {
                val fileChooser = javax.swing.JFileChooser().apply {
                    fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "选择保存目录"
                    approveButtonText = "选择"
                    selectedFile = java.io.File(System.getProperty("user.home"), "Downloads")
                }

                val result = fileChooser.showSaveDialog(null)
                if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                    val selectedDir = fileChooser.selectedFile
                    if (selectedDir != null) {
                        onDownloadVersion(versionToDownload!!, selectedDir.absolutePath)
                    }
                }
            } catch (e: Exception) {
                // 这里没有状态消息回调，所以只打印错误
                println("选择目录失败: ${e.message}")
            } finally {
                showDirectoryPicker = false
                versionToDownload = null
            }
        }
    }
}
