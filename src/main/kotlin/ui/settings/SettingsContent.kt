package ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import data.UserSettingsManager
import theme.*

/**
 * 设置内容 - 用于全屏显示的设置界面（非弹窗）
 */
@Composable
fun SettingsContent() {
    var settings by remember { mutableStateOf(UserSettingsManager.getCurrentSettings()) }
    var selectedTab by remember { mutableStateOf(SettingsTab.GENERAL) }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧边栏
        SettingsSidebar(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // 右侧内容区域
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 标题栏（不需要关闭按钮）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.SpaceM),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedTab.title,
                    fontSize = AppTypography.TitleMedium.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.TextPrimary
                )
            }

            Divider(color = AppColors.Divider)

            // 内容区域 - 复用 SettingsDialog 中的内容组件
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    SettingsTab.GENERAL -> GeneralSettingsContent(
                        settings = settings,
                        onSettingsChange = { settings = it }
                    )
                    SettingsTab.AI -> AISettingsContent(
                        settings = settings,
                        onSettingsChange = { settings = it }
                    )
                }
            }

            Divider(color = AppColors.Divider)

            // 底部保存按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.SpaceM),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = {
                        UserSettingsManager.updateSettings(settings)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppColors.Primary
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("保存设置", color = Color.White)
                }
            }
        }
    }
}
