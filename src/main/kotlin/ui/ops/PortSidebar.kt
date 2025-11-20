package ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.PortForwardingRuleData
import data.PortManager
import kotlinx.coroutines.launch
import theme.AppColors
import theme.AppDimensions
import theme.AppTypography

/**
 * 端口管理侧边栏
 */
@Composable
fun PortSidebar(
    modifier: Modifier = Modifier,
    onAddPortRule: () -> Unit,
    onEditPortRule: (PortForwardingRuleData) -> Unit,
    onDeletePortRule: (PortForwardingRuleData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val portRules by PortManager.portRules.collectAsState()

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
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "端口转发",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text(
                        text = "端口转发规则",
                        style = AppTypography.BodyMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onAddPortRule,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加规则",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // 端口规则列表
            if (portRules.isEmpty()) {
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AppColors.TextDisabled,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "暂无端口转发规则",
                            style = AppTypography.BodyMedium,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "点击上方 + 按钮添加规则",
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
                    items(portRules) { rule ->
                        PortRuleItem(
                            rule = rule,
                            onEdit = { onEditPortRule(rule) },
                            onDelete = { onDeletePortRule(rule) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 端口规则项组件
 */
@Composable
private fun PortRuleItem(
    rule: PortForwardingRuleData,
    onEdit: () -> Unit,
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
            // 第一行：规则名称和操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 规则名称
                Text(
                    text = rule.description.ifEmpty { "端口转发 ${rule.localPort}" },
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

            // 第二行：端口信息
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
            ) {
                // 本地端口
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "本地:",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${rule.localPort}",
                        style = AppTypography.Caption,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }

                // 箭头
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "转发到",
                    tint = AppColors.TextDisabled,
                    modifier = Modifier.size(10.dp)
                )

                // 远程端口
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${rule.remoteHost}:${rule.remotePort}",
                        style = AppTypography.Caption,
                        color = AppColors.TextPrimary,
                        fontSize = 10.sp
                    )
                }
            }

            // 第三行：自动启动标记（如果启用）
            Spacer(modifier = Modifier.height(2.dp))
            if (rule.autoStart) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = AppColors.Warning.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "自动启动",
                            style = AppTypography.Caption,
                            color = AppColors.Warning,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}
