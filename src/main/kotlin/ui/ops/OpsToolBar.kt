package ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.sp
import theme.*

/**
 * 右侧工具栏组件（固定显示的图标栏）
 */
@Composable
fun OpsToolBar(
    selectedTab: ui.common.OpsDrawerTab,
    isExpanded: Boolean,
    onTabSelected: (ui.common.OpsDrawerTab) -> Unit,
    onToggleExpanded: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight(),
        color = AppColors.SurfaceVariant,
        elevation = AppDimensions.ElevationXS
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AppDimensions.SpaceS),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ui.common.OpsDrawerTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .background(
                            color = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable {
                            if (isSelected) {
                                // 如果点击的是当前选中的tab，则切换展开状态
                                onToggleExpanded(!isExpanded)
                            } else {
                                // 如果点击的是其他tab，则选中并展开
                                onTabSelected(tab)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.displayName,
                            tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = tab.displayName,
                            style = AppTypography.Caption,
                            color = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
