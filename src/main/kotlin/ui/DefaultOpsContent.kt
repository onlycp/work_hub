package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import theme.*

/**
 * 默认运维内容 - 当未选择主机时的显示内容
 */
@Composable
fun DefaultOpsContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(AppDimensions.PaddingL),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp,
            backgroundColor = AppColors.Surface
        ) {
            Column(
                modifier = Modifier.padding(AppDimensions.PaddingXL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL)
            ) {
                // 图标
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "运维工具",
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.Primary.copy(alpha = 0.6f)
                )

                // 标题
                Text(
                    text = "运维工具集",
                    fontSize = AppTypography.TitleLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.Center
                )

                // 描述
                Text(
                    text = "请选择左侧的主机配置开始运维操作\n\n支持的功能：\n• SSH命令执行\n• 端口转发管理\n• 文件传输",
                    fontSize = AppTypography.BodyLarge.fontSize,
                    color = AppColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = AppTypography.BodyLarge.lineHeight
                )

                // 提示信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = AppColors.BackgroundSecondary,
                    elevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(AppDimensions.PaddingM),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "提示",
                            modifier = Modifier.size(20.dp),
                            tint = AppColors.Info
                        )
                        Text(
                            text = "点击左侧主机列表中的主机开始操作",
                            fontSize = AppTypography.BodySmall.fontSize,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}













