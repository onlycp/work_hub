package ui.hublink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.HubLinkConfig
import data.HubLinkManager
import data.CurrentUserManager
import kotlinx.coroutines.launch
import theme.*


/**
 * HubLink主界面
 */
@Composable
fun HubLinkContent(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val configs by HubLinkManager.configs.collectAsState()
    var selectedConfig by remember { mutableStateOf<HubLinkConfig?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var configToEdit by remember { mutableStateOf<HubLinkConfig?>(null) }

    // 设置当前用户
    LaunchedEffect(Unit) {
        HubLinkManager.setCurrentUser(data.CurrentUserManager.getCurrentUserId())
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 左侧：配置列表
        HubLinkSidebar(
            configs = configs,
            selectedConfig = selectedConfig,
            onConfigSelected = { selectedConfig = it },
            onAddConfig = { showAddDialog = true },
            onEditConfig = {
                configToEdit = it
                showEditDialog = true
            },
            onDeleteConfig = { config ->
                scope.launch {
                    HubLinkManager.deleteConfig(config.id)
                    if (selectedConfig?.id == config.id) {
                        selectedConfig = null
                    }
                }
            },
            modifier = Modifier.width(280.dp)
        )

        // 中间：连接详情或欢迎页面
        Box(
            modifier = Modifier
                .weight(1f)
                .background(AppColors.BackgroundPrimary)
                .padding(end = 56.dp)
        ) {
            selectedConfig?.let { config ->
                HubLinkDetailPanel(
                    config = config,
                    onConfigChanged = { /* 配置变化会通过StateFlow自动更新 */ }
                )
            } ?: HubLinkWelcomePanel(onAddConfig = { showAddDialog = true })
        }
    }

    // 添加配置对话框
    if (showAddDialog) {
        HubLinkConfigDialog(
            onSave = { config ->
                scope.launch {
                    val result = HubLinkManager.saveConfig(config)
                    if (result.isSuccess) {
                        showAddDialog = false
                    } else {
                        // 可以在这里显示错误信息
                        println("保存配置失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            },
            onCancel = { showAddDialog = false }
        )
    }

    // 编辑配置对话框
    if (showEditDialog && configToEdit != null) {
        HubLinkConfigDialog(
            config = configToEdit,
            onSave = { config ->
                scope.launch {
                    val result = HubLinkManager.saveConfig(config)
                    if (result.isSuccess) {
                        selectedConfig = config
                        showEditDialog = false
                        configToEdit = null
                    } else {
                        // 可以在这里显示错误信息
                        println("保存配置失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            },
            onCancel = {
                showEditDialog = false
                configToEdit = null
            }
        )
    }
}

/**
 * HubLink侧边栏
 */
@Composable
fun HubLinkSidebar(
    configs: List<HubLinkConfig>,
    selectedConfig: HubLinkConfig?,
    onConfigSelected: (HubLinkConfig) -> Unit,
    onAddConfig: () -> Unit,
    onEditConfig: (HubLinkConfig) -> Unit,
    onDeleteConfig: (HubLinkConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "代理管理 (${configs.size})",
                        style = AppTypography.BodyMedium,
                        color = AppColors.TextPrimary
                    )

                    IconButton(
                        onClick = onAddConfig,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加配置",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 配置列表
            if (configs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimensions.SpaceL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VpnLock,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                        Text(
                            text = "暂无代理配置",
                            style = AppTypography.BodyMedium,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                        Text(
                            text = "点击上方 + 按钮添加配置",
                            style = AppTypography.Caption,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(configs) { config ->
                        HubLinkConfigItem(
                            config = config,
                            isSelected = config.id == selectedConfig?.id,
                            onClick = { onConfigSelected(config) },
                            onEdit = { onEditConfig(config) },
                            onDelete = { onDeleteConfig(config) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * HubLink配置项
 */
@Composable
fun HubLinkConfigItem(
    config: HubLinkConfig,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.SpaceXS, vertical = 2.dp)
            .clickable(onClick = onClick),
        color = if (isSelected)
            AppColors.Primary.copy(alpha = 0.08f)
        else
            Color.Transparent,
        shape = RoundedCornerShape(AppDimensions.CornerSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.SpaceS, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态圆点（小）
            Surface(
                modifier = Modifier.size(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (isSelected) AppColors.Primary else AppColors.TextDisabled
            ) {}

            Spacer(modifier = Modifier.width(AppDimensions.SpaceS))

            // 配置信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.name,
                    style = AppTypography.BodyMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = "${config.host}:${config.port}",
                    style = AppTypography.Caption,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
                Text(
                    text = when (config.transport) {
                        data.HubLinkTransportType.DIRECT -> "直接连接"
                        data.HubLinkTransportType.MQTT -> "MQTT代理"
                    },
                    style = AppTypography.Caption,
                    color = AppColors.Primary
                )
            }

            // 操作按钮
            Row {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = AppColors.Error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * HubLink详情面板
 */
@Composable
fun HubLinkDetailPanel(
    config: HubLinkConfig,
    onConfigChanged: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val clientManager by remember { mutableStateOf(HubLinkManager.getClientManager(config.id)) }

    // 使用LazyColumn来提供更好的滚动体验和布局
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppDimensions.PaddingScreen),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXL),
        contentPadding = PaddingValues(
            top = AppDimensions.PaddingScreen,
            bottom = AppDimensions.SpaceXL
        )
    ) {
        // 配置信息卡片
        item {
            HubLinkConfigCard(config)
        }

        // 连接控制卡片
        item {
            clientManager?.let { manager ->
                val state by manager.state.collectAsState()
                val reconnectState by manager.reconnectState.collectAsState()

                HubLinkConnectionCard(
                    config = config,
                    state = state,
                    reconnectState = reconnectState,
                    onConnect = {
                        scope.launch {
                            manager.connect()
                        }
                    },
                    onDisconnect = {
                        scope.launch {
                            manager.disconnect()
                        }
                    },
                    onCancelReconnect = {
                        manager.reconnectManager.stopReconnect()
                    },
                    onCancelConnect = {
                        scope.launch {
                            manager.disconnect() // 终止正在进行的连接
                            onConfigChanged()
                        }
                    }
                )
            }
        }

        // 系统代理设置卡片
        item {
            SystemProxyCard()
        }

        // 添加底部间距
        item {
            Spacer(modifier = Modifier.height(AppDimensions.SpaceL))
        }
    }
}

/**
 * HubLink欢迎面板
 */
@Composable
fun HubLinkWelcomePanel(onAddConfig: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .padding(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VpnLock,
                    contentDescription = "HubLink",
                    modifier = Modifier.size(64.dp),
                    tint = AppColors.Primary
                )

                Text(
                    text = "HubLink 私有代理",
                    style = AppTypography.TitleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Text(
                    text = "安全、高效的私有代理解决方案\n支持直接连接和MQTT代理两种模式",
                    style = AppTypography.BodyMedium,
                    color = AppColors.TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onAddConfig,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary
                    )
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text("添加代理配置", style = AppTypography.BodyMedium)
                }
            }
        }
    }
}
