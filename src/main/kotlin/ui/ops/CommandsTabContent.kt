package ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.CommandRuleData
import data.CommandManager
import data.PermissionManager
import data.SSHConfigManager
import kotlinx.coroutines.launch
import theme.*
import java.util.UUID

/**
 * 命令标签内容
 */
@Composable
fun CommandsTabContent(
    showCommandDialog: Boolean,
    editingCommandRule: CommandRuleData?,
    onShowCommandDialog: () -> Unit,
    onHideCommandDialog: () -> Unit,
    onEditingCommandRule: (CommandRuleData?) -> Unit,
    onExecutingCommandRule: (CommandRuleData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val commandRules by CommandManager.commandRules.collectAsState()

    // 获取当前SSH配置，用于权限检查
    val currentConfigId = CommandManager.getCurrentConfigId()
    val currentConfig = currentConfigId?.let { SSHConfigManager.getConfigById(it) }
    val canEditCommands = PermissionManager.shouldShowAddCommandButton(currentConfig)

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
                text = "命令管理 (${commandRules.size})",
                fontSize = AppTypography.BodyLarge.fontSize,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )

            if (canEditCommands) {
                IconButton(
                    onClick = {
                        onEditingCommandRule(null)
                        onShowCommandDialog()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加命令",
                        modifier = Modifier.size(20.dp),
                        tint = AppColors.Primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 命令列表
        if (commandRules.isEmpty()) {
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
                        imageVector = Icons.Default.Build,
                        contentDescription = "无命令",
                        modifier = Modifier.size(48.dp),
                        tint = AppColors.TextDisabled
                    )

                    Text(
                        text = "暂无命令规则",
                        fontSize = AppTypography.BodyLarge.fontSize,
                        color = AppColors.TextSecondary
                    )

                    Text(
                        text = "点击上方按钮添加新的命令规则",
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
                        text = "名称",
                        fontSize = AppTypography.BodySmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(0.2f)
                    )
                    Text(
                        text = "命令",
                        fontSize = AppTypography.BodySmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(0.3f)
                    )
                    Text(
                        text = "工作目录",
                        fontSize = AppTypography.BodySmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(0.25f)
                    )
                    Text(
                        text = "备注",
                        fontSize = AppTypography.BodySmall.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.weight(0.25f)
                    )
                    Spacer(modifier = Modifier.width(80.dp))
                }
            }

            Divider(color = AppColors.Divider)

            // 表格列表
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(commandRules) { rule ->
                    CommandTableRow(
                        commandRule = rule,
                        canEdit = PermissionManager.shouldShowEditCommandButton(currentConfig),
                        canDelete = PermissionManager.shouldShowDeleteCommandButton(currentConfig),
                        onEdit = {
                            println("点击编辑命令按钮: ${rule.name}")
                            onEditingCommandRule(rule)
                            onShowCommandDialog()
                        },
                        onDelete = {
                            scope.launch {
                                val result = CommandManager.deleteCommandRule(rule.id)
                                if (result.isFailure) {
                                    println("删除命令规则失败: ${result.exceptionOrNull()?.message}")
                                }
                            }
                        }
                    )
                    Divider(color = AppColors.Divider)
                }
            }
        }
    }

    // 命令对话框
    if (showCommandDialog) {
        ui.ops.CommandDialog(
            initialRule = editingCommandRule,
            onDismiss = onHideCommandDialog,
            onSave = { rule ->
                scope.launch {
                    val currentEditingRule = editingCommandRule
                    val result = if (currentEditingRule != null) {
                        // 编辑模式：使用原有ID更新规则
                        val updatedRule = rule.copy(id = currentEditingRule.id)
                        CommandManager.updateCommandRule(currentEditingRule.id, updatedRule)
                    } else {
                        // 新增模式：生成新ID
                        val newId = UUID.randomUUID().toString()
                        val newRule = rule.copy(id = newId)
                        CommandManager.addCommandRule(newRule)
                    }

                    if (result.isSuccess) {
                        onHideCommandDialog()
                        onEditingCommandRule(null)
                    }
                }
            }
        )
    }
}

/**
 * 命令表格行
 */
@Composable
private fun CommandTableRow(
    commandRule: CommandRuleData,
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
        // 名称
        Text(
            text = commandRule.name,
            fontSize = AppTypography.BodyMedium.fontSize,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(0.2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 命令
        Text(
            text = commandRule.script,
            fontSize = AppTypography.BodySmall.fontSize,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(0.3f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 工作目录
        Text(
            text = if (commandRule.workingDirectory.isNotBlank()) commandRule.workingDirectory else "默认目录",
            fontSize = AppTypography.BodySmall.fontSize,
            color = if (commandRule.workingDirectory.isNotBlank()) AppColors.TextPrimary else AppColors.TextDisabled,
            modifier = Modifier.weight(0.25f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // 备注
        Text(
            text = commandRule.remarks,
            fontSize = AppTypography.BodySmall.fontSize,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(0.25f),
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
