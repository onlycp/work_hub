package ui

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
import data.CommandRuleData
import kotlinx.coroutines.launch
import theme.AppColors
import java.util.UUID
import theme.AppDimensions
import theme.AppTypography

/**
 * 命令规则对话框
 */
@Composable
fun CommandDialog(
    initialRule: CommandRuleData? = null,
    onDismiss: () -> Unit,
    onSave: (CommandRuleData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val isEdit = initialRule != null

    // 表单状态
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var script by remember { mutableStateOf(initialRule?.script ?: "") }
    var logFile by remember { mutableStateOf(initialRule?.logFile ?: "") }
    var remarks by remember { mutableStateOf(initialRule?.remarks ?: "") }

    // 错误状态
    var nameError by remember { mutableStateOf<String?>(null) }
    var scriptError by remember { mutableStateOf<String?>(null) }

    // 验证输入
    fun validateInputs(): Boolean {
        var isValid = true

        // 验证名称
        if (name.trim().isEmpty()) {
            nameError = "请输入命令名称"
            isValid = false
        } else {
            nameError = null
        }

        // 验证脚本
        if (script.trim().isEmpty()) {
            scriptError = "请输入执行脚本"
            isValid = false
        } else {
            scriptError = null
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
                    .widthIn(min = 450.dp, max = 600.dp)
                    .wrapContentHeight()
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
                            text = if (isEdit) "编辑命令规则" else "添加命令规则",
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
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(AppDimensions.PaddingScreen)
                ) {
                    // 命令名称
                    OutlinedTextField(
                        value = name,
                        onValueChange = { value: String -> name = value },
                        label = { Text("命令名称", style = AppTypography.Caption) },
                        placeholder = { Text("例如: 重启服务", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError != null,
                        maxLines = 1
                    )

                    if (nameError != null) {
                        Text(
                            text = nameError!!,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                    // 执行脚本
                    OutlinedTextField(
                        value = script,
                        onValueChange = { script = it },
                        label = { Text("执行脚本", style = AppTypography.Caption) },
                        placeholder = { Text("例如: systemctl restart nginx", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = scriptError != null,
                        maxLines = 5,
                        minLines = 3
                    )

                    if (scriptError != null) {
                        Text(
                            text = scriptError!!,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                    // 日志文件路径
                    OutlinedTextField(
                        value = logFile,
                        onValueChange = { logFile = it },
                        label = { Text("日志文件路径", style = AppTypography.Caption) },
                        placeholder = { Text("例如: /var/log/nginx/error.log（可选）", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                    // 备注
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("备注", style = AppTypography.Caption) },
                        placeholder = { Text("可选，用于描述此命令的作用", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
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
                                    val rule = CommandRuleData(
                                        id = initialRule?.id ?: UUID.randomUUID().toString(),
                                        name = name.trim(),
                                        script = script.trim(),
                                        logFile = logFile.trim(),
                                        remarks = remarks.trim(),
                                        autoStart = false
                                    )
                                    onSave(rule)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            enabled = name.isNotBlank() && script.isNotBlank()
                        ) {
                            Text(if (isEdit) "保存" else "添加", style = AppTypography.BodySmall)
                        }
                    }
                }
            }
        }
    }
}
