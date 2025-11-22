package ui.hublink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import data.HubLinkConfig
import data.HubLinkState
import data.HubLinkReconnectState
import data.HubLinkTransportType
import service.SystemProxySetter
import theme.*


/**
 * HubLink连接控制卡片
 */
@Composable
fun HubLinkConnectionCard(
    config: HubLinkConfig,
    state: HubLinkState,
    reconnectState: HubLinkReconnectState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCancelReconnect: () -> Unit,
    onCancelConnect: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingCard),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
        ) {
            // 标题和状态
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)) {
                    Text(
                        text = config.name,
                        style = AppTypography.BodySmall,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "${config.host}:${config.port}",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = when (config.transport) {
                            HubLinkTransportType.DIRECT -> "直接连接"
                            HubLinkTransportType.MQTT -> "MQTT代理"
                        },
                        style = AppTypography.Caption,
                        color = AppColors.Primary
                    )
                }

                // 连接状态指示器
                ConnectionStatusIndicator(state = state)
            }

            // 连接控制按钮行
            ConnectionControlButtons(
                state = state,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onCancel = onCancelConnect
            )

            // 重连状态显示
            ReconnectStatusIndicator(
                reconnectState = reconnectState,
                onCancelReconnect = onCancelReconnect
            )

            // 连接详情 (已连接时显示)
            if (state is HubLinkState.Connected) {
                ConnectionDetails(state)
            }

            // 错误信息显示
            if (state is HubLinkState.Error) {
                ErrorMessageCard(state.message)
            }
        }
    }
}

/**
 * 连接状态指示器
 */
@Composable
fun ConnectionStatusIndicator(state: HubLinkState) {
    val (color, icon, text) = when (state) {
        HubLinkState.Disconnected ->
            Triple(AppColors.TextDisabled, Icons.Default.RadioButtonUnchecked, "未连接")
        HubLinkState.Connecting ->
            Triple(AppColors.Primary, Icons.Default.RadioButtonChecked, "连接中...")
        is HubLinkState.Connected ->
            Triple(AppColors.Primary, Icons.Default.RadioButtonChecked, "已连接")
        is HubLinkState.Error ->
            Triple(AppColors.Error, Icons.Default.Error, "连接失败")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = AppTypography.BodySmall,
            color = color
        )
    }
}

/**
 * 连接控制按钮
 */
@Composable
fun ConnectionControlButtons(
    state: HubLinkState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state) {
            HubLinkState.Disconnected, is HubLinkState.Error -> {
                Button(onClick = onConnect) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("连接")
                }
            }

            HubLinkState.Connecting -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("连接中...", style = AppTypography.BodySmall)
                }
                // 终止连接按钮
                onCancel?.let { cancel ->
                    TextButton(
                        onClick = cancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AppColors.Error
                        )
                    ) {
                        Text("终止", style = AppTypography.BodySmall)
                    }
                }
            }

            is HubLinkState.Connected -> {
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Error
                    )
                ) {
                    Icon(Icons.Default.Stop, null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("断开")
                }
            }
        }
    }
}

/**
 * 重连状态指示器
 */
@Composable
fun ReconnectStatusIndicator(
    reconnectState: HubLinkReconnectState,
    onCancelReconnect: () -> Unit
) {
    when (reconnectState) {
        HubLinkReconnectState.Idle -> {
            // 不显示任何内容
        }

        is HubLinkReconnectState.Waiting -> {
            val remainingTime = (reconnectState.nextRetryAt - System.currentTimeMillis()) / 1000
            if (remainingTime > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text(
                        "${remainingTime}秒后重连...",
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = onCancelReconnect) {
                        Text("取消", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        HubLinkReconnectState.Retrying -> {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("正在重连...", style = MaterialTheme.typography.bodySmall)
            }
        }

        HubLinkReconnectState.MaxRetriesExceeded -> {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(AppDimensions.CornerSmall),
                color = Color(0xFFFFDAD6) // 错误容器的背景色
            ) {
                Text(
                    text = "重连失败，已达到最大重试次数",
                    style = AppTypography.Caption,
                    color = Color(0xFF410002), // 错误容器上的文字颜色
                    modifier = Modifier.padding(AppDimensions.SpaceS)
                )
            }
        }
    }
}

/**
 * 连接详情
 */
@Composable
fun ConnectionDetails(state: HubLinkState.Connected) {
    Surface(
        shape = RoundedCornerShape(AppDimensions.CornerSmall),
        color = AppColors.Primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
        ) {
            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = AppColors.Primary
            )
            Text(
                "代理端口: ${state.localPort}",
                style = AppTypography.Caption,
                color = AppColors.TextPrimary
            )
            Text(
                "远程: ${state.remoteHost}:${state.remotePort}",
                style = AppTypography.Caption,
                color = AppColors.TextPrimary
            )
        }
    }
}

/**
 * 错误信息卡片
 */
@Composable
fun ErrorMessageCard(message: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFFDAD6) // 错误容器的背景色
            ) {
                Text(
                    text = message,
                    style = AppTypography.Caption,
                    color = Color(0xFF410002), // 错误容器上的文字颜色
            modifier = Modifier.padding(8.dp)
        )
    }
}

/**
 * 系统代理设置卡片
 */
@Composable
fun SystemProxyCard() {
    var systemProxyEnabled by remember { mutableStateOf(false) }
    var currentProxy by remember { mutableStateOf<SystemProxySetter.ProxyInfo?>(null) }

    // 获取当前系统代理设置
    LaunchedEffect(Unit) {
        currentProxy = SystemProxySetter.getCurrentProxy()
        // TODO: 检查是否为HubLink设置的代理
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingCard),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)) {
                    Text(
                        "系统代理",
                        style = AppTypography.BodySmall,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        "自动配置系统HTTP/SOCKS代理",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }

                Switch(
                    checked = systemProxyEnabled,
                    onCheckedChange = { enabled ->
                        systemProxyEnabled = enabled
                        // TODO: 实现系统代理设置
                        // SystemProxySetter.setProxy(...)
                    }
                )
            }

            if (systemProxyEnabled) {
                Text(
                    "系统代理已启用，所有流量将通过HubLink代理转发",
                    style = AppTypography.BodySmall,
                    color = AppColors.Primary
                )
            }

            currentProxy?.let { proxy ->
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = AppColors.SurfaceVariant
                ) {
                    Text(
                        "当前系统代理: ${proxy.host}:${proxy.port}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * HubLink配置信息卡片
 */
@Composable
fun HubLinkConfigCard(config: HubLinkConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface)
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingCard),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
        ) {
            Text(
                "配置信息",
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary
            )

            // 配置详情
            ConfigDetailRow("服务器", "${config.host}:${config.port}")
            ConfigDetailRow("本地SOCKS5端口", if (config.localPort > 0) config.localPort.toString() else "自动分配")
            ConfigDetailRow("传输方式", when (config.transport) {
                HubLinkTransportType.DIRECT -> "直接TCP连接"
                HubLinkTransportType.MQTT -> "MQTT代理"
            })

            if (config.transport == HubLinkTransportType.MQTT) {
                config.mqttConfig?.let { mqtt ->
                    ConfigDetailRow("MQTT Broker", "${mqtt.mqttHost}:${mqtt.mqttPort}")
                    if (mqtt.useSSL) {
                        ConfigDetailRow("MQTT安全", "SSL/TLS启用")
                    }
                    ConfigDetailRow("客户端ID", mqtt.clientId)
                    ConfigDetailRow("服务端ID", mqtt.serverId)
                }
            }

            if (config.obfs != null) {
                ConfigDetailRow("流量混淆", "${config.obfs} (${config.obfsHost})")
            }

            ConfigDetailRow("自动重连", if (config.autoReconnect) "启用" else "禁用")
            if (config.autoReconnect) {
                ConfigDetailRow("重试配置", "最多${config.maxRetries}次，间隔${config.baseRetryDelay}ms-${config.maxRetryDelay}ms")
            }
        }
    }
}

/**
 * 配置详情行
 */
@Composable
private fun ConfigDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = AppTypography.Caption,
            color = AppColors.TextSecondary
        )
        Text(
            text = value,
            style = AppTypography.Caption,
            color = AppColors.TextPrimary
        )
    }
}
