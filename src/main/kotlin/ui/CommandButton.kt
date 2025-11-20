package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.CommandRuleData
import theme.*

/**
 * 现代化命令卡片按钮组件
 */
@Composable
fun CommandButton(
    commandRule: CommandRuleData,
    isConnected: Boolean,
    onExecuteCommand: (CommandRuleData) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isExecuting by remember { mutableStateOf(false) } // TODO: 从状态管理获取

    // 动画效果
    val elevation by animateFloatAsState(
        targetValue = if (isHovered && isConnected) 8f else 2f,
        animationSpec = tween(durationMillis = 200)
    )

    val scale by animateFloatAsState(
        targetValue = if (isHovered && isConnected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200)
    )

    Card(
        modifier = Modifier
            .width(280.dp) // 固定宽度，更紧凑
            .height(100.dp) // 固定高度
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = AppColors.Shadow,
                spotColor = AppColors.Shadow
            )
            .hoverable(interactionSource = interactionSource)
            .clickable(enabled = isConnected && !isExecuting) {
                if (isConnected) {
                    onExecuteCommand(commandRule)
                } else {
                    onStatusMessage("请先连接到主机")
                }
            },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = if (isHovered && isConnected)
            AppColors.Hover.copy(alpha = 0.3f)
        else
            AppColors.Surface,
        elevation = 0.dp // 使用自定义阴影
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isConnected && !isExecuting) {
                        Brush.verticalGradient(
                            colors = listOf(
                                AppColors.Primary.copy(alpha = 0.05f),
                                AppColors.Surface
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                AppColors.BackgroundSecondary,
                                AppColors.Surface
                            )
                        )
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.PaddingM),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
            ) {
                // 左侧图标区域
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            color = when {
                                !isConnected -> AppColors.BackgroundSecondary
                                else -> Color.Transparent
                            }
                        )
                        .background(
                            brush = when {
                                isExecuting -> Brush.verticalGradient(
                                    colors = listOf(
                                        AppColors.Warning.copy(alpha = 0.2f),
                                        AppColors.Warning.copy(alpha = 0.1f)
                                    )
                                )
                                isConnected -> Brush.verticalGradient(
                                    colors = listOf(
                                        AppColors.Primary.copy(alpha = 0.15f),
                                        AppColors.Primary.copy(alpha = 0.08f)
                                    )
                                )
                                else -> Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent)
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isExecuting -> Icons.Default.Refresh
                            !isConnected -> Icons.Default.Warning
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = when {
                            isExecuting -> "执行中"
                            !isConnected -> "未连接"
                            else -> "执行"
                        },
                        modifier = Modifier.size(24.dp),
                        tint = when {
                            !isConnected -> AppColors.TextDisabled
                            isExecuting -> AppColors.Warning
                            else -> AppColors.Primary
                        }
                    )
                }

                // 右侧信息区域
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 命令名称
                    Text(
                        text = commandRule.name,
                        fontSize = AppTypography.BodyLarge.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (isConnected) AppColors.TextPrimary else AppColors.TextDisabled,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 命令脚本
                    Text(
                        text = commandRule.script.take(60) + if (commandRule.script.length > 60) "..." else "",
                        fontSize = AppTypography.BodySmall.fontSize,
                        color = AppColors.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = AppTypography.BodySmall.lineHeight * 1.2f
                    )

                    // 备注（如果有）
                    if (commandRule.remarks.isNotEmpty()) {
                        Text(
                            text = commandRule.remarks,
                            fontSize = AppTypography.Caption.fontSize,
                            color = AppColors.TextDisabled,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 执行状态指示器（右上角）
            if (isExecuting) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(AppDimensions.SpaceS)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Primary
                    )
                }
            }

            // 连接状态指示器（左下角）
            if (!isConnected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(AppDimensions.SpaceS)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.Error.copy(alpha = 0.1f),
                        border = null
                    ) {
                        Text(
                            text = "未连接",
                            fontSize = AppTypography.Caption.fontSize,
                            color = AppColors.Error,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
