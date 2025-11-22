package ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import data.*
import theme.*

/**
 * 自适应列数计算器
 */
@Composable
private fun calculateAdaptiveColumns(availableWidth: Dp, minCardWidth: Dp, cardSpacing: Dp): Int {
    // 计算一列的最小宽度（卡片宽度 + 间距）
    val minColumnWidth = minCardWidth + cardSpacing

    // 计算最大列数
    val maxColumns = max(1, (availableWidth / minColumnWidth).toInt())

    // 限制最大列数，避免过于拥挤（最大6列）
    return maxColumns.coerceAtMost(6)
}

/**
 * 通用的操作卡片组件
 */
@Composable
private fun OperationCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    statusIcon: @Composable () -> Unit,
    statusText: String,
    buttons: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.BackgroundSecondary)
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingL)) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    icon()
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Column {
                        Text(
                            text = title,
                            style = AppTypography.BodyLarge,
                            color = AppColors.TextPrimary
                        )
                        Text(
                            text = subtitle,
                            style = AppTypography.Caption,
                            color = AppColors.TextSecondary
                        )
                    }
                }
                statusIcon()
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            // 状态文本
            Text(
                text = statusText,
                style = AppTypography.Caption,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            // 操作按钮行
            buttons()
        }
    }
}

/**
 * 首页内容 - 提供快速开启代理和主机的操作区
 */
@Composable
fun HomeContent(
    // 代理相关状态
    hublinkConfigs: List<HubLinkConfig> = emptyList(),
    hublinkStates: Map<String, HubLinkState> = emptyMap(),

    // SSH主机相关状态
    sshConfigs: List<SSHConfigData> = emptyList(),
    sshConnectionStates: Map<String, Boolean> = emptyMap(),

    // 回调函数
    onHubLinkConnect: (String) -> Unit = {},
    onHubLinkDisconnect: (String) -> Unit = {},
    onSetSystemProxy: (String, Int, Boolean) -> Unit = { _, _, _ -> },
    onSSHConnect: (String) -> Unit = {},
    onSSHDisconnect: (String) -> Unit = {},
    onShowHostDetails: (String) -> Unit = {},
    onOpenHostTerminal: (String) -> Unit = {},
    onStatusMessage: (String) -> Unit = {}
) {
    // 配置对话框状态
    var showProxyConfigDialog by remember { mutableStateOf(false) }
    var showHostConfigDialog by remember { mutableStateOf(false) }

    // 初始化配置管理器并监听配置变化
    val indexConfig by IndexConfigManager.config.collectAsState()

    LaunchedEffect(Unit) {
        IndexConfigManager.setCurrentUser(data.CurrentUserManager.getCurrentUserId())
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(AppDimensions.PaddingScreen),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL)
    ) {
        // 代理服务分组
        item {
            ActionGroup(
                title = "代理服务",
                icon = Icons.Default.VpnLock,
                onSettingsClick = { showProxyConfigDialog = true },
                content = {
                    val visibleProxies = hublinkConfigs.filter { config ->
                        val visibleIds = indexConfig?.visibleProxyIds ?: emptySet()
                        visibleIds.isEmpty() || visibleIds.contains(config.id)
                    }

                    if (visibleProxies.isEmpty()) {
                        if (hublinkConfigs.isEmpty()) {
                            EmptyStateCard(Icons.Default.VpnLock, "无代理配置", "请在代理管理中添加配置")
                        } else {
                            EmptyStateCard(Icons.Default.VpnLock, "无显示的代理", "请在设置中选择要显示的代理")
                        }
                    } else {
                        ProxyCardsGrid(
                            configs = visibleProxies,
                            states = hublinkStates,
                            onConnect = onHubLinkConnect,
                            onDisconnect = onHubLinkDisconnect,
                            onSetSystemProxy = onSetSystemProxy,
                            onStatusMessage = onStatusMessage
                        )
                    }
                }
            )
        }

        // 主机连接分组
        item {
            ActionGroup(
                title = "主机连接",
                icon = Icons.Default.Computer,
                onSettingsClick = { showHostConfigDialog = true },
                content = {
                    val visibleHosts = sshConfigs.filter { config ->
                        val visibleIds = indexConfig?.visibleHostIds ?: emptySet()
                        visibleIds.isEmpty() || visibleIds.contains(config.id)
                    }

                    if (visibleHosts.isEmpty()) {
                        if (sshConfigs.isEmpty()) {
                            EmptyStateCard(Icons.Default.Computer, "无主机配置", "请在运维工具中添加主机配置")
                        } else {
                            EmptyStateCard(Icons.Default.Computer, "无显示的主机", "请在设置中选择要显示的主机")
                        }
                    } else {
                        HostCardsGrid(
                            configs = visibleHosts.take(8),
                            connectionStates = sshConnectionStates,
                            onConnect = onSSHConnect,
                            onDisconnect = onSSHDisconnect,
                            onStatusMessage = onStatusMessage,
                            onShowDetails = onShowHostDetails,
                            onOpenTerminal = onOpenHostTerminal
                        )
                    }
                }
            )
        }
    }

        // 代理配置对话框
        if (showProxyConfigDialog) {
            IndexConfigDialog(
                title = "代理显示设置",
                items = hublinkConfigs.map { config ->
                    val visibleIds = indexConfig?.visibleProxyIds ?: emptySet()
                    ConfigItem(
                        id = config.id,
                        name = config.name,
                        subtitle = "${config.host}:${config.port}",
                        isSelected = visibleIds.isEmpty() || visibleIds.contains(config.id)
                    )
                },
            onDismiss = { showProxyConfigDialog = false },
            onConfirm = { selectedIds ->
                IndexConfigManager.updateVisibleProxies(selectedIds)
                showProxyConfigDialog = false
                onStatusMessage("代理显示配置已保存")
            }
        )
    }

        // 主机配置对话框
        if (showHostConfigDialog) {
            IndexConfigDialog(
                title = "主机显示设置",
                items = sshConfigs.map { config ->
                    val visibleIds = indexConfig?.visibleHostIds ?: emptySet()
                    ConfigItem(
                        id = config.id,
                        name = config.name,
                        subtitle = "${config.host}:${config.port}",
                        isSelected = visibleIds.isEmpty() || visibleIds.contains(config.id)
                    )
                },
            onDismiss = { showHostConfigDialog = false },
            onConfirm = { selectedIds ->
                IndexConfigManager.updateVisibleHosts(selectedIds)
                showHostConfigDialog = false
                onStatusMessage("主机显示配置已保存")
            }
        )
    }
}

/**
 * 操作分组组件 - 带轮廓和背景
 */
@Composable
private fun ActionGroup(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSettingsClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA) // 浅灰色背景，更适合白色背景
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color(0xFFE9ECEF) // 浅边框色
        )
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingM)) {
            // 分组标题 - 更突出的样式
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = AppDimensions.SpaceM)
            ) {
                // 图标容器 - 添加背景
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = AppColors.Primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                Text(
                    text = title,
                    style = AppTypography.TitleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                // 设置按钮
                onSettingsClick?.let {
                    IconButton(
                        onClick = it,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 内容区域
            content()
        }
    }
}


/**
 * 空状态卡片
 */
@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.BackgroundSecondary)
    ) {
    Column(
            modifier = Modifier.padding(AppDimensions.PaddingM),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
                imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
            Text(
                text = title,
                style = AppTypography.BodyMedium,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))
            Text(
                text = subtitle,
                style = AppTypography.Caption,
                color = AppColors.TextDisabled
            )
        }
    }
}

/**
 * 代理卡片网格
 */
@Composable
private fun ProxyCardsGrid(
    configs: List<HubLinkConfig>,
    states: Map<String, HubLinkState>,
    onConnect: (String) -> Unit,
    onDisconnect: (String) -> Unit,
    onSetSystemProxy: (String, Int, Boolean) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    BoxWithConstraints {
        // 根据可用宽度计算列数
        val columns = calculateAdaptiveColumns(
            availableWidth = maxWidth,
            minCardWidth = 140.dp,
            cardSpacing = AppDimensions.SpaceM
        )

        // 将卡片分组为行
        val rows = (configs.size + columns - 1) / columns

        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)) {
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < configs.size) {
                            val config = configs[index]
                            ProxyMiniCard(
                                config = config,
                                state = states[config.id] ?: HubLinkState.Disconnected,
                                onConnect = { onConnect(config.id) },
                                onDisconnect = { onDisconnect(config.id) },
                                onSetSystemProxy = { enable ->
                                    val state = states[config.id]
                                    if (state is HubLinkState.Connected) {
                                        onSetSystemProxy("127.0.0.1", state.localPort, enable)
                                    } else {
                                        onStatusMessage("代理未连接，无法设置系统代理")
                                    }
                                }
                            )
                        } else {
                            // 填充空的占位符
                            Spacer(modifier = Modifier.size(140.dp, 90.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 代理小卡片 - macOS风格设计
 */
@Composable
private fun ProxyMiniCard(
    config: HubLinkConfig,
    state: HubLinkState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSetSystemProxy: (Boolean) -> Unit
) {
    var isSystemProxyEnabled by remember { mutableStateOf(false) }

    // 根据状态确定视觉风格 - 增强对比度
    val backgroundBrush: Brush
    val borderColor: Color
    val iconTint: Color
    val statusColor: Color

    when (state) {
        is HubLinkState.Connected -> {
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF0F8F0), // 浅绿色背景
                    Color(0xFFE8F5E8)
                )
            )
            borderColor = AppColors.Success.copy(alpha = 0.4f)
            iconTint = AppColors.Success
            statusColor = AppColors.Success
        }
        is HubLinkState.Connecting -> {
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF8E1), // 浅橙色背景
                    Color(0xFFFFF3C4)
                )
            )
            borderColor = AppColors.Warning.copy(alpha = 0.4f)
            iconTint = AppColors.Warning
            statusColor = AppColors.Warning
        }
        is HubLinkState.Error -> {
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFEBEE), // 浅红色背景
                    Color(0xFFFFCDD2)
                )
            )
            borderColor = AppColors.Error.copy(alpha = 0.5f)
            iconTint = AppColors.Error
            statusColor = AppColors.Error
        }
        else -> {
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFAFBFC), // 非常浅的蓝色调背景
                    Color(0xFFF1F3F4)
                )
            )
            borderColor = Color(0xFFE3F2FD) // 浅蓝色边框
            iconTint = AppColors.TextSecondary
            statusColor = AppColors.TextDisabled
        }
    }

    // macOS风格的卡片容器 - 增强对比度
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(110.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = AppColors.Shadow.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable {
                when (state) {
                    is HubLinkState.Connected -> onDisconnect()
                    is HubLinkState.Connecting -> {} // 连接中不响应
                    else -> onConnect()
                }
            }
    ) {
        Column(modifier = Modifier.padding(AppDimensions.SpaceS)) {
            // 顶部：图标和状态指示器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 图标容器 - macOS风格的圆形背景
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = iconTint.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnLock,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // 状态指示点 - macOS风格的光晕效果
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = statusColor,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))

            // 中间：名称和地址分开显示 - 两行布局
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 代理名称
                    Text(
                        text = config.name,
                        style = AppTypography.BodySmall.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 代理地址
                    Text(
                        text = "${config.host}:${config.port}",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 底部：操作按钮 - macOS风格的紧凑布局
            when (state) {
                is HubLinkState.Connected -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 断开连接按钮
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = AppColors.Error,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { onDisconnect() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                null,
                                Modifier.size(12.dp),
                                tint = Color.White
                            )
                        }

                        // 系统代理按钮
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (isSystemProxyEnabled) AppColors.Success else AppColors.Primary,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    onSetSystemProxy(!isSystemProxyEnabled)
                                    isSystemProxyEnabled = !isSystemProxyEnabled
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isSystemProxyEnabled) Icons.Default.Clear else Icons.Default.Settings,
                                null,
                                Modifier.size(12.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                is HubLinkState.Connecting -> {
                    // 连接中状态
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                color = AppColors.Warning.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            null,
                            Modifier.size(12.dp),
                            tint = Color.White
                        )
                    }
                }
                else -> {
                    // 连接按钮
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                color = AppColors.Success,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            Modifier.size(12.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * 主机卡片网格
 */
@Composable
private fun HostCardsGrid(
    configs: List<SSHConfigData>,
    connectionStates: Map<String, Boolean>,
    onConnect: (String) -> Unit,
    onDisconnect: (String) -> Unit,
    onStatusMessage: (String) -> Unit,
    onShowDetails: (String) -> Unit = {},
    onOpenTerminal: (String) -> Unit = {}
) {
    BoxWithConstraints {
        // 根据可用宽度计算列数
        val columns = calculateAdaptiveColumns(
            availableWidth = maxWidth,
            minCardWidth = 140.dp,
            cardSpacing = AppDimensions.SpaceM
        )

        // 将卡片分组为行
        val rows = (configs.size + columns - 1) / columns

        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)) {
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < configs.size) {
                            val config = configs[index]
                            HostMiniCard(
                                config = config,
                                isConnected = connectionStates[config.id] == true,
                                onConnect = { onConnect(config.id) },
                                onDisconnect = { onDisconnect(config.id) },
                                onShowDetails = { onShowDetails(config.id) },
                                onOpenTerminal = { onOpenTerminal(config.id) }
                            )
                        } else {
                            // 填充空的占位符
                            Spacer(modifier = Modifier.size(140.dp, 90.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 主机小卡片
 */
@Composable
private fun HostMiniCard(
    config: SSHConfigData,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onShowDetails: () -> Unit = {},
    onOpenTerminal: () -> Unit = {}
) {
    // 根据连接状态确定视觉风格 - 增强对比度
    val backgroundBrush: Brush
    val borderColor: Color
    val iconTint: Color
    val statusColor: Color

    if (isConnected) {
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F8F0), // 浅绿色背景
                Color(0xFFE8F5E8)
            )
        )
        borderColor = AppColors.Success.copy(alpha = 0.4f)
        iconTint = AppColors.Success
        statusColor = AppColors.Success
    } else {
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFAFBFC), // 非常浅的蓝色调背景
                Color(0xFFF1F3F4)
            )
        )
        borderColor = Color(0xFFE3F2FD) // 浅蓝色边框
        iconTint = AppColors.TextSecondary
        statusColor = AppColors.TextDisabled
    }

    // macOS风格的卡片容器 - 增强对比度
    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("  📋 详情") { onShowDetails() },
                ContextMenuItem("  🖥️ 终端") { onOpenTerminal() }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(110.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = AppColors.Shadow.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundBrush)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { if (isConnected) onDisconnect() else onConnect() }
        ) {
        Column(modifier = Modifier.padding(AppDimensions.SpaceS)) {
            // 顶部：图标和状态指示器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 图标容器 - macOS风格的圆形背景
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = iconTint.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Computer,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // 状态指示点 - macOS风格的光晕效果
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = statusColor,
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))

            // 中间：名称和地址分开显示 - 两行布局
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // 主机名称
                    Text(
                        text = config.name,
                        style = AppTypography.BodySmall.copy(fontWeight = FontWeight.Medium),
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 主机地址
                    Text(
                        text = "${config.host}:${config.port}",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 底部：操作按钮 - macOS风格的紧凑布局
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        color = if (isConnected) AppColors.Error else AppColors.Success,
                        shape = RoundedCornerShape(6.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                    null,
                    Modifier.size(12.dp),
                    tint = Color.White
                )
            }
        }
    }
    }
}

/**
 * 配置项数据类
 */
data class ConfigItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val isSelected: Boolean
)

/**
 * 首页配置对话框
 */

/**
 * 首页配置对话框 - 简化版本
 */
@Composable
private fun IndexConfigDialog(
    title: String,
    items: List<ConfigItem>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(items.filter { it.isSelected }.map { it.id }.toSet()) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(AppDimensions.PaddingL)) {
                // 标题
                Text(
                    text = title,
                    style = AppTypography.TitleLarge,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = AppDimensions.SpaceL)
                )

                // 配置项列表
                if (items.isEmpty()) {
                    Text("无配置项", style = AppTypography.BodyMedium, color = AppColors.TextSecondary)
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        // 只显示前几个项目，避免界面太长
                        items.take(10).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedIds = if (selectedIds.contains(item.id)) {
                                            selectedIds - item.id
                                        } else {
                                            selectedIds + item.id
                                        }
                                    }
                                    .padding(vertical = AppDimensions.SpaceS),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedIds.contains(item.id),
                                    onCheckedChange = null // 由点击处理
                                )

                                Spacer(modifier = Modifier.width(AppDimensions.SpaceM))

                                Column(modifier = Modifier.weight(1f)) {
        Text(
                                        text = item.name,
            style = AppTypography.BodyMedium,
                                        color = AppColors.TextPrimary
                                    )
                                    Text(
                                        text = item.subtitle,
                                        style = AppTypography.Caption,
            color = AppColors.TextSecondary
        )
                                }
                            }
                        }
                        if (items.size > 10) {
                            Text(
                                text = "还有 ${items.size - 10} 个项目...",
                                style = AppTypography.Caption,
                                color = AppColors.TextSecondary,
                                modifier = Modifier.padding(vertical = AppDimensions.SpaceS)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = AppColors.TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(AppDimensions.SpaceM))

                    Button(
                        onClick = { onConfirm(selectedIds) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text("确定")
                    }
                }
            }
        }
    }
}
