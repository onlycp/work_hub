package ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import data.*
import data.SystemProxyManager
import kotlinx.coroutines.launch
import theme.*

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

    // 分组展开状态
    var proxyGroupExpanded by remember { mutableStateOf(true) }
    var hostGroupExpanded by remember { mutableStateOf(true) }

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
                expanded = proxyGroupExpanded,
                onExpandedChange = { proxyGroupExpanded = it },
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
                expanded = hostGroupExpanded,
                onExpandedChange = { hostGroupExpanded = it },
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
                            configs = visibleHosts,
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
 * 操作分组组件 - 带轮廓和背景，支持收缩展开
 */
@Composable
private fun ActionGroup(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onSettingsClick: (() -> Unit)? = null,
    expanded: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
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
            // 分组标题 - 更突出的样式，支持点击展开/收缩
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (expanded) AppDimensions.SpaceM else 0.dp)
                    .clickable(
                        enabled = onExpandedChange != null,
                        onClick = { onExpandedChange?.invoke(!expanded) }
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
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

                // 展开/收缩按钮
                onExpandedChange?.let {
                    IconButton(
                        onClick = { it(!expanded) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (expanded) "收缩" else "展开",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

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

            // 内容区域 - 带动画效果
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(200)) + shrinkVertically(animationSpec = tween(200))
            ) {
                content()
            }
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
 * 代理小卡片 - 增强版macOS风格设计
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
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // 动画状态
    val animatedElevation by animateDpAsState(
        targetValue = if (isHovered) 12.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "elevation"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // 根据状态确定视觉风格 - 更丰富的渐变和颜色
    val backgroundBrush: Brush
    val borderColor: Color
    val iconTint: Color
    val statusColor: Color
    val glowColor: Color

    when (state) {
        is HubLinkState.Connected -> {
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF0F9F0), // 更柔和的绿色渐变
                    Color(0xFFE8F8E8),
                    Color(0xFFE1F5E1)
                )
            )
            borderColor = AppColors.Success.copy(alpha = 0.6f)
            iconTint = AppColors.Success
            statusColor = AppColors.Success
            glowColor = AppColors.Success.copy(alpha = 0.1f)
        }
        is HubLinkState.Connecting -> {
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF9E6), // 更温暖的橙色渐变
                    Color(0xFFFFF5D6),
                    Color(0xFFFFF2C2)
                )
            )
            borderColor = AppColors.Warning.copy(alpha = 0.6f)
            iconTint = AppColors.Warning
            statusColor = AppColors.Warning
            glowColor = AppColors.Warning.copy(alpha = 0.1f)
        }
        is HubLinkState.Error -> {
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFF0F0), // 更柔和的红色渐变
                    Color(0xFFFFE6E6),
                    Color(0xFFFFDCDC)
                )
            )
            borderColor = AppColors.Error.copy(alpha = 0.7f)
            iconTint = AppColors.Error
            statusColor = AppColors.Error
            glowColor = AppColors.Error.copy(alpha = 0.1f)
        }
        else -> {
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFCFCFD), // 更精致的默认渐变
                    Color(0xFFF8F9FA),
                    Color(0xFFF4F5F6)
                )
            )
            borderColor = Color(0xFFE8F0FE).copy(alpha = 0.8f)
            iconTint = AppColors.TextSecondary
            statusColor = AppColors.TextDisabled
            glowColor = AppColors.Primary.copy(alpha = 0.05f)
        }
    }

    // 增强版macOS风格的卡片容器
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(110.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .shadow(
                elevation = animatedElevation,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isHovered) glowColor.copy(alpha = 0.3f) else AppColors.Shadow.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundBrush)
            .border(
                width = if (isHovered) 1.5.dp else 1.dp,
                color = if (isHovered) borderColor.copy(alpha = 0.8f) else borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                when (state) {
                    is HubLinkState.Connected -> onDisconnect()
                    is HubLinkState.Connecting -> {} // 连接中不响应
                    else -> onConnect()
                }
            }
    ) {
        Column(modifier = Modifier.padding(AppDimensions.SpaceS)) {
            // 顶部：图标和状态指示器 - 增强版设计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 图标容器 - 增强版macOS风格
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(
                            color = if (isHovered)
                                iconTint.copy(alpha = 0.15f)
                            else
                                iconTint.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(7.dp)
                        )
                        .shadow(
                            elevation = if (isHovered) 2.dp else 1.dp,
                            shape = RoundedCornerShape(7.dp),
                            spotColor = iconTint.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnLock,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // 状态指示点 - 增强版光晕效果
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = statusColor.copy(alpha = if (isHovered) 0.3f else 0.2f),
                            shape = RoundedCornerShape(50)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isHovered) 7.dp else 6.dp)
                            .background(
                                color = statusColor,
                                shape = RoundedCornerShape(50)
                            )
                            .shadow(
                                elevation = 1.dp,
                                shape = RoundedCornerShape(50),
                                spotColor = statusColor.copy(alpha = 0.5f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))

            // 中间：名称和地址分开显示 - 优化布局
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    // 代理名称 - 增强版样式
                    Text(
                        text = config.name,
                        style = AppTypography.BodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.1.sp
                        ),
                        color = if (isHovered)
                            AppColors.TextPrimary.copy(alpha = 0.9f)
                        else
                            AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    // 代理地址 - 更精细的样式
                    Text(
                        text = "${config.host}:${config.port}",
                        style = AppTypography.Caption.copy(
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.05.sp
                        ),
                        color = if (isHovered)
                            AppColors.TextSecondary.copy(alpha = 0.8f)
                        else
                            AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 底部：操作按钮 - 增强版macOS风格
            when (state) {
                is HubLinkState.Connected -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 断开连接按钮 - 增强版设计
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    color = if (isHovered)
                                        AppColors.Error.copy(alpha = 0.9f)
                                    else
                                        AppColors.Error,
                                    shape = RoundedCornerShape(7.dp)
                                )
                                .shadow(
                                    elevation = if (isHovered) 3.dp else 1.dp,
                                    shape = RoundedCornerShape(7.dp),
                                    spotColor = AppColors.Error.copy(alpha = 0.3f)
                                )
                                .clickable { onDisconnect() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                null,
                                Modifier.size(13.dp),
                                tint = Color.White
                            )
                        }

                        // 系统代理开关 - 优化布局
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "系统",
                                style = AppTypography.Caption.copy(
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.05.sp
                                ),
                                color = if (isHovered)
                                    AppColors.TextSecondary.copy(alpha = 0.9f)
                                else
                                    AppColors.TextSecondary,
                                modifier = Modifier.offset(y = (-1).dp)
                            )
                            Switch(
                                checked = isSystemProxyEnabled,
                                onCheckedChange = { enabled ->
                                    onSetSystemProxy(enabled)
                                    isSystemProxyEnabled = enabled
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AppColors.Success,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = AppColors.TextDisabled.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.graphicsLayer(scaleX = 0.7f, scaleY = 0.7f)
                            )
                        }
                    }
                }
                is HubLinkState.Connecting -> {
                    // 连接中状态 - 增强版动画效果
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(
                                color = if (isHovered)
                                    AppColors.Warning.copy(alpha = 0.9f)
                                else
                                    AppColors.Warning.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(7.dp)
                            )
                            .shadow(
                                elevation = if (isHovered) 2.dp else 1.dp,
                                shape = RoundedCornerShape(7.dp),
                                spotColor = AppColors.Warning.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            null,
                            Modifier.size(13.dp),
                            tint = Color.White
                        )
                    }
                }
                else -> {
                    // 连接按钮 - 增强版设计
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .background(
                                color = if (isHovered)
                                    AppColors.Success.copy(alpha = 0.9f)
                                else
                                    AppColors.Success,
                                shape = RoundedCornerShape(7.dp)
                            )
                            .shadow(
                                elevation = if (isHovered) 3.dp else 1.dp,
                                shape = RoundedCornerShape(7.dp),
                                spotColor = AppColors.Success.copy(alpha = 0.3f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            Modifier.size(13.dp),
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
 * 主机小卡片 - 增强版macOS风格设计
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
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // 动画状态
    val animatedElevation by animateDpAsState(
        targetValue = if (isHovered) 12.dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "elevation"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    // 根据连接状态确定视觉风格 - 更丰富的渐变和颜色
    val backgroundBrush: Brush
    val borderColor: Color
    val iconTint: Color
    val statusColor: Color
    val glowColor: Color

    if (isConnected) {
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF0F9F0), // 更柔和的绿色渐变
                Color(0xFFE8F8E8),
                Color(0xFFE1F5E1)
            )
        )
        borderColor = AppColors.Success.copy(alpha = 0.6f)
        iconTint = AppColors.Success
        statusColor = AppColors.Success
        glowColor = AppColors.Success.copy(alpha = 0.1f)
    } else {
        backgroundBrush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFCFCFD), // 更精致的默认渐变
                Color(0xFFF8F9FA),
                Color(0xFFF4F5F6)
            )
        )
        borderColor = Color(0xFFE8F0FE).copy(alpha = 0.8f)
        iconTint = AppColors.TextSecondary
        statusColor = AppColors.TextDisabled
        glowColor = AppColors.Primary.copy(alpha = 0.05f)
    }

    // 增强版macOS风格的卡片容器
    ContextMenuArea(
        items = {
            listOf(
                ContextMenuItem("📋 详情", onClick = onShowDetails),
                ContextMenuItem("🖥️ 终端", onClick = onOpenTerminal)
            )
        }
    ) {
        Box(
            modifier = Modifier.clickable { if (isConnected) onDisconnect() else onConnect() }
                .width(140.dp)
                .height(110.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
                .shadow(
                    elevation = animatedElevation,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = if (isHovered) glowColor.copy(alpha = 0.3f) else AppColors.Shadow.copy(alpha = 0.15f)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundBrush)
                .border(
                    width = if (isHovered) 1.5.dp else 1.dp,
                    color = if (isHovered) borderColor.copy(alpha = 0.8f) else borderColor,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Column(modifier = Modifier.padding(AppDimensions.SpaceS)) {
                // 顶部：图标和状态指示器 - 增强版设计
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 图标容器 - 增强版macOS风格
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                color = if (isHovered)
                                    iconTint.copy(alpha = 0.15f)
                                else
                                    iconTint.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(7.dp)
                            )
                            .shadow(
                                elevation = if (isHovered) 2.dp else 1.dp,
                                shape = RoundedCornerShape(7.dp),
                                spotColor = iconTint.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // 状态指示点 - 增强版光晕效果
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = statusColor.copy(alpha = if (isHovered) 0.3f else 0.2f),
                                shape = RoundedCornerShape(50)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isHovered) 7.dp else 6.dp)
                                .background(
                                    color = statusColor,
                                    shape = RoundedCornerShape(50)
                                )
                                .shadow(
                                    elevation = 1.dp,
                                    shape = RoundedCornerShape(50),
                                    spotColor = statusColor.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))

                // 中间：名称和地址分开显示 - 优化布局
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        // 主机名称 - 增强版样式
                        Text(
                            text = config.name,
                            style = AppTypography.BodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.1.sp
                            ),
                            color = if (isHovered)
                                AppColors.TextPrimary.copy(alpha = 0.9f)
                            else
                                AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // 主机地址 - 更精细的样式
                        Text(
                            text = "${config.host}:${config.port}",
                            style = AppTypography.Caption.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.05.sp
                            ),
                            color = if (isHovered)
                                AppColors.TextSecondary.copy(alpha = 0.8f)
                            else
                                AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 底部：操作按钮 - 增强版macOS风格
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .background(
                            color = if (isHovered)
                                (if (isConnected) AppColors.Error else AppColors.Success).copy(alpha = 0.9f)
                            else
                                if (isConnected) AppColors.Error else AppColors.Success,
                            shape = RoundedCornerShape(7.dp)
                        )
                        .shadow(
                            elevation = if (isHovered) 3.dp else 1.dp,
                            shape = RoundedCornerShape(7.dp),
                            spotColor = (if (isConnected) AppColors.Error else AppColors.Success).copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isConnected) Icons.Default.Stop else Icons.Default.PlayArrow,
                        null,
                        Modifier.size(13.dp),
                        tint = Color.White
                    )
                }
            }
        }

    }
}


