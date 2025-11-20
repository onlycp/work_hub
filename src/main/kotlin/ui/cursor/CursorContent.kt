package ui.cursor

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.BaseData
import data.CursorPlatform
import data.CursorRuleData
import data.CursorRuleManager
import data.PermissionManager
import kotlinx.coroutines.launch
import service.CursorRuleService
import theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cursor规则表格行
 */
@Composable
private fun CursorRuleTableRow(
    rule: CursorRuleData,
    dateFormatter: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onPublish: () -> Unit,
    onDownload: () -> Unit,
    onViewVersions: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 名称
            Text(
                text = rule.name,
                modifier = Modifier.weight(2f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 适用平台
            Text(
                text = CursorRuleService.getPlatformDisplayNames(rule.platforms),
                modifier = Modifier.weight(1.5f),
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 版本号
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = AppColors.Primary.copy(alpha = 0.1f),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Text(
                    text = "v${rule.currentVersion}",
                    style = AppTypography.Caption,
                    color = AppColors.Primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // 共享状态
            Row(
                modifier = Modifier.width(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (rule.isShared) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.Success.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "共享",
                            style = AppTypography.Caption,
                            color = AppColors.Success,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.TextDisabled.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "私有",
                            style = AppTypography.Caption,
                            color = AppColors.TextDisabled,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 发布时间
            Text(
                text = dateFormatter.format(Date(rule.getVersionPublishTime(rule.currentVersion))),
                modifier = Modifier.weight(1.5f),
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 操作按钮
            Row(
                modifier = Modifier.width(200.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 编辑按钮 - 只有创建者才能显示
                if (PermissionManager.canEdit(with(rule) {
                    object : data.BaseData {
                        override val id: String = this@with.id
                        override val createdBy: String = this@with.createdBy
                        override val createdAt: Long = this@with.createdAt
                        override val lastModified: Long = this@with.lastModified
                    }
                })) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 发布按钮 - 只有创建者才能显示
                if (PermissionManager.canEdit(with(rule) {
                    object : data.BaseData {
                        override val id: String = this@with.id
                        override val createdBy: String = this@with.createdBy
                        override val createdAt: Long = this@with.createdAt
                        override val lastModified: Long = this@with.lastModified
                    }
                })) {
                    IconButton(
                        onClick = onPublish,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Publish,
                            contentDescription = "发布",
                            tint = AppColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 下载按钮 - 所有人可见
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "下载",
                        tint = AppColors.Info,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 版本历史按钮 - 所有人可见
                IconButton(
                    onClick = onViewVersions,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "版本历史",
                        tint = AppColors.Warning,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 删除按钮 - 只有创建者才能显示
                if (PermissionManager.canDelete(with(rule) {
                    object : data.BaseData {
                        override val id: String = this@with.id
                        override val createdBy: String = this@with.createdBy
                        override val createdAt: Long = this@with.createdAt
                        override val lastModified: Long = this@with.lastModified
                    }
                })) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = AppColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

/**
 * Cursor规则管理主界面
 */
@Composable
fun CursorContent(
    onStatusMessage: (String) -> Unit = {},
    currentUserId: String = ""
) {
    val scope = rememberCoroutineScope()
    val rules by CursorRuleManager.rules.collectAsState()
    var showRuleDialog by remember { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CursorRuleData?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<CursorRuleData?>(null) }
    var showVersionHistoryDialog by remember { mutableStateOf<CursorRuleData?>(null) }
    var showDirectoryPicker by remember { mutableStateOf(false) }
    var ruleToDownload by remember { mutableStateOf<CursorRuleData?>(null) }

    // 日期格式化器
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimensions.PaddingScreen)
    ) {
        // 页面标题
        Text(
            text = "Cursor规则管理",
            style = AppTypography.TitleLarge,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = AppDimensions.SpaceL)
        )

        // 操作栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "规则列表 (${rules.size})",
                style = AppTypography.BodyLarge,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = {
                    editingRule = null
                    showRuleDialog = true
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加规则",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加规则", style = AppTypography.Caption)
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 规则表格
        Card(
            modifier = Modifier.weight(1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(AppDimensions.RadiusL)
        ) {
            if (rules.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimensions.PaddingL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = AppColors.TextDisabled
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                        Text(
                            text = "暂无Cursor规则",
                            style = AppTypography.BodyLarge,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                        Text(
                            text = "添加Cursor规则以管理项目规范",
                            style = AppTypography.Caption,
                            color = AppColors.TextDisabled
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 表头
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.BackgroundSecondary,
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "名称",
                                modifier = Modifier.weight(2f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "适用平台",
                                modifier = Modifier.weight(1.5f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "版本",
                                modifier = Modifier.width(60.dp),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "共享状态",
                                modifier = Modifier.width(60.dp),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "发布时间",
                                modifier = Modifier.weight(1.5f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "操作",
                                modifier = Modifier.width(200.dp),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // 表格内容
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(rules) { rule ->
                            CursorRuleTableRow(
                                rule = rule,
                                dateFormatter = dateFormatter,
                                onEdit = {
                                    editingRule = rule
                                    showRuleDialog = true
                                },
                                onPublish = {
                                    scope.launch {
                                        val result = CursorRuleService.publishNewVersion(
                                            ruleId = rule.id,
                                            content = rule.getCurrentContent(),
                                            publishedBy = "当前用户"
                                        )
                                        if (result.isSuccess) {
                                            onStatusMessage("✅ 规则发布成功，版本已更新到 v${result.getOrNull()?.currentVersion}")
                                        } else {
                                            onStatusMessage("❌ 规则发布失败: ${result.exceptionOrNull()?.message}")
                                        }
                                    }
                                },
                                onDownload = {
                                    ruleToDownload = rule
                                    showDirectoryPicker = true
                                },
                                onViewVersions = {
                                    showVersionHistoryDialog = rule
                                },
                                onDelete = {
                                    showDeleteConfirmDialog = rule
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 规则对话框
    if (showRuleDialog) {
        CursorRuleDialog(
            rule = editingRule,
            onDismiss = {
                showRuleDialog = false
                editingRule = null
            },
            onConfirm = { rule ->
                scope.launch {
                    val result = if (editingRule != null) {
                        // 编辑时更新整个规则对象（包含名称、平台、内容等）
                        val updatedRule = rule.copy(lastModified = System.currentTimeMillis())
                        CursorRuleManager.updateRule(updatedRule)
                    } else {
                        CursorRuleService.createRule(rule.name, rule.platforms, rule.getCurrentContent(), currentUserId)
                    }

                    if (result.isSuccess) {
                        onStatusMessage("✅ 规则${if (editingRule != null) "更新" else "创建"}成功")
                        showRuleDialog = false
                        editingRule = null
                    } else {
                        onStatusMessage("❌ 规则操作失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
        )
    }

    // 版本历史对话框
    showVersionHistoryDialog?.let { rule ->
        CursorVersionHistoryDialog(
            rule = rule,
            onDismiss = { showVersionHistoryDialog = null },
            onDownloadVersion = { version, savePath ->
                scope.launch {
                    val result = CursorRuleService.downloadRuleFile(rule, savePath, version)
                    if (result.isSuccess) {
                        onStatusMessage("✅ ${result.getOrNull()}")
                    } else {
                        onStatusMessage("❌ 下载失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
        )
    }

    // 删除确认对话框
    showDeleteConfirmDialog?.let { rule ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = "确认删除",
                    style = AppTypography.BodyLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定要删除Cursor规则 \"${rule.name}\" 吗？此操作不可撤销。",
                    style = AppTypography.BodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = CursorRuleService.deleteRule(rule.id)
                            if (result.isSuccess) {
                                onStatusMessage("✅ 规则删除成功")
                            } else {
                                onStatusMessage("❌ 规则删除失败: ${result.exceptionOrNull()?.message}")
                            }
                            showDeleteConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppColors.Error,
                        contentColor = Color.White
                    )
                ) {
                    Text("删除", style = AppTypography.Caption)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("取消", style = AppTypography.Caption)
                }
            }
        )
    }

    // 目录选择对话框
    LaunchedEffect(showDirectoryPicker) {
        if (showDirectoryPicker && ruleToDownload != null) {
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
                        val downloadResult = CursorRuleService.downloadRuleFile(
                            ruleToDownload!!,
                            selectedDir.absolutePath
                        )
                        if (downloadResult.isSuccess) {
                            onStatusMessage("✅ ${downloadResult.getOrNull()}")
                        } else {
                            onStatusMessage("❌ 下载失败: ${downloadResult.exceptionOrNull()?.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                onStatusMessage("❌ 选择目录失败: ${e.message}")
            } finally {
                showDirectoryPicker = false
                ruleToDownload = null
            }
        }
    }
}
