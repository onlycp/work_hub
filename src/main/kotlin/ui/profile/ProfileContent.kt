package ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import data.UserSettingsManager
import data.CurrentUserManager
import data.MemberManager
import kotlinx.coroutines.launch
import theme.*

/**
 * 个人信息界面
 */
@Composable
fun ProfileContent(
    onLogout: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val settings by remember { mutableStateOf(UserSettingsManager.getCurrentSettings()) }
    val userName = CurrentUserManager.getCurrentUserName()
    val userId = CurrentUserManager.getCurrentUserId()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppDimensions.PaddingScreen)
    ) {
        // 用户信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp,
            backgroundColor = AppColors.Surface
        ) {
            Column(
                modifier = Modifier.padding(AppDimensions.PaddingL),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL)
            ) {
                // 用户头像和基本信息
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL)
                ) {
                    // 用户头像
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(40.dp))
                            .background(AppColors.Primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "用户头像",
                            modifier = Modifier.size(48.dp),
                            tint = AppColors.Primary
                        )
                    }

                    // 用户信息
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                    ) {
                        Text(
                            text = userName.ifEmpty { "未设置用户名" },
                            fontSize = AppTypography.TitleLarge.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )

                        Text(
                            text = "用户ID: ${userId.ifEmpty { "未设置" }}",
                            fontSize = AppTypography.BodyLarge.fontSize,
                            color = AppColors.TextSecondary
                        )

                        Text(
                            text = "系统状态: 正常",
                            fontSize = AppTypography.BodySmall.fontSize,
                            color = AppColors.Success
                        )
                    }
                }

                Divider(color = AppColors.Divider)

                // 系统信息
                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)) {
                    Text(
                        text = "系统信息",
                        fontSize = AppTypography.TitleMedium.fontSize,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "应用版本",
                            fontSize = AppTypography.BodySmall.fontSize,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "1.0.0",
                            fontSize = AppTypography.BodySmall.fontSize,
                            color = AppColors.TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "最后更新时间",
                            fontSize = AppTypography.BodySmall.fontSize,
                            color = AppColors.TextSecondary
                        )
                        Text(
                            text = "2025-01-18",
                            fontSize = AppTypography.BodySmall.fontSize,
                            color = AppColors.TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

        // 操作按钮
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                backgroundColor = AppColors.Error,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "退出登录",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
            Text("退出登录", style = AppTypography.BodySmall)
        }
    }
}
