package ui.ops

import androidx.compose.foundation.background
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
import data.SSHConfigData
import theme.*

/**
 * 运维抽屉面板
 */
@Composable
fun OpsDrawerPanel(
    selectedTab: ui.common.OpsDrawerTab,
    config: SSHConfigData,
    isConnected: Boolean,
    showPortDialog: Boolean,
    editingPortRule: data.PortForwardingRuleData?,
    showCommandDialog: Boolean = false,
    editingCommandRule: data.CommandRuleData? = null,
    onClose: () -> Unit = {},
    onShowPortDialog: () -> Unit,
    onHidePortDialog: () -> Unit,
    onEditingPortRule: (data.PortForwardingRuleData?) -> Unit,
    onShowCommandDialog: () -> Unit = {},
    onHideCommandDialog: () -> Unit = {},
    onEditingCommandRule: (data.CommandRuleData?) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .width(640.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        elevation = 8.dp,
        color = AppColors.Background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部标题栏和收缩按钮
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Surface,
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.PaddingM, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                    ) {
                        Icon(
                            imageVector = selectedTab.icon,
                            contentDescription = null,
                            tint = AppColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = selectedTab.displayName,
                            fontSize = AppTypography.TitleMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                    }

                    // 显眼的收缩按钮
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                color = AppColors.Primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "收起",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Divider(color = AppColors.Divider)

            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    ui.common.OpsDrawerTab.COMMANDS -> {
                        CommandsTabContent(
                            showCommandDialog = showCommandDialog,
                            editingCommandRule = editingCommandRule,
                            onShowCommandDialog = onShowCommandDialog,
                            onHideCommandDialog = onHideCommandDialog,
                            onEditingCommandRule = onEditingCommandRule,
                            onExecutingCommandRule = { /* TODO */ }
                        )
                    }
                    ui.common.OpsDrawerTab.PORT_FORWARDING -> {
                        PortForwardingTabContent(
                            showPortDialog = showPortDialog,
                            editingPortRule = editingPortRule,
                            onShowPortDialog = onShowPortDialog,
                            onHidePortDialog = onHidePortDialog,
                            onEditingPortRule = onEditingPortRule
                        )
                    }
                    ui.common.OpsDrawerTab.MONITORING -> {
                        SystemMonitoringTabContent(
                            config = config,
                            isConnected = isConnected
                        )
                    }
                    ui.common.OpsDrawerTab.FILE_MANAGER -> {
                        FileManagerTabContent(
                            config = config,
                            isConnected = isConnected
                        )
                    }
                }
            }
        }
    }
}

