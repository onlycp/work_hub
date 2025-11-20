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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.CommandRuleData
import data.CommandManager
import kotlinx.coroutines.launch
import theme.AppColors
import theme.AppDimensions
import theme.AppTypography

/**
 * 命令管理侧边栏
 */
@Composable
fun CommandSidebar(
    modifier: Modifier = Modifier,
    onAddCommandRule: () -> Unit,
    onEditCommandRule: (CommandRuleData) -> Unit,
    onExecuteCommandRule: (CommandRuleData) -> Unit,
    onDeleteCommandRule: (CommandRuleData) -> Unit
) {
    println("CommandSidebar 渲染，onAddCommandRule=$onAddCommandRule")
    val scope = rememberCoroutineScope()
    val commandRules by CommandManager.commandRules.collectAsState()

    Surface(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight(),
        color = AppColors.BackgroundSecondary,
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
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
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "命令管理",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text(
                        text = "命令管理",
                        style = AppTypography.BodyMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            println("点击添加命令按钮")
                            onAddCommandRule()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加命令",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 命令规则列表
            if (commandRules.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = AppColors.TextDisabled,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "暂无命令配置",
                            style = AppTypography.BodyMedium,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "点击上方 + 按钮添加命令",
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
                    contentPadding = PaddingValues(vertical = AppDimensions.SpaceS)
                ) {
                    items(commandRules) { rule ->
                        CommandRuleItem(
                            rule = rule,
                            onEdit = { onEditCommandRule(rule) },
                            onExecute = { onExecuteCommandRule(rule) },
                            onDelete = { onDeleteCommandRule(rule) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 命令规则项组件
 */
@Composable
private fun CommandRuleItem(
    rule: CommandRuleData,
    onEdit: () -> Unit,
    onExecute: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.SpaceS, vertical = 2.dp),
        shape = RoundedCornerShape(AppDimensions.CornerSmall),
        color = AppColors.Surface,
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.SpaceS)
        ) {
            // 第一行：命令名称和操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 命令名称
                Text(
                    text = rule.name,
                    style = AppTypography.BodySmall,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 操作按钮组
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 执行按钮
                    IconButton(
                        onClick = onExecute,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "执行",
                            tint = AppColors.Success,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // 编辑按钮
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    // 删除按钮
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = AppColors.Error,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // 第二行：命令脚本（简化显示）
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rule.script,
                style = AppTypography.Caption,
                color = AppColors.TextSecondary,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // 第三行：备注和自动启动标记
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 备注
                if (rule.remarks.isNotEmpty()) {
                    Text(
                        text = rule.remarks,
                        style = AppTypography.Caption,
                        color = AppColors.TextDisabled,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

            }
        }
    }
}
