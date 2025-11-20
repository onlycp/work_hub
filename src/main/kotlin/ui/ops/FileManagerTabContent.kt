package ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.SSHConfigData
import theme.*
import ui.ops.SFTPBrowser

/**
 * 文件管理标签内容
 */
@Composable
fun FileManagerTabContent(
    config: SSHConfigData,
    isConnected: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimensions.PaddingScreen)
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "文件管理",
                fontSize = AppTypography.TitleMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

        // 显示文件浏览器
        val sessionManager = service.SSHSessionManager.getSession(config.name)
        val sftpManager = sessionManager?.getSftpManager()
        
        ui.ops.SFTPBrowser(
            sftpManager = sftpManager,
            modifier = Modifier.fillMaxSize()
        )
    }
}
