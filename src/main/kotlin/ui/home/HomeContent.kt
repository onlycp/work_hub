package ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import theme.AppColors
import theme.AppDimensions
import theme.AppTypography

/**
 * 首页内容
 */
@Composable
fun HomeContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpaceL))
        Text(
            text = "欢迎使用 WorkHub",
            style = AppTypography.TitleLarge,
            color = AppColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
        Text(
            text = "选择左侧功能开始使用",
            style = AppTypography.BodyMedium,
            color = AppColors.TextSecondary
        )
    }
}
