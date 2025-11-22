package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import data.KeyManager
import data.SSHConfigData
import data.SSHConfigManager
import theme.*

/**
 * SSH配置对话框（新建/编辑）
 */
@Composable
fun SSHConfigDialog(
    initialConfig: SSHConfigData? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, host: String, port: String, username: String, password: String, privateKeyPath: String, privateKeyPassphrase: String, group: String, usePassword: Boolean, keyId: String?, isShared: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialConfig?.name ?: "") }
    var host by remember { mutableStateOf(initialConfig?.host ?: "") }
    var port by remember { mutableStateOf(initialConfig?.port?.toString() ?: "22") }
    var username by remember { mutableStateOf(initialConfig?.username ?: "") }
    var password by remember { mutableStateOf(initialConfig?.password ?: "") }
    var privateKeyPath by remember { mutableStateOf(initialConfig?.privateKeyPath ?: "") }
    var privateKeyPassphrase by remember { mutableStateOf(initialConfig?.privateKeyPassphrase ?: "") }
    var group by remember { mutableStateOf(initialConfig?.group ?: "默认分组") }
    var selectedKeyId by remember { mutableStateOf(initialConfig?.keyId) }
    var isShared by remember { mutableStateOf(initialConfig?.isShared ?: false) }

    // 认证方式：0=使用密钥, 1=密码认证, 2=私钥认证
    var authType by remember {
        mutableStateOf(
            when {
                selectedKeyId != null -> 0 // 使用密钥
                initialConfig?.password?.isNotEmpty() == true -> 1 // 密码认证
                else -> 2 // 私钥认证
            }
        )
    }

    val isEditing = initialConfig != null

    // 获取所有现有分组和密钥
    val existingGroups = remember { SSHConfigManager.getAllGroups() }
    val availableKeys by KeyManager.keys.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 450.dp, max = 600.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            elevation = AppDimensions.ElevationDialog
        ) {
            Column(
                modifier = Modifier
                    .padding(AppDimensions.SpaceL)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "编辑主机配置" else "新建主机配置",
                    style = AppTypography.TitleMedium,
                    modifier = Modifier.padding(bottom = AppDimensions.SpaceL)
                )

                // 基本信息 - 两列布局
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
                ) {
                    // 左侧列
                    Column(modifier = Modifier.weight(1f)) {
                        // 连接名称
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("连接名称", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextPrimary,
                                backgroundColor = AppColors.Surface
                            )
                        )

                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                        // 分组
                        OutlinedTextField(
                            value = group,
                            onValueChange = { group = it },
                            label = { Text("分组", style = AppTypography.BodySmall) },
                            placeholder = { Text("如: 生产环境、测试环境", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextPrimary,
                                backgroundColor = AppColors.Surface
                            )
                        )
                    }

                    // 右侧列
                    Column(modifier = Modifier.weight(1f)) {
                        // 主机地址
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("主机地址", style = AppTypography.BodySmall) },
                            placeholder = { Text("例如: 192.168.1.100", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextPrimary,
                                backgroundColor = AppColors.Surface
                            )
                        )

                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                        // 端口
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it },
                            label = { Text("端口", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextPrimary,
                                backgroundColor = AppColors.Surface
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                // 用户名和认证方式 - 两列布局
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
                ) {
                    // 左侧列 - 用户名
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("用户名", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextPrimary,
                                backgroundColor = AppColors.Surface
                            )
                        )
                    }

                    // 右侧列 - 认证方式选择
                    Column(modifier = Modifier.weight(1f)) {
                        // 认证方式下拉选择器
                        var authDropdownExpanded by remember { mutableStateOf(false) }
                        val authOptions = listOf(
                            "使用密钥" to 0,
                            "密码认证" to 1,
                            "私钥认证" to 2
                        )
                        val selectedAuthOption = authOptions.find { it.second == authType }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedAuthOption?.first ?: "选择认证方式",
                                onValueChange = {},
                                label = { Text("认证方式", style = AppTypography.BodySmall) },
                                modifier = Modifier.fillMaxWidth(),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { authDropdownExpanded = !authDropdownExpanded }) {
                                        androidx.compose.material.Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                            contentDescription = "展开"
                                        )
                                    }
                                },
                                textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = AppColors.TextPrimary,
                                    backgroundColor = AppColors.Surface
                                )
                            )

                            androidx.compose.material.DropdownMenu(
                                expanded = authDropdownExpanded,
                                onDismissRequest = { authDropdownExpanded = false },
                                modifier = Modifier.widthIn(min = 200.dp)
                            ) {
                                authOptions.forEach { (name, type) ->
                                    androidx.compose.material.DropdownMenuItem(
                                        onClick = {
                                            authType = type
                                            selectedKeyId = if (type == 0) availableKeys.firstOrNull()?.id else null
                                            authDropdownExpanded = false
                                        }
                                    ) {
                                        Text(
                                            text = name,
                                            style = AppTypography.BodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                // 根据认证方式显示不同的输入字段
                when (authType) {
                    0 -> {
                        // 使用密钥
                        if (availableKeys.isEmpty()) {
                            Text(
                                text = "暂无可用的密钥，请先在密钥管理中添加",
                                style = AppTypography.Caption,
                                color = AppColors.Error,
                                modifier = Modifier.padding(vertical = AppDimensions.SpaceS)
                            )
                        } else {
                            // 密钥选择下拉框
                            var expanded by remember { mutableStateOf(false) }
                            val selectedKey = availableKeys.find { it.id == selectedKeyId }

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = selectedKey?.name ?: "请选择密钥",
                                    onValueChange = {},
                                    label = { Text("选择密钥", style = AppTypography.BodySmall) },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { expanded = !expanded }) {
                                            androidx.compose.material.Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                                                contentDescription = "展开"
                                            )
                                        }
                                    },
                                    textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                                    colors = TextFieldDefaults.outlinedTextFieldColors(
                                        textColor = AppColors.TextPrimary,
                                        backgroundColor = AppColors.Surface
                                    )
                                )

                                androidx.compose.material.DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.widthIn(min = 200.dp)
                                ) {
                                    availableKeys.forEach { key ->
                                        androidx.compose.material.DropdownMenuItem(
                                            onClick = {
                                                selectedKeyId = key.id
                                                expanded = false
                                            }
                                        ) {
                                            Text(
                                                text = "${key.name} (${key.username})",
                                                style = AppTypography.BodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // 密码认证 - 显示密码字段
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = AppColors.TextPrimary,
                                backgroundColor = AppColors.Surface
                            )
                        )
                    }

                    2 -> {
                        // 私钥认证 - 显示私钥字段
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // 私钥路径
                            OutlinedTextField(
                                value = privateKeyPath,
                                onValueChange = { privateKeyPath = it },
                                label = { Text("私钥文件路径（可选）", style = AppTypography.BodySmall) },
                                placeholder = { Text("留空自动使用默认私钥", style = AppTypography.BodySmall) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = AppColors.TextPrimary,
                                    backgroundColor = AppColors.Surface
                                )
                            )

                            Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                            // 私钥密码
                            OutlinedTextField(
                                value = privateKeyPassphrase,
                                onValueChange = { privateKeyPassphrase = it },
                                label = { Text("私钥密码（可选）", style = AppTypography.BodySmall) },
                                placeholder = { Text("如果私钥有密码保护，请输入密码", style = AppTypography.BodySmall) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = AppColors.TextPrimary,
                                    backgroundColor = AppColors.Surface
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "提示: 留空将自动尝试使用 ~/.ssh/id_ed25519 或 ~/.ssh/id_rsa",
                                style = AppTypography.Caption,
                                modifier = Modifier.padding(start = AppDimensions.SpaceS)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                // 共享选项
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material.Checkbox(
                        checked = isShared,
                        onCheckedChange = { isShared = it }
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text(
                        text = "共享给其他用户",
                        style = AppTypography.BodySmall,
                        color = AppColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text(
                        text = "（共享后其他用户可以查看此主机配置）",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", style = AppTypography.BodySmall)
                    }

                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))

                    Button(
                        onClick = {
                            val isValid = when (authType) {
                                0 -> name.isNotBlank() && host.isNotBlank() && selectedKeyId != null // 使用密钥
                                1 -> name.isNotBlank() && host.isNotBlank() && username.isNotBlank() // 密码认证
                                2 -> name.isNotBlank() && host.isNotBlank() && username.isNotBlank() // 私钥认证
                                else -> false
                            }

                            if (isValid) {
                                onSave(name, host, port, username, password, privateKeyPath, privateKeyPassphrase, group.ifBlank { "默认分组" }, authType == 1, selectedKeyId, isShared)
                            }
                        },
                        enabled = when (authType) {
                            0 -> name.isNotBlank() && host.isNotBlank() && selectedKeyId != null
                            1 -> name.isNotBlank() && host.isNotBlank() && username.isNotBlank()
                            2 -> name.isNotBlank() && host.isNotBlank() && username.isNotBlank()
                            else -> false
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AppColors.Primary
                        )
                    ) {
                        Text(
                            text = if (isEditing) "保存" else "创建",
                            style = AppTypography.BodySmall
                        )
                    }
                }
            }
        }
    }
}
