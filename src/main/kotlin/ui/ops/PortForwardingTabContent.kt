package ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.PortForwardingRuleData
import data.PortManager
import data.PermissionManager
import data.SSHConfigManager
import kotlinx.coroutines.launch
import theme.*
import ui.ops.PortDialog
import java.util.UUID

/**
 * 端口转发标签内容
 */
@Composable
fun PortForwardingTabContent(
    showPortDialog: Boolean,
    editingPortRule: PortForwardingRuleData?,
    onShowPortDialog: () -> Unit,
    onHidePortDialog: () -> Unit,
    onEditingPortRule: (PortForwardingRuleData?) -> Unit
) {
    val scope = rememberCoroutineScope()
    val portRules by PortManager.portRules.collectAsState()

    // 获取当前SSH配置，用于权限检查
    val currentConfigId = PortManager.getCurrentConfigId()
    val currentConfig = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
    val canEditPorts = PermissionManager.shouldShowAddPortButton(currentConfig)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimensions.PaddingM)
    ) {
        // 标题和操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "端口转发 (${portRules.size})",
                fontSize = AppTypography.BodyLarge.fontSize,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )

            if (canEditPorts) {
                IconButton(
                    onClick = {
                        onEditingPortRule(null)
                        onShowPortDialog()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加端口规则",
                        modifier = Modifier.size(20.dp),
                        tint = AppColors.Primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 端口规则列表
        if (portRules.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.PaddingXL),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowForward,
                        contentDescription = "无端口规则",
                        modifier = Modifier.size(48.dp),
                        tint = AppColors.TextDisabled
                    )

                    Text(
                        text = "暂无端口转发规则",
                        fontSize = AppTypography.BodyLarge.fontSize,
                        color = AppColors.TextSecondary
                    )

                    Text(
                        text = "点击上方按钮添加新的端口转发规则",
                        fontSize = AppTypography.BodySmall.fontSize,
                        color = AppColors.TextDisabled
                    )
                }
            }
        } else {
            // 表格头部
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Surface,
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "类型",
                        fontSize = AppTypography.BodySmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = "本地端口",
                        fontSize = AppTypography.BodySmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = "远程地址",
                        fontSize = AppTypography.BodySmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "说明",
                        fontSize = AppTypography.BodySmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }

            Divider(color = AppColors.Divider)

            // 表格列表
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(portRules) { rule ->
                    PortTableRow(
                        rule = rule,
                        canEdit = PermissionManager.shouldShowEditPortButton(currentConfig),
                        canDelete = PermissionManager.shouldShowDeletePortButton(currentConfig),
                        onEdit = {
                            onEditingPortRule(rule)
                            onShowPortDialog()
                        },
                        onDelete = {
                            scope.launch {
                                val result = PortManager.deletePortRule(rule.id)
                                if (result.isFailure) {
                                    println("删除端口规则失败: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    )
                    Divider(color = AppColors.Divider)
                }
            }
        }
    }

    // 端口对话框
    if (showPortDialog) {
        ui.ops.PortDialog(
            initialRule = editingPortRule,
            onDismiss = onHidePortDialog,
            onSave = { rule ->
                scope.launch {
                    val currentEditingRule = editingPortRule
                    val result = if (currentEditingRule != null) {
                        // 编辑模式：使用原有ID更新规则
                        val updatedRule = rule.copy(id = currentEditingRule.id)
                        PortManager.updatePortRule(currentEditingRule.id, updatedRule)
                    } else {
                        // 新增模式：生成新ID
                        val newRule = rule.copy(id = UUID.randomUUID().toString())
                        PortManager.addPortRule(newRule)
                    }

                    if (result.isSuccess) {
                        onHidePortDialog()
                        onEditingPortRule(null)
                    } else {
                        // TODO: 显示错误提示
                        println("保存端口规则失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
        )
    }
}

/**
 * 端口表格行
 */
@Composable
private fun PortTableRow(
    rule: PortForwardingRuleData,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Background)
            .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceM),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 类型
        Text(
            text = rule.type,
            fontSize = AppTypography.BodySmall.fontSize,
            color = AppColors.Primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )

        // 本地端口
        Text(
            text = rule.localPort.toString(),
            fontSize = AppTypography.BodyMedium.fontSize,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary,
            modifier = Modifier.width(80.dp)
        )

        // 远程地址
        Text(
            text = "${rule.remoteHost}:${rule.remotePort}",
            fontSize = AppTypography.BodySmall.fontSize,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 说明
        Text(
            text = rule.description,
            fontSize = AppTypography.BodySmall.fontSize,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 操作按钮
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 编辑按钮
            if (canEdit) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.TextSecondary
                    )
                }
            }

            // 删除按钮
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.Error
                    )
                }
            }
        }
    }
}
