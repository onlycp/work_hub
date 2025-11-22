package ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import data.SSHConfigData
import theme.AppColors
import theme.AppDimensions
import ui.cursor.*
import ui.expense.*
import ui.home.*
import ui.hublink.*
import ui.keys.*
import ui.logs.*
import ui.members.*
import ui.ops.*
import ui.profile.*
import ui.settings.*

/**
 * 右侧内容区
 */
@Composable
fun ContentArea(
    selectedModule: ModuleType,
    statusMessage: String,
    onStatusMessage: (String) -> Unit,
    sshConfigs: List<SSHConfigData> = emptyList(),
    hublinkConfigs: List<data.HubLinkConfig> = emptyList(),
    hublinkStates: Map<String, data.HubLinkState> = emptyMap(),
    selectedSSHConfigId: String? = null,
    selectedOpsTab: OpsDrawerTab = OpsDrawerTab.COMMANDS,
    showOpsDrawer: Boolean = false,
    sshConnectionStates: Map<String, Boolean> = emptyMap(),
    sshConnectionTimes: Map<String, Long> = emptyMap(),
    openedHostTabs: List<String> = emptyList(),
    selectedHostTabId: String? = null,
    showPortDialog: Boolean = false,
    editingPortRule: data.PortForwardingRuleData? = null,
    showCommandDialog: Boolean = false,
    editingCommandRule: data.CommandRuleData? = null,
    executingCommandRule: data.CommandRuleData? = null,
    autoReconnectEnabled: Map<String, Boolean> = emptyMap(),
    reconnectingStates: Map<String, Boolean> = emptyMap(),
    onSSHConfigSelected: (String) -> Unit = {},
    onSSHConfigEdit: (String) -> Unit = {},
    onSSHConfigDelete: (String) -> Unit = {},
    onSSHConfigConnect: (String) -> Unit = {},
    onSSHConfigDisconnect: (String) -> Unit = {},
    onSSHConfigAddNew: () -> Unit = {},
    onSSHConfigShare: (String) -> Unit = {},
    currentUserId: String = "",
    onOpenHostTab: (String) -> Unit = {},
    onCloseHostTab: (String) -> Unit = {},
    onHostTabSelected: (String) -> Unit = {},
    onOpsTabSelected: (OpsDrawerTab) -> Unit = {},
    onOpsDrawerToggle: (Boolean) -> Unit = {},
    onShowPortDialog: () -> Unit = {},
    onHidePortDialog: () -> Unit = {},
    onEditingPortRule: (data.PortForwardingRuleData?) -> Unit = {},
    onShowCommandDialog: () -> Unit = {},
    onHideCommandDialog: () -> Unit = {},
    onEditingCommandRule: (data.CommandRuleData?) -> Unit = {},
    onExecutingCommandRule: (data.CommandRuleData?) -> Unit = {},
    onAutoReconnectChanged: (String, Boolean) -> Unit = { _, _ -> },
    onHubLinkConnect: (String) -> Unit = {},
    onHubLinkDisconnect: (String) -> Unit = {},
    onSetSystemProxy: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    onShowHostDetails: (String) -> Unit = {},
    onOpenHostTerminal: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 主内容区
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(AppColors.BackgroundPrimary)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimensions.PaddingScreen)
                ) {
                    when (selectedModule) {
                        ModuleType.HOME -> HomeContent(
                            hublinkConfigs = hublinkConfigs,
                            hublinkStates = hublinkStates,
                            sshConfigs = sshConfigs,
                            sshConnectionStates = sshConnectionStates.mapValues { it.value },
                            onHubLinkConnect = onHubLinkConnect,
                            onHubLinkDisconnect = onHubLinkDisconnect,
                            onSetSystemProxy = onSetSystemProxy,
                            onSSHConnect = onSSHConfigConnect,
                            onSSHDisconnect = onSSHConfigDisconnect,
                            onShowHostDetails = onShowHostDetails,
                            onOpenHostTerminal = onOpenHostTerminal,
                            onStatusMessage = onStatusMessage
                        )
                        ModuleType.OPS -> OpsContent(
                            sshConfigs = sshConfigs,
                            selectedSSHConfigId = selectedSSHConfigId,
                            selectedOpsTab = selectedOpsTab,
                            showOpsDrawer = showOpsDrawer,
                            sshConnectionStates = sshConnectionStates,
                            sshConnectionTimes = sshConnectionTimes,
                            openedHostTabs = openedHostTabs,
                            selectedHostTabId = selectedHostTabId,
                            showPortDialog = showPortDialog,
                            editingPortRule = editingPortRule,
                            showCommandDialog = showCommandDialog,
                            editingCommandRule = editingCommandRule,
                            executingCommandRule = executingCommandRule,
                            autoReconnectEnabled = autoReconnectEnabled,
                            reconnectingStates = reconnectingStates,
                            onSSHConfigSelected = onSSHConfigSelected,
                            onSSHConfigEdit = onSSHConfigEdit,
                            onSSHConfigDelete = onSSHConfigDelete,
                            onSSHConfigConnect = onSSHConfigConnect,
                            onSSHConfigDisconnect = onSSHConfigDisconnect,
                            onSSHConfigAddNew = onSSHConfigAddNew,
                            onSSHConfigShare = onSSHConfigShare,
                            currentUserId = currentUserId,
                            onOpenHostTab = onOpenHostTab,
                            onCloseHostTab = onCloseHostTab,
                            onHostTabSelected = onHostTabSelected,
                            onOpsTabSelected = onOpsTabSelected,
                            onOpsDrawerToggle = onOpsDrawerToggle,
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
                        ModuleType.KEYS -> KeysContent(onStatusMessage = onStatusMessage)
                        ModuleType.CURSOR -> CursorContent(onStatusMessage = onStatusMessage)
                        ModuleType.MEMBERS -> MembersContent(onStatusMessage = onStatusMessage)
                        ModuleType.EXPENSE -> ExpenseContent()
                        ModuleType.LOGS -> LogsContent()
                        ModuleType.PROFILE -> ProfileContent(onLogout = onLogout)
                        ModuleType.SETTINGS -> SettingsContent()
                        ModuleType.HUBLINK -> HubLinkContent(onBack = {})
                    }
                }
            }
        }
    }
}
