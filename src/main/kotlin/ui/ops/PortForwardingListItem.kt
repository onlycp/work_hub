package ui.ops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.PortForwardingRuleData
import theme.*

/**
 * 端口转发列表项组件
 */
@Composable
fun PortForwardingListItem(
    rule: PortForwardingRuleData,
    isActive: Boolean,
    isConnected: Boolean,
    onToggleStatus: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerSmall),
        color = AppColors.BackgroundSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态指示器
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = when {
                    !isConnected -> AppColors.TextDisabled
                    isActive -> AppColors.Success
                    else -> AppColors.Warning
                }
            ) {}

            Spacer(modifier = Modifier.width(AppDimensions.SpaceS))

            // 规则名称
            Text(
                text = rule.description.ifEmpty { "端口 ${rule.localPort}" },
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(120.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(AppDimensions.SpaceM))

            // 端口转发信息
            Text(
                text = "${rule.localPort} → ${rule.remoteHost}:${rule.remotePort}",
                style = AppTypography.Caption,
                color = AppColors.TextPrimary,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )

            // 自动启动标记
            if (rule.autoStart) {
                Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = AppColors.Warning.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "自动",
                        style = AppTypography.Caption,
                        color = AppColors.Warning,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            // 启动/停止按钮
            if (isConnected) {
                Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                IconButton(
                    onClick = { onToggleStatus(rule.id) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isActive) "停止" else "启动",
                        tint = if (isActive) AppColors.Error else AppColors.Success,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
