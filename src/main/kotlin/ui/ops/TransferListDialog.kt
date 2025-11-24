package ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.TransferManager
import data.TransferStatus
import data.TransferTask
import data.TransferType
import theme.*
import java.io.File

/**
 * 传输列表弹窗
 * 显示所有文件传输任务
 */
@Composable
fun TransferListDialog(
    onDismiss: () -> Unit
) {
    val tasksState = TransferManager.tasks.collectAsState()
    // 使用 derivedStateOf 创建列表快照以避免并发修改异常
    val tasks = remember {
        derivedStateOf {
            tasksState.value.toList()
        }
    }.value

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.CornerMedium),
            color = AppColors.Surface,
            elevation = AppDimensions.ElevationDialog,
            modifier = Modifier
                .width(480.dp)
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 标题栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.SurfaceVariant,
                    elevation = AppDimensions.ElevationXS
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "传输列表",
                            style = AppTypography.TitleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // 清除已完成按钮
                            if (tasks.any { it.status == TransferStatus.COMPLETED || it.status == TransferStatus.FAILED }) {
                                TextButton(
                                    onClick = { TransferManager.clearCompleted() },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "清除",
                                        style = AppTypography.Caption,
                                        color = AppColors.TextSecondary
                                    )
                                }
                            }

                            // 关闭按钮
                            IconButton(
                                onClick = onDismiss,
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

                Divider(color = AppColors.Divider)

                // 传输列表
                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(AppDimensions.SpaceXL),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "无传输任务",
                                tint = AppColors.TextDisabled,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                            Text(
                                text = "暂无传输任务",
                                style = AppTypography.BodyMedium,
                                color = AppColors.TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = AppDimensions.SpaceS)
                    ) {
                        items(tasks) { task ->
                            TransferTaskItem(
                                task = task,
                                onOpenFile = {
                                    try {
                                        val file = File(task.localPath ?: task.filePath)
                                        if (file.exists()) {
                                            java.awt.Desktop.getDesktop().open(file.parentFile)
                                        }
                                    } catch (e: Exception) {
                                        println("打开文件失败: ${e.message}")
                                    }
                                },
                                onRemove = {
                                    TransferManager.removeTask(task.id)
                                }
                            )
                            if (task != tasks.last()) {
                                Divider(
                                    color = AppColors.Divider.copy(alpha = 0.3f),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = AppDimensions.SpaceM)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 传输任务项
 */
@Composable
fun TransferTaskItem(
    task: TransferTask,
    onOpenFile: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS)
            .clickable { onOpenFile() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 文件图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(AppDimensions.CornerXS))
                .background(
                    when (task.type) {
                        TransferType.UPLOAD -> AppColors.Primary.copy(alpha = 0.1f)
                        TransferType.DOWNLOAD -> AppColors.Success.copy(alpha = 0.1f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (task.type) {
                    TransferType.UPLOAD -> Icons.Default.Upload
                    TransferType.DOWNLOAD -> Icons.Default.Download
                },
                contentDescription = null,
                tint = when (task.type) {
                    TransferType.UPLOAD -> AppColors.Primary
                    TransferType.DOWNLOAD -> AppColors.Success
                },
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(AppDimensions.SpaceM))

        // 文件信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = task.fileName,
                style = AppTypography.BodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
            ) {
                // 文件大小
                Text(
                    text = task.formatSize(task.totalSize),
                    style = AppTypography.Caption,
                    color = AppColors.TextSecondary
                )

                // 状态文本
                Text(
                    text = when (task.status) {
                        TransferStatus.PENDING -> "等待中"
                        TransferStatus.RUNNING -> {
                            if (task.totalSize > 0) {
                                "${task.progress.toInt()}% - ${task.formatSpeed()}"
                            } else {
                                "传输中..."
                            }
                        }
                        TransferStatus.COMPLETED -> "已完成"
                        TransferStatus.FAILED -> "失败: ${task.errorMessage ?: "未知错误"}"
                        TransferStatus.CANCELLED -> "已取消"
                    },
                    style = AppTypography.Caption,
                    color = when (task.status) {
                        TransferStatus.COMPLETED -> AppColors.Success
                        TransferStatus.FAILED -> AppColors.Error
                        TransferStatus.CANCELLED -> AppColors.TextDisabled
                        else -> AppColors.TextSecondary
                    }
                )
            }

            // 进度条（仅传输中显示）
            if (task.status == TransferStatus.RUNNING && task.totalSize > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = task.progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = AppColors.Primary,
                    backgroundColor = AppColors.Divider
                )
            }
        }

        Spacer(modifier = Modifier.width(AppDimensions.SpaceS))

        // 操作按钮
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // 打开文件位置按钮（仅已完成的任务）
            if (task.status == TransferStatus.COMPLETED && task.localPath != null) {
                IconButton(
                    onClick = onOpenFile,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "打开文件位置",
                        tint = AppColors.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 删除按钮
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "删除",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

