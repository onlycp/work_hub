package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import theme.AppColors
import theme.AppDimensions
import theme.AppTypography
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 底部状态栏
 */
@Composable
fun StatusBar(statusMessage: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.StatusBarHeight)
            .background(AppColors.BackgroundSecondary)
            .padding(horizontal = AppDimensions.PaddingScreen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = statusMessage,
            style = AppTypography.Caption,
            color = AppColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            style = AppTypography.Caption,
            color = AppColors.TextSecondary
        )
    }
}
