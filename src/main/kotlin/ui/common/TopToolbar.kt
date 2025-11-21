package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import theme.*

/**
 * 顶部工具栏
 */
@Composable
fun TopToolbar(
    onLogClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.ToolbarHeight)
            .background(AppColors.BackgroundPrimary) // macOS风格统一背景色
            .padding(horizontal = AppDimensions.PaddingScreen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 应用标识和标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 应用图标
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = "应用图标",
                tint = AppColors.Primary,
                modifier = Modifier.size(AppDimensions.IconSizeL)
            )

            Spacer(modifier = Modifier.width(AppDimensions.SpaceM))

            // 标题区域
            Column {
                Text(
                    text = "WorkHub",
                    style = AppTypography.TitleMedium,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = "效率工具集",
                    style = AppTypography.Caption,
                    color = AppColors.TextSecondary
                )
            }
        }

        // 右侧按钮组
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
        ) {
            // 分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(AppColors.Divider)
            )

            // 同步按钮
            IconButton(
                onClick = onSyncClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "同步数据",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(AppDimensions.IconSizeM)
                )
            }

            // 设置按钮
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(AppDimensions.IconSizeM)
                )
            }
        }
    }
}
