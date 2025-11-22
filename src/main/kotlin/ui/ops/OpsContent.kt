package ui.ops

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Cable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.SSHConfigData
import theme.*
import ui.common.OpsDrawerTab
import ui.ops.*

/**
 * 运维内容主组件
 */
@Composable
fun OpsContent(
    sshConfigs: List<SSHConfigData>,
    selectedSSHConfigId: String?,
    selectedOpsTab: OpsDrawerTab,
    showOpsDrawer: Boolean,
    sshConnectionStates: Map<String, Boolean>,
    sshConnectionTimes: Map<String, Long>,
    openedHostTabs: List<String>,
    selectedHostTabId: String?,
    showPortDialog: Boolean,
    editingPortRule: data.PortForwardingRuleData?,
    showCommandDialog: Boolean,
    editingCommandRule: data.CommandRuleData?,
    executingCommandRule: data.CommandRuleData?,
    autoReconnectEnabled: Map<String, Boolean>,
    reconnectingStates: Map<String, Boolean>,
    currentUserId: String,
    onSSHConfigSelected: (String) -> Unit,
    onSSHConfigEdit: (String) -> Unit,
    onSSHConfigDelete: (String) -> Unit,
    onSSHConfigConnect: (String) -> Unit,
    onSSHConfigDisconnect: (String) -> Unit,
    onSSHConfigShare: (String) -> Unit,
    onSSHConfigAddNew: () -> Unit,
    onOpenHostTab: (String) -> Unit,
    onCloseHostTab: (String) -> Unit,
    onHostTabSelected: (String) -> Unit,
    onOpsTabSelected: (OpsDrawerTab) -> Unit,
    onOpsDrawerToggle: (Boolean) -> Unit,
    onShowPortDialog: () -> Unit,
    onHidePortDialog: () -> Unit,
    onEditingPortRule: (data.PortForwardingRuleData?) -> Unit,
    onShowCommandDialog: () -> Unit,
    onHideCommandDialog: () -> Unit,
    onEditingCommandRule: (data.CommandRuleData?) -> Unit,
    onExecutingCommandRule: (data.CommandRuleData?) -> Unit,
    onAutoReconnectChanged: (String, Boolean) -> Unit,
    onOpenHostTerminal: (String) -> Unit = {},
    onStatusMessage: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区域（左侧SSH列表 + 中间操作区）
        Row(modifier = Modifier.fillMaxSize()) {
            // 左侧主机管理区（SSH配置列表）
            SSHConfigListView(
                configs = sshConfigs,
                selectedConfig = selectedHostTabId,
                currentUserId = currentUserId,
                onConfigSelected = { configId ->
                    onOpenHostTab(configId)
                },
                onConfigEdit = onSSHConfigEdit,
                onConfigDelete = onSSHConfigDelete,
                onConfigConnect = onSSHConfigConnect,
                onConfigShare = onSSHConfigShare,
                onAddNew = onSSHConfigAddNew,
                modifier = Modifier.width(280.dp)
            )

            // 中间主操作区（Tab布局）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 56.dp)
            ) {
                // Tab栏
                if (openedHostTabs.isNotEmpty()) {
                    if (openedHostTabs.size == 1) {
                        // 单个tab的简单布局
                        SingleTabHeader(
                            configId = openedHostTabs.first(),
                            sshConfigs = sshConfigs,
                            sshConnectionStates = sshConnectionStates,
                            onCloseHostTab = onCloseHostTab
                        )
                    } else {
                        // 多个tab使用ScrollableTabRow
                        MultipleTabsHeader(
                            openedHostTabs = openedHostTabs,
                            selectedHostTabId = selectedHostTabId,
                            sshConfigs = sshConfigs,
                            sshConnectionStates = sshConnectionStates,
                            onHostTabSelected = onHostTabSelected,
                            onCloseHostTab = onCloseHostTab
                        )
                    }
                }

                // Tab内容区域
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedHostTabId != null) {
                        val selectedConfig = sshConfigs.find { it.id == selectedHostTabId }
                        if (selectedConfig != null) {
                            // 初始化端口和命令管理器
                            LaunchedEffect(selectedHostTabId) {
                                if (data.PortManager.getCurrentConfigId() != selectedHostTabId) {
                                    data.PortManager.setCurrentConfig(selectedHostTabId)
                                }
                                if (data.CommandManager.getCurrentConfigId() != selectedHostTabId) {
                                    data.CommandManager.setCurrentConfig(selectedHostTabId)
                                }
                            }

                            OpsMainContent(
                                config = selectedConfig,
                                selectedOpsTab = selectedOpsTab,
                                sshConnectionStates = sshConnectionStates,
                                sshConnectionTimes = sshConnectionTimes,
                                showPortDialog = showPortDialog,
                                editingPortRule = editingPortRule,
                                showCommandDialog = showCommandDialog,
                                editingCommandRule = editingCommandRule,
                                executingCommandRule = executingCommandRule,
                                autoReconnectEnabled = autoReconnectEnabled,
                                reconnectingStates = reconnectingStates,
                                onTabSelected = onOpsTabSelected,
                                onConnect = onSSHConfigConnect,
                                onDisconnect = onSSHConfigDisconnect,
                                onShowPortDialog = onShowPortDialog,
                                onHidePortDialog = onHidePortDialog,
                                onEditingPortRule = onEditingPortRule,
                                onShowCommandDialog = onShowCommandDialog,
                                onHideCommandDialog = onHideCommandDialog,
                                onEditingCommandRule = onEditingCommandRule,
                                onExecutingCommandRule = onExecutingCommandRule,
                                onAutoReconnectChanged = onAutoReconnectChanged,
                                onOpenHostTerminal = onOpenHostTerminal,
                                onStatusMessage = onStatusMessage
                            )
                        } else {
                            DefaultOpsContent()
                        }
                    } else {
                        // 默认提示界面
                        EmptyOpsContent(
                            sshConfigs = sshConfigs,
                            onOpenHostTab = onOpenHostTab
                        )
                    }
                }
            }
        }

        // 右侧工具栏
        if (selectedHostTabId != null) {
            println("显示右侧工具栏，selectedHostTabId=$selectedHostTabId")
            OpsToolBar(
                selectedTab = selectedOpsTab,
                isExpanded = showOpsDrawer,
                onTabSelected = { tab ->
                    onOpsTabSelected(tab)
                    onOpsDrawerToggle(true)
                },
                onToggleExpanded = onOpsDrawerToggle,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }

        // 抽屉遮罩层
        AnimatedVisibility(
            visible = showOpsDrawer,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 56.dp) // 留出工具栏空间
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable { onOpsDrawerToggle(false) }
            )
        }

        // 抽屉面板
        AnimatedVisibility(
            visible = showOpsDrawer,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 56.dp) // 留出工具栏空间
        ) {
            val drawerConfig = sshConfigs.find { it.id == selectedHostTabId }
            if (drawerConfig != null) {
                println("右侧边栏显示中，selectedOpsTab=$selectedOpsTab, drawerConfig=${drawerConfig.name}")
                OpsDrawerPanel(
                    selectedTab = selectedOpsTab,
                    config = drawerConfig,
                    isConnected = sshConnectionStates[drawerConfig.id] == true,
                    showPortDialog = showPortDialog,
                    editingPortRule = editingPortRule,
                    showCommandDialog = showCommandDialog,
                    editingCommandRule = editingCommandRule,
                    onClose = { onOpsDrawerToggle(false) },
                    onShowPortDialog = onShowPortDialog,
                    onHidePortDialog = onHidePortDialog,
                    onEditingPortRule = onEditingPortRule,
                    onShowCommandDialog = onShowCommandDialog,
                    onHideCommandDialog = onHideCommandDialog,
                    onEditingCommandRule = onEditingCommandRule
                )
            } else {
                Text("配置未找到", style = AppTypography.BodyLarge, color = AppColors.Error)
            }
        }
    }
}

/**
 * 单个Tab的标题栏
 */
@Composable
private fun SingleTabHeader(
    configId: String,
    sshConfigs: List<SSHConfigData>,
    sshConnectionStates: Map<String, Boolean>,
    onCloseHostTab: (String) -> Unit
) {
    val config = sshConfigs.find { it.id == configId }
    val isConnected = sshConnectionStates[configId] == true

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.BackgroundSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isConnected) {
                Surface(
                    modifier = Modifier.size(6.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = AppColors.Success
                ) {}
                Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
            }

            Text(
                text = config?.name ?: "未知主机",
                style = AppTypography.BodySmall,
                color = AppColors.Primary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { onCloseHostTab(configId) },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * 多个Tab的标题栏
 */
@Composable
private fun MultipleTabsHeader(
    openedHostTabs: List<String>,
    selectedHostTabId: String?,
    sshConfigs: List<SSHConfigData>,
    sshConnectionStates: Map<String, Boolean>,
    onHostTabSelected: (String) -> Unit,
    onCloseHostTab: (String) -> Unit
) {
    // 使用自定义的水平滚动Row替换ScrollableTabRow，避免状态同步问题
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.BackgroundSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = AppDimensions.SpaceS, vertical = AppDimensions.SpaceXS)
        ) {
            openedHostTabs.forEach { configId ->
                key(configId) {
                    val config = sshConfigs.find { it.id == configId }
                    val isConnected = sshConnectionStates[configId] == true
                    val isSelected = selectedHostTabId == configId

                    // 自定义Tab样式
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 1.dp, vertical = 1.dp)
                            .clickable { onHostTabSelected(configId) },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent,
                        border = if (isSelected) BorderStroke(
                            1.dp,
                            AppColors.Primary.copy(alpha = 0.3f)
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isConnected) {
                                Surface(
                                    modifier = Modifier.size(6.dp),
                                    shape = RoundedCornerShape(3.dp),
                                    color = AppColors.Success
                                ) {}
                            }

                            Text(
                                text = config?.name ?: "未知主机",
                                style = AppTypography.Caption,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            IconButton(
                                onClick = { onCloseHostTab(configId) },
                                modifier = Modifier.size(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 空状态提示内容
 */
@Composable
private fun EmptyOpsContent(
    sshConfigs: List<SSHConfigData>,
    onOpenHostTab: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimensions.SpaceXXL),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Computer,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpaceL))
        Text(
            text = "主机管理中心",
            style = AppTypography.TitleLarge,
            color = AppColors.TextPrimary
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
        Text(
            text = "选择左侧主机开始管理",
            style = AppTypography.BodyMedium,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

        if (sshConfigs.isNotEmpty()) {
            Button(
                onClick = { onOpenHostTab(sshConfigs.first().id) },
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("打开第一个主机", style = AppTypography.BodyMedium)
            }
        }
    }
}
