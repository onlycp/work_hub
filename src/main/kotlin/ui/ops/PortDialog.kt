package ui.ops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.PortForwardingRuleData
import data.PortManager
import kotlinx.coroutines.launch
import theme.AppColors
import theme.AppDimensions
import theme.AppTypography

/**
 * 端口转发规则对话框
 */
@Composable
fun PortDialog(
    initialRule: PortForwardingRuleData? = null,
    onDismiss: () -> Unit,
    onSave: (PortForwardingRuleData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEdit = initialRule != null

    // 表单状态
    var ruleType by remember { mutableStateOf(initialRule?.type ?: "LOCAL") }
    var localPort by remember { mutableStateOf(initialRule?.localPort?.toString() ?: "") }
    var remoteHost by remember { mutableStateOf(initialRule?.remoteHost ?: "localhost") }
    var remotePort by remember { mutableStateOf(initialRule?.remotePort?.toString() ?: "") }
    var description by remember { mutableStateOf(initialRule?.description ?: "") }
    var autoStart by remember { mutableStateOf(initialRule?.autoStart ?: true) }

    // 错误状态
    var localPortError by remember { mutableStateOf<String?>(null) }
    var remotePortError by remember { mutableStateOf<String?>(null) }

    // 验证输入
    fun validateInputs(): Boolean {
        var isValid = true

        // 验证本地端口
        val localPortNum = localPort.toIntOrNull()
        if (localPortNum == null || localPortNum !in 1..65535) {
            localPortError = "请输入有效的端口号 (1-65535)"
            isValid = false
        } else {
            localPortError = null
        }

        // 验证远程端口
        val remotePortNum = remotePort.toIntOrNull()
        if (remotePortNum == null || remotePortNum !in 1..65535) {
            remotePortError = "请输入有效的端口号 (1-65535)"
            isValid = false
        } else {
            remotePortError = null
        }

        return isValid
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.RadiusL),
            color = AppColors.BackgroundPrimary,
            elevation = AppDimensions.ElevationDialog
        ) {
            Column(
                modifier = Modifier
                    .width(500.dp)
                    .heightIn(min = 600.dp, max = 700.dp)
            ) {
                // 对话框标题栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Surface,
                    elevation = AppDimensions.ElevationXS
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEdit) "编辑端口转发规则" else "添加端口转发规则",
                            style = AppTypography.TitleMedium,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 表单内容
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(AppDimensions.PaddingScreen)
                ) {
                    // 转发类型（固定为本地转发）
                    Text(
                        text = "转发类型：本地转发",
                        style = AppTypography.BodyMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = AppDimensions.SpaceS)
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                    // 本地端口
                    OutlinedTextField(
                        value = localPort,
                        onValueChange = { value: String -> localPort = value },
                        label = { Text("本地端口", style = AppTypography.Caption) },
                        placeholder = { Text("例如: 8080", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = localPortError != null
                    )

                    if (localPortError != null) {
                        Text(
                            text = localPortError!!,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                    // 远程主机和端口
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                    ) {
                        OutlinedTextField(
                            value = remoteHost,
                            onValueChange = { remoteHost = it },
                            label = { Text("远程主机", style = AppTypography.Caption) },
                            placeholder = { Text("localhost", style = AppTypography.Caption) },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = remotePort,
                            onValueChange = { remotePort = it },
                            label = { Text("远程端口", style = AppTypography.Caption) },
                            placeholder = { Text("80", style = AppTypography.Caption) },
                            modifier = Modifier.weight(1f),
                            isError = remotePortError != null
                        )
                    }

                    if (remotePortError != null) {
                        Text(
                            text = remotePortError!!,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                    // 规则描述
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("规则描述", style = AppTypography.Caption) },
                        placeholder = { Text("可选，用于标识规则用途", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                    // 自动启动选项
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = autoStart,
                            onCheckedChange = { autoStart = it }
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                        Text(
                            text = "自动启动",
                            style = AppTypography.BodySmall,
                            color = AppColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                        Text(
                            text = "（连接SSH后自动启动端口转发）",
                            style = AppTypography.Caption,
                            color = AppColors.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }

                // 底部按钮
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.Surface,
                    elevation = AppDimensions.ElevationXS
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("取消", style = AppTypography.BodySmall)
                        }

                        Spacer(modifier = Modifier.width(AppDimensions.SpaceS))

                        Button(
                            onClick = {
                                if (validateInputs()) {
                                    val rule = PortForwardingRuleData(
                                        id = initialRule?.id ?: "",
                                        type = ruleType,
                                        localPort = localPort.toInt(),
                                        remoteHost = remoteHost,
                                        remotePort = remotePort.toIntOrNull() ?: 0,
                                        description = description.trim(),
                                        autoStart = autoStart
                                    )
                                    onSave(rule)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            enabled = localPort.isNotBlank() && remoteHost.isNotBlank() && remotePort.isNotBlank()
                        ) {
                            Text(if (isEdit) "保存" else "添加", style = AppTypography.BodySmall)
                        }
                    }
                }
            }
        }
    }
}
