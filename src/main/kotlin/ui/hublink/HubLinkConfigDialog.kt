package ui.hublink

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import data.HubLinkConfig
import data.HubLinkMqttConfig
import data.HubLinkTransportType
import theme.*
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi


/**
 * HubLink配置对话框
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
fun HubLinkConfigDialog(
    config: HubLinkConfig? = null,
    onSave: (HubLinkConfig) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(config?.name ?: "") }
    var host by remember { mutableStateOf(config?.host ?: "") }
    var port by remember { mutableStateOf(config?.port?.toString() ?: "6180") }
    var localPort by remember { mutableStateOf(config?.localPort?.toString() ?: "0") }
    var psk by remember { mutableStateOf(config?.psk ?: "") }
    var transportType by remember { mutableStateOf(config?.transport ?: HubLinkTransportType.DIRECT) }
    var isShared by remember { mutableStateOf(config?.isShared ?: false) }
    var obfs by remember { mutableStateOf(config?.obfs ?: "none") }
    var obfsHost by remember { mutableStateOf(config?.obfsHost ?: "") }

    // MQTT配置
    var mqttHost by remember { mutableStateOf(config?.mqttConfig?.mqttHost ?: "") }
    var mqttPort by remember { mutableStateOf(config?.mqttConfig?.mqttPort?.toString() ?: "1883") }
    var useSSL by remember { mutableStateOf(config?.mqttConfig?.useSSL ?: false) }
    var clientId by remember { mutableStateOf(config?.mqttConfig?.clientId ?: Uuid.random().toString()) }
    var serverId by remember { mutableStateOf(config?.mqttConfig?.serverId ?: "hublink-server") }
    var useAuth by remember { mutableStateOf(config?.mqttConfig?.username != null) }
    var username by remember { mutableStateOf(config?.mqttConfig?.username ?: "") }
    var password by remember { mutableStateOf(config?.mqttConfig?.password ?: "") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onCancel,
        modifier = Modifier.width(600.dp),
        containerColor = AppColors.BackgroundSecondary,
        title = {
            Text(
                text = if (config == null) "添加HubLink配置" else "编辑HubLink配置",
                style = AppTypography.TitleMedium,
                color = AppColors.TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .padding(vertical = AppDimensions.SpaceS),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL)
            ) {
                // 配置
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = AppColors.BackgroundSecondary
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(AppDimensions.SpaceL),
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
                    ) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("配置名称", style = AppTypography.BodyMedium) },
                            placeholder = { Text("我的代理服务器", style = AppTypography.BodyMedium) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = AppTypography.BodyMedium
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = host,
                                onValueChange = { host = it },
                                label = { Text("服务器地址", style = AppTypography.BodyMedium) },
                                placeholder = { Text("example.com", style = AppTypography.BodyMedium) },
                                modifier = Modifier.weight(1f),
                                textStyle = AppTypography.BodyMedium
                            )

                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it },
                                label = { Text("端口", style = AppTypography.BodyMedium) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = AppTypography.BodyMedium,
                                modifier = Modifier.width(120.dp)
                            )
                        }

                        OutlinedTextField(
                            value = localPort,
                            onValueChange = { localPort = it },
                            label = { Text("本地SOCKS5端口", style = AppTypography.BodyMedium) },
                            placeholder = { Text("0", style = AppTypography.BodyMedium) },
                            supportingText = { Text("本地代理监听端口，0表示自动分配", style = AppTypography.Caption) },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = AppTypography.BodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = psk,
                            onValueChange = { psk = it },
                            label = { Text("预共享密钥", style = AppTypography.BodyMedium) },
                            visualTransformation = PasswordVisualTransformation(),
                            supportingText = { Text("密钥长度至少32个字符，用于加密通信", style = AppTypography.Caption) },
                            textStyle = AppTypography.BodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 传输方式和共享选项
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "传输方式",
                                    style = AppTypography.BodyMedium,
                                    color = AppColors.TextPrimary,
                                    modifier = Modifier.padding(bottom = AppDimensions.SpaceS)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    FilterChip(
                                        selected = transportType == HubLinkTransportType.DIRECT,
                                        onClick = { transportType = HubLinkTransportType.DIRECT },
                                        label = { Text("直接连接") }
                                    )
                                    FilterChip(
                                        selected = transportType == HubLinkTransportType.MQTT,
                                        onClick = { transportType = HubLinkTransportType.MQTT },
                                        label = { Text("MQTT代理") }
                                    )
                                }
                            }

                            // 共享选项
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = isShared,
                                    onCheckedChange = { isShared = it }
                                )
                                Text(
                                    text = "共享配置",
                                    style = AppTypography.BodyMedium,
                                    color = AppColors.TextPrimary
                                )
                            }
                        }

                        // 流量混淆和混淆主机
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 混淆选项
                            var obfsExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = obfs,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("流量混淆", style = AppTypography.BodyMedium) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (obfsExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            modifier = Modifier.clickable { obfsExpanded = !obfsExpanded }
                                        )
                                    },
                                    textStyle = AppTypography.BodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                DropdownMenu(
                                    expanded = obfsExpanded,
                                    onDismissRequest = { obfsExpanded = false }
                                ) {
                                    listOf("none", "tls", "http").forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option, style = AppTypography.BodyMedium) },
                                            onClick = {
                                                obfs = option
                                                obfsExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // 混淆主机
                            AnimatedVisibility(
                                visible = obfs != "none",
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = obfsHost,
                                    onValueChange = { obfsHost = it },
                                    label = { Text("混淆主机", style = AppTypography.BodyMedium) },
                                    placeholder = { Text("www.bing.com", style = AppTypography.BodyMedium) },
                                    textStyle = AppTypography.BodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // MQTT配置 (条件显示)
                if (transportType == HubLinkTransportType.MQTT) {
                    OutlinedCard(
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = AppColors.BackgroundSecondary
                        )
                    ) {
                        MqttConfigSection(
                            mqttHost = mqttHost,
                            onMqttHostChange = { mqttHost = it },
                            mqttPort = mqttPort,
                            onMqttPortChange = { mqttPort = it },
                            useSSL = useSSL,
                            onUseSSLChange = { useSSL = it },
                            clientId = clientId,
                            onClientIdChange = { clientId = it },
                            serverId = serverId,
                            onServerIdChange = { serverId = it },
                            useAuth = useAuth,
                            onUseAuthChange = { useAuth = it },
                            username = username,
                            onUsernameChange = { username = it },
                            password = password,
                            onPasswordChange = { password = it }
                        )
                    }
                }


                // 部署指南
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "🚀 快速部署指南",
                        style = AppTypography.BodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                    Text(
                        text = buildString {
                            append("1. 下载对应平台的HubLink服务端二进制文件\n")
                            append("2. 启动服务端: ./hublink-server -listen :${port} -psk \"${psk.take(10)}...\"\n")
                            append("3. 配置防火墙开放端口 ${port}\n")
                            val localPortDisplay = if (localPort.toIntOrNull() ?: 0 > 0) localPort else "自动分配"
                            append("4. 本地SOCKS5代理将在端口 ${localPortDisplay} 上监听\n")
                            if (transportType == HubLinkTransportType.MQTT) {
                                append("5. 确保MQTT代理服务器可访问: ${if (useSSL) "ssl://" else ""}$mqttHost:$mqttPort")
                            }
                        },
                        style = AppTypography.BodySmall,
                        color = AppColors.TextSecondary
                    )
                }
            }
        },
        confirmButton = {
            val isValid = name.isNotBlank() &&
                         host.isNotBlank() &&
                         psk.length >= 32 &&
                         port.toIntOrNull()?.let { it in 1..65535 } == true &&
                         localPort.toIntOrNull()?.let { it == 0 || it in 1024..65535 } == true &&
                         (transportType == HubLinkTransportType.DIRECT ||
                          (transportType == HubLinkTransportType.MQTT &&
                           mqttHost.isNotBlank() &&
                           clientId.isNotBlank() &&
                           serverId.isNotBlank()))

            Button(
                onClick = {
                    val newConfig = HubLinkConfig(
                        id = config?.id ?: Uuid.random().toString(),
                        name = name,
                        host = host,
                        port = port.toInt(),
                        localPort = localPort.toIntOrNull() ?: 0,
                        psk = psk,
                        transport = transportType,
                        isShared = isShared,
                        mqttConfig = if (transportType == HubLinkTransportType.MQTT) {
                            HubLinkMqttConfig(
                                mqttHost = mqttHost,
                                mqttPort = mqttPort.toIntOrNull() ?: 1883,
                                useSSL = useSSL,
                                clientId = clientId,
                                serverId = serverId,
                                username = if (useAuth && username.isNotBlank()) username else null,
                                password = if (useAuth && password.isNotBlank()) password else null
                            )
                        } else null,
                        obfs = if (obfs == "none") null else obfs,
                        obfsHost = if (obfs == "none") null else obfsHost
                    )
                    onSave(newConfig)
                },
                enabled = isValid,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AppColors.Primary
                )
            ) {
                Text("保存配置", style = AppTypography.BodyMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("取消", style = AppTypography.BodyMedium)
            }
        }
    )
}

/**
 * MQTT配置部分
 */
@Composable
fun MqttConfigSection(
    mqttHost: String,
    onMqttHostChange: (String) -> Unit,
    mqttPort: String,
    onMqttPortChange: (String) -> Unit,
    useSSL: Boolean,
    onUseSSLChange: (Boolean) -> Unit,
    clientId: String,
    onClientIdChange: (String) -> Unit,
    serverId: String,
    onServerIdChange: (String) -> Unit,
    useAuth: Boolean,
    onUseAuthChange: (Boolean) -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(AppDimensions.SpaceL),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
    ) {
        Text(
            text = "MQTT Broker 配置",
            style = AppTypography.BodyLarge,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )

        // MQTT服务器配置
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = mqttHost,
                                onValueChange = onMqttHostChange,
                                label = { Text("Broker 地址", style = AppTypography.BodyMedium) },
                                placeholder = { Text("mqtt.example.com", style = AppTypography.BodyMedium) },
                                modifier = Modifier.weight(1f),
                                textStyle = AppTypography.BodyMedium
                            )

                            OutlinedTextField(
                                value = mqttPort,
                                onValueChange = onMqttPortChange,
                                label = { Text("端口", style = AppTypography.BodyMedium) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp),
                                textStyle = AppTypography.BodyMedium
                            )
        }

        // SSL设置
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = useSSL,
                onCheckedChange = onUseSSLChange
            )
            Text("启用 SSL/TLS", style = AppTypography.BodyMedium, color = AppColors.TextPrimary)
            if (useSSL) {
                Text(
                    "(端口通常为 8883)",
                    style = AppTypography.Caption,
                    color = AppColors.TextSecondary
                )
            }
        }

        // 客户端和服务端ID
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = clientId,
                onValueChange = onClientIdChange,
                label = { Text("客户端 ID") },
                placeholder = { Text("workhub-client") },
                supportingText = {
                    Text("MQTT客户端的唯一标识", style = MaterialTheme.typography.bodySmall)
                },
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = serverId,
                onValueChange = onServerIdChange,
                label = { Text("服务端 ID") },
                placeholder = { Text("hublink-server") },
                supportingText = {
                    Text("MQTT服务端的唯一标识", style = MaterialTheme.typography.bodySmall)
                },
                modifier = Modifier.weight(1f)
            )
        }

        // 认证设置
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = useAuth,
                onCheckedChange = onUseAuthChange
            )
            Text("启用认证", style = AppTypography.BodyMedium, color = AppColors.TextPrimary)
        }

        // 认证表单 (条件显示)
        AnimatedVisibility(
            visible = useAuth,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("用户名") },
                    placeholder = { Text("MQTT用户名") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = { Text("MQTT密码") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
