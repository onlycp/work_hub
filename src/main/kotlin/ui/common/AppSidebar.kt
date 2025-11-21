package ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import theme.*

/**
 * 左侧功能栏
 */
@Composable
fun Sidebar(
    selectedModule: ModuleType,
    onModuleSelected: (ModuleType) -> Unit,
    currentUserName: String = ""
) {
    Surface(
        modifier = Modifier
            .width(AppDimensions.SidebarWidth)
            .fillMaxHeight(),
        color = AppColors.Surface, // macOS侧边栏使用白色背景
        elevation = AppDimensions.ElevationXS // macOS风格的细微阴影
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AppDimensions.SpaceS),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 模块按钮列表
            ModuleType.entries.forEach { module ->
                if (module != ModuleType.PROFILE && module != ModuleType.SETTINGS && module != ModuleType.LOGS) { // 个人信息按钮在底部单独处理，设置按钮已在顶部工具栏，日志按钮仅在顶部工具栏显示
                    ModuleButton(
                        module = module,
                        isSelected = module == selectedModule,
                        onClick = { onModuleSelected(module) }
                    )
                }
            }

            // 底部个人信息按钮
            Spacer(modifier = Modifier.weight(1f))

            ModuleButton(
                module = ModuleType.PROFILE,
                isSelected = selectedModule == ModuleType.PROFILE,
                onClick = { onModuleSelected(ModuleType.PROFILE) },
                customDisplayName = if (currentUserName.isNotEmpty()) currentUserName else null
            )
        }
    }
}

/**
 * 模块按钮
 */
@Composable
private fun ModuleButton(
    module: ModuleType,
    isSelected: Boolean,
    onClick: () -> Unit,
    customDisplayName: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = Modifier
            .width(AppDimensions.SidebarWidth)
            .padding(vertical = AppDimensions.SpaceXS),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 图标容器
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable(onClick = onClick)
                .hoverable(interactionSource),
            shape = RoundedCornerShape(AppDimensions.CornerMedium), // macOS风格更大圆角
            color = if (isSelected)
                AppColors.Primary.copy(alpha = 0.1f) // macOS更柔和的选择色
            else if (isHovered)
                AppColors.BackgroundTertiary
            else
                Color.Transparent
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = module.icon,
                    contentDescription = module.displayName,
                    tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 文字标签
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = customDisplayName ?: module.displayName,
            fontSize = 9.sp,
            color = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            fontFamily = AppTypography.Caption.fontFamily
        )
    }
}
