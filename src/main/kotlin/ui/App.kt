import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import data.*
import kotlinx.coroutines.launch
import service.*
import theme.*
import ui.common.*
import ui.ops.*
import ui.SSHConfigDialog
import java.util.UUID

/**
 * 应用主界面
 * 采用经典的三段式布局：顶部工具栏、左侧功能栏、右侧内容区、底部状态栏
 */
@Composable
fun App(onLogout: () -> Unit = {}) {
    val settings by remember { mutableStateOf(UserSettingsManager.getCurrentSettings()) }
    val scope = rememberCoroutineScope()

    // 当前选中的模块
    var selectedModule by remember { mutableStateOf(ModuleType.HOME) }

    // 状态栏消息
    var statusMessage by remember { mutableStateOf("应用已就绪") }

    // SSH主机管理
    var sshConfigs by remember { mutableStateOf<List<SSHConfigData>>(emptyList()) }
    var selectedSSHConfigId by remember { mutableStateOf<String?>(null) }
    var showSSHConfigDialog by remember { mutableStateOf(false) }
    var editingSSHConfigId by remember { mutableStateOf<String?>(null) }

    // SSH连接状态管理
    var sshConnectionStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var sshConnectionTimes by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var autoReconnectEnabled by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var reconnectingStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // 打开的主机tab管理（需要在startAutoReconnect和onSSHConnect之前定义）
    var openedHostTabs by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedHostTabId by remember { mutableStateOf<String?>(null) }

    val onAutoReconnectChanged = { configId: String, enabled: Boolean ->
        autoReconnectEnabled = autoReconnectEnabled + (configId to enabled)
        statusMessage = if (enabled) "已启用自动重连" else "已禁用自动重连"
    }

    // 自动重连逻辑
    suspend fun startAutoReconnect(configId: String) {
        // 每次重连前重新加载最新配置
        val config = SSHConfigManager.getConfigById(configId)
        if (config == null || autoReconnectEnabled[configId] != true) return

        reconnectingStates = reconnectingStates + (configId to true)
        statusMessage = "正在尝试重连 ${config.name}..."

        var retryCount = 0
        val maxRetries = 5
        val baseDelayMs = 2000L // 2秒基础延迟

        while (retryCount < maxRetries && autoReconnectEnabled[configId] == true) {
            try {
                kotlinx.coroutines.delay(baseDelayMs * (retryCount + 1)) // 递增延迟

                // 检查是否已经连接或正在连接
                if (sshConnectionStates[configId] == true) {
                    reconnectingStates = reconnectingStates - configId
                    statusMessage = "连接已恢复 ${config.name}"
                    break
                }

                // 尝试连接
                val sshConfig = SSHConfig.fromSSHConfigData(config)
                val sessionManager = SSHSessionManager.getOrCreateSession(sshConfig)
                val result = sessionManager.connect()

                if (result.isSuccess) {
                    sshConnectionStates = sshConnectionStates + (configId to true)
                    sshConnectionTimes = sshConnectionTimes + (configId to System.currentTimeMillis())

                    // 加载端口转发规则（从SSHConfigManager获取最新配置）
                    val latestConfig = SSHConfigManager.getConfigById(configId)
                    val portRules = latestConfig?.portForwardingRules ?: emptyList()
                    if (portRules.isNotEmpty()) {
                        sessionManager.loadPortForwardingRules(portRules)
                        statusMessage = "自动重连成功 ${config.name} (加载了 ${portRules.size} 个端口转发规则)"
                    } else {
                        statusMessage = "自动重连成功 ${config.name}"
                    }
                    
                    // 如果当前选中的主机是正在重连的主机，需要重新加载PortManager的配置
                    if (selectedHostTabId == configId) {
                        data.PortManager.setCurrentConfig(configId)
                    }

                    reconnectingStates = reconnectingStates - configId
                    break
                } else {
                    retryCount++
                    if (retryCount < maxRetries) {
                        statusMessage = "重连失败 ${config.name}，${retryCount}/${maxRetries} 次重试..."
                    }
                }
            } catch (e: Exception) {
                retryCount++
                if (retryCount < maxRetries) {
                    statusMessage = "重连异常 ${config.name}，${retryCount}/${maxRetries} 次重试..."
                }
            }
        }

        if (retryCount >= maxRetries) {
            reconnectingStates = reconnectingStates - configId
            statusMessage = "自动重连失败 ${config.name}，已达到最大重试次数"
        }
    }

    // SSH连接/断开操作
    val onSSHConnect = { configId: String ->
        // 从SSHConfigManager获取最新配置
        val config = SSHConfigManager.getConfigById(configId)
        if (config != null) {
            scope.launch {
                try {
                    statusMessage = "正在连接到 ${config.name}..."
                    val sshConfig = SSHConfig.fromSSHConfigData(config)
                    val sessionManager = SSHSessionManager.getOrCreateSession(sshConfig)
                    val result = sessionManager.connect()

                    if (result.isSuccess) {
                        sshConnectionStates = sshConnectionStates + (configId to true)
                        sshConnectionTimes = sshConnectionTimes + (configId to System.currentTimeMillis())
                        reconnectingStates = reconnectingStates - configId // 清除重连状态

                        // 加载端口转发规则（从SSHConfigManager获取最新配置）
                        val latestConfig = SSHConfigManager.getConfigById(configId)
                        val portRules = latestConfig?.portForwardingRules ?: emptyList()
                        if (portRules.isNotEmpty()) {
                            sessionManager.loadPortForwardingRules(portRules)
                            statusMessage = "已连接到 ${config.name} (加载了 ${portRules.size} 个端口转发规则)"
                        } else {
                            statusMessage = "已连接到 ${config.name}"
                        }
                        
                        // 如果当前选中的主机是正在连接的主机，需要重新加载PortManager的配置
                        if (selectedHostTabId == configId) {
                            data.PortManager.setCurrentConfig(configId)
                        }
                    } else {
                        statusMessage = "连接失败: ${result.exceptionOrNull()?.message}"
                    }
                } catch (e: Exception) {
                    statusMessage = "连接失败: ${e.message}"
                }
            }
        }
    }

    val onSSHDisconnect = { configId: String ->
        val config = sshConfigs.find { it.id == configId }
        if (config != null) {
            // 停止所有端口转发
            SSHSessionManager.getSession(config.name)?.let { session ->
                session.disconnect()
                // 重置端口状态
                val portRules = config.portForwardingRules
                for (rule in portRules) {
                    data.PortManager.setPortRuleStatus(rule.id, false)
                }
            }
            SSHSessionManager.removeSession(config.name)
            sshConnectionStates = sshConnectionStates + (configId to false)
            sshConnectionTimes = sshConnectionTimes - configId
            reconnectingStates = reconnectingStates - configId // 清除重连状态
            statusMessage = "已断开连接 ${config.name}"

            // 如果启用了自动重连，启动重连逻辑
            if (autoReconnectEnabled[configId] == true) {
                scope.launch {
                    startAutoReconnect(configId)
                }
            }
        }
    }

    // 运维界面状态
    var selectedOpsTab by remember { mutableStateOf(OpsDrawerTab.COMMANDS) }
    val showOpsDrawerState = remember { mutableStateOf(false) }
    var showOpsDrawer by showOpsDrawerState

    // 端口转发对话框状态
    var showPortDialog by remember { mutableStateOf(false) }
    var editingPortRule by remember { mutableStateOf<data.PortForwardingRuleData?>(null) }

    // 命令对话框状态
    var showCommandDialog by remember { mutableStateOf(false) }
    var editingCommandRule by remember { mutableStateOf<data.CommandRuleData?>(null) }
    var executingCommandRule by remember { mutableStateOf<data.CommandRuleData?>(null) }

    // 命令对话框回调函数
    val onShowCommandDialog = { showCommandDialog = true }
    val onHideCommandDialog = { showCommandDialog = false }
    val onEditingCommandRule = { rule: data.CommandRuleData? -> editingCommandRule = rule }
    val onExecutingCommandRule = { rule: data.CommandRuleData? -> executingCommandRule = rule }

    // 抽屉切换函数
    val onOpsDrawerToggle: (Boolean) -> Unit = { show -> showOpsDrawerState.value = show }

    // 打开主机tab
    val onOpenHostTab = { configId: String ->
        if (!openedHostTabs.contains(configId)) {
            openedHostTabs = openedHostTabs + configId
        }
        selectedHostTabId = configId
        // 打开主机时不自动展开右侧抽屉面板
    }

    // 关闭主机tab
    val onCloseHostTab = { configId: String ->
        val newTabs = openedHostTabs - configId
        openedHostTabs = newTabs
        if (selectedHostTabId == configId) {
            selectedHostTabId = newTabs.lastOrNull()
        }
    }

    // 选择主机tab
    val onHostTabSelected = { configId: String ->
        selectedHostTabId = configId
        // 选择主机时不自动展开右侧抽屉面板
    }

    // 初始化和重新加载SSH配置
    LaunchedEffect(Unit) {
        try {
            // 确保数据被正确加载
            sshConfigs = SSHConfigManager.getAllConfigs()
            println("SSH配置加载完成，共 ${sshConfigs.size} 个配置")
        } catch (e: Exception) {
            println("SSH配置加载失败: ${e.message}")
            statusMessage = "数据加载失败: ${e.message}"
        }
    }

    AppTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部工具栏
            TopToolbar(
                onLogClick = { statusMessage = "打开日志查看器" },
                onSettingsClick = {
                    selectedModule = ModuleType.SETTINGS
                    statusMessage = "切换到设置界面"
                },
                onSyncClick = {
                    scope.launch {
                        try {
                            statusMessage = "正在同步数据..."

                            // 1. 推送当前用户的数据
                            val currentUser = CurrentUserManager.getCurrentUserId()
                            if (currentUser.isNotEmpty()) {
                                val pushResult = GitDataManager.pushCurrentUserData(currentUser)
                                if (pushResult.isFailure) {
                                    statusMessage = "推送数据失败: ${pushResult.exceptionOrNull()?.message}"
                                    return@launch
                                }
                            }

                            // 2. 拉取其他分支的数据
                            val syncResult = GitDataManager.syncAllBranches()
                            if (syncResult.isFailure) {
                                statusMessage = "拉取数据失败: ${syncResult.exceptionOrNull()?.message}"
                                return@launch
                            }

                            // 3. 更新内存中的数据
                            SSHConfigManager.setCurrentUser(currentUser)
                            KeyManager.setCurrentUser(currentUser)
                            CursorRuleManager.setCurrentUser(currentUser)
                            MemberManager.setCurrentUser(currentUser)

                            // 重新加载SSH配置
                            sshConfigs = SSHConfigManager.getAllConfigs()

                            // 重新初始化各个管理器
                            SSHConfigManager.setCurrentUser(currentUser)
                            KeyManager.setCurrentUser(currentUser)
                            CursorRuleManager.setCurrentUser(currentUser)
                            MemberManager.setCurrentUser(currentUser)
                            ExpenseManager.setCurrentUser(currentUser)

                            statusMessage = "数据同步完成"
                        } catch (e: Exception) {
                            statusMessage = "同步失败: ${e.message}"
                        }
                    }
                }
            )

            // 主内容区
            Row(modifier = Modifier.weight(1f)) {
                // 左侧功能栏
                Sidebar(
                    selectedModule = selectedModule,
                    onModuleSelected = { module ->
                        selectedModule = module
                        statusMessage = "切换到 ${module.displayName}"
                    }
                )

                // 右侧内容区
                Box(modifier = Modifier.weight(1f)) {
                ContentArea(
                    selectedModule = selectedModule,
                    statusMessage = statusMessage,
                    onStatusMessage = { message -> statusMessage = message },
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
                    onSSHConfigSelected = { selectedSSHConfigId = it },
                    onSSHConfigEdit = { configId ->
                        editingSSHConfigId = configId
                        showSSHConfigDialog = true
                    },
                    onSSHConfigDelete = { configId ->
                        scope.launch {
                            SSHConfigManager.deleteConfig(configId).fold(
                                onSuccess = {
                                    sshConfigs = SSHConfigManager.getAllConfigs()
                                    if (selectedSSHConfigId == configId) {
                                        selectedSSHConfigId = null
                                    }
                                    statusMessage = "主机配置已删除"
                                },
                                onFailure = { error ->
                                    statusMessage = "删除失败: ${error.message}"
                                }
                            )
                        }
                    },
                    onSSHConfigConnect = { configId ->
                        onSSHConnect(configId)
                    },
                    onSSHConfigDisconnect = { configId ->
                        onSSHDisconnect(configId)
                    },
                    onSSHConfigAddNew = {
                        editingSSHConfigId = null
                        showSSHConfigDialog = true
                    },
                    onOpenHostTab = { configId ->
                        onOpenHostTab(configId)
                    },
                    onCloseHostTab = { configId ->
                        onCloseHostTab(configId)
                    },
                    onHostTabSelected = { configId ->
                        onHostTabSelected(configId)
                    },
                    onOpsTabSelected = { selectedOpsTab = it },
                    onOpsDrawerToggle = { showOpsDrawer = it },
                    onShowPortDialog = { showPortDialog = true },
                    onHidePortDialog = { showPortDialog = false },
                    onEditingPortRule = { rule -> editingPortRule = rule },
                    onShowCommandDialog = onShowCommandDialog,
                    onHideCommandDialog = onHideCommandDialog,
                    onEditingCommandRule = onEditingCommandRule,
                    onExecutingCommandRule = onExecutingCommandRule,
                    onAutoReconnectChanged = onAutoReconnectChanged,
                    onLogout = {
                        // 退出登录逻辑
                        scope.launch {
                            try {
                                // 断开所有SSH连接
                                sshConnectionStates.keys.forEach { configId ->
                                    onSSHDisconnect(configId)
                                }
                                // 清理状态
                                selectedModule = ModuleType.HOME
                                selectedSSHConfigId = null
                                openedHostTabs = emptyList()
                                selectedHostTabId = null
                                statusMessage = "已退出登录"

                                // 调用外部的退出登录回调（跳转到登录页）
                                onLogout()
                            } catch (e: Exception) {
                                statusMessage = "退出登录失败: ${e.message}"
                            }
                        }
                    }
                )
                }
            }

            // 底部状态栏
            StatusBar(statusMessage = statusMessage)

            // SSH配置对话框
            if (showSSHConfigDialog) {
                SSHConfigDialog(
                    initialConfig = editingSSHConfigId?.let { id ->
                        sshConfigs.find { it.id == id }
                    },
                    onDismiss = { showSSHConfigDialog = false },
                    onSave = { name, host, portStr, username, password, privateKeyPath, privateKeyPassphrase, group, usePassword, keyId, isShared ->
                        scope.launch {
                            try {
                                val port = portStr.toIntOrNull() ?: 22
                                if (editingSSHConfigId != null) {
                                    // 编辑现有配置
                                    val existingConfig = sshConfigs.find { it.id == editingSSHConfigId }
                                    if (existingConfig != null) {
                                        val updatedConfig = existingConfig.copy(
                                            name = name,
                                            host = host,
                                            port = port,
                                            username = username,
                                            password = if (usePassword) password else "",
                                            privateKeyPath = if (usePassword.not()) privateKeyPath else "",
                                            privateKeyPassphrase = if (usePassword.not()) privateKeyPassphrase else "",
                                            group = group,
                                            keyId = keyId,
                                            isShared = isShared,
                                            lastModified = System.currentTimeMillis()
                                        )
                                        SSHConfigManager.updateConfig(editingSSHConfigId!!, updatedConfig).fold(
                                            onSuccess = {
                                                sshConfigs = SSHConfigManager.getAllConfigs()
                                                statusMessage = "主机配置已更新"
                                            },
                                            onFailure = { error ->
                                                statusMessage = "更新失败: ${error.message}"
                                            }
                                        )
                                    }
                                } else {
                                    // 创建新配置
                                    val newConfig = SSHConfigData(
                                        name = name,
                                        host = host,
                                        port = port,
                                        username = username,
                                        password = if (usePassword) password else "",
                                        privateKeyPath = if (usePassword.not()) privateKeyPath else "",
                                        privateKeyPassphrase = if (usePassword.not()) privateKeyPassphrase else "",
                                        group = group,
                                        keyId = keyId,
                                        createdBy = data.CurrentUserManager.getCurrentUserId(),
                                        isShared = isShared
                                    )
                                    SSHConfigManager.addConfig(newConfig).fold(
                                        onSuccess = {
                                            sshConfigs = SSHConfigManager.getAllConfigs()
                                            statusMessage = "主机配置已创建"
                                        },
                                        onFailure = { error ->
                                            statusMessage = "创建失败: ${error.message}"
                                        }
                                    )
                                }
                            } catch (e: Exception) {
                                statusMessage = "操作失败: ${e.message}"
                            }
                        }
                        showSSHConfigDialog = false
                    }
                )
            }
        }

        // 命令对话框
        if (showCommandDialog) {
            CommandDialog(
                initialRule = editingCommandRule,
                onDismiss = onHideCommandDialog,
                onSave = { rule ->
                    scope.launch {
                        try {
                            val currentEditingRule = editingCommandRule
                            val result = if (currentEditingRule != null) {
                                // 编辑模式：使用原有ID更新规则
                                val updatedRule = rule.copy(id = currentEditingRule.id)
                                data.CommandManager.updateCommandRule(currentEditingRule.id, updatedRule)
                            } else {
                                // 新增模式：生成新ID
                                val newId = UUID.randomUUID().toString()
                                val newRule = rule.copy(id = newId)
                                data.CommandManager.addCommandRule(newRule)
                            }

                            if (result.isSuccess) {
                                showCommandDialog = false
                                editingCommandRule = null
                                statusMessage = if (currentEditingRule != null) "命令规则已更新" else "命令规则已添加"
                            } else {
                                statusMessage = "保存失败: ${result.exceptionOrNull()?.message}"
                            }
                        } catch (e: Exception) {
                            statusMessage = "操作失败: ${e.message}"
                        }
                    }
                }
            )
        }

        // 命令执行对话框
        val currentExecutingRule = executingCommandRule
        if (currentExecutingRule != null) {
            CommandExecutionDialog(
                commandRule = currentExecutingRule,
                onDismiss = { executingCommandRule = null }
            )
        }

    }
}
