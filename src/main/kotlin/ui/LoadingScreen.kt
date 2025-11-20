package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import theme.*

/**
 * 加载屏幕
 */
@Composable
fun LoadingScreen(
    message: String = "加载中...",
    showProgress: Boolean = true
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    color = AppColors.Primary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))
            }

            Text(
                text = message,
                style = AppTypography.BodyMedium,
                color = AppColors.TextPrimary
            )
        }
    }
}

/**
 * 错误屏幕
 */
@Composable
fun ErrorScreen(
    errorMessage: String,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(AppDimensions.SpaceXL)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "错误",
                tint = AppColors.Error,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

            Text(
                text = "出现错误",
                style = AppTypography.TitleMedium,
                color = AppColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            Text(
                text = errorMessage,
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(horizontal = AppDimensions.SpaceL)
            )

            if (onRetry != null) {
                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重试"
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text("重试")
                }
            }
        }
    }
}
