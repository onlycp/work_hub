package ui.ops

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.AuthType
import data.KeyData
import data.KeyManager
import theme.*
import java.util.*

/**
 * 密钥对话框
 */
@Composable
fun KeyDialog(
    key: KeyData?,
    onDismiss: () -> Unit,
    onConfirm: (KeyData) -> Unit,
    currentUserId: String = ""
) {
    var name by remember { mutableStateOf(key?.name ?: "") }
    var username by remember { mutableStateOf(key?.username ?: "") }
    var authType by remember { mutableStateOf(key?.authType ?: AuthType.PASSWORD) }
    var password by remember { mutableStateOf(key?.password ?: "") }
    var privateKeyContent by remember { mutableStateOf(key?.privateKeyContent ?: "") }
    var privateKeyPassphrase by remember { mutableStateOf(key?.privateKeyPassphrase ?: "") }

    var showPassword by remember { mutableStateOf(false) }
    var showPrivateKeyPassword by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var privateKeyError by remember { mutableStateOf<String?>(null) }
    var isShared by remember { mutableStateOf(key?.isShared ?: false) }

    val isEditing = key != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.RadiusL),
            elevation = 8.dp,
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 600.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.PaddingL)
            ) {
                // 标题
                Text(
                    text = if (isEditing) "编辑密钥" else "添加密钥",
                    style = AppTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
                ) {
                    // 密钥名称
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = { Text("密钥名称", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError != null,
                        textStyle = AppTypography.BodySmall
                    )
                    if (nameError != null) {
                        Text(
                            text = nameError!!,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 4.dp)
                        )
                    }

                    // 用户名
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = null
                        },
                        label = { Text("用户名", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = usernameError != null,
                        textStyle = AppTypography.BodySmall
                    )
                    if (usernameError != null) {
                        Text(
                            text = usernameError!!,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 4.dp)
                        )
                    }

                    // 认证类型选择
                    Column {
                        Text(
                            text = "认证类型",
                            style = AppTypography.BodySmall,
                            color = AppColors.TextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
                        ) {
                            AuthType.values().forEach { type ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = authType == type,
                                        onClick = { authType = type }
                                    )
                                    Text(
                                        text = when (type) {
                                            AuthType.PASSWORD -> "密码认证"
                                            AuthType.KEY -> "密钥认证"
                                        },
                                        style = AppTypography.BodySmall,
                                        color = AppColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // 根据认证类型显示不同字段
                    when (authType) {
                        AuthType.PASSWORD -> {
                            // 密码输入
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    passwordError = null
                                },
                                label = { Text("密码", style = AppTypography.Caption) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                isError = passwordError != null,
                                textStyle = AppTypography.BodySmall
                            )
                            if (passwordError != null) {
                                Text(
                                    text = passwordError!!,
                                    style = AppTypography.Caption,
                                    color = AppColors.Error,
                                    modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 4.dp)
                                )
                            }
                        }

                        AuthType.KEY -> {
                            // 私钥内容
                            OutlinedTextField(
                                value = privateKeyContent,
                                onValueChange = {
                                    privateKeyContent = it
                                    privateKeyError = null
                                },
                                label = { Text("私钥内容", style = AppTypography.Caption) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                maxLines = 8,
                                isError = privateKeyError != null,
                                textStyle = AppTypography.BodySmall
                            )
                            if (privateKeyError != null) {
                                Text(
                                    text = privateKeyError!!,
                                    style = AppTypography.Caption,
                                    color = AppColors.Error,
                                    modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 4.dp)
                                )
                            }

                            // 私钥密码
                            OutlinedTextField(
                                value = privateKeyPassphrase,
                                onValueChange = { privateKeyPassphrase = it },
                                label = { Text("私钥密码（可选）", style = AppTypography.Caption) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (showPrivateKeyPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { showPrivateKeyPassword = !showPrivateKeyPassword }) {
                                        Icon(
                                            imageVector = if (showPrivateKeyPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (showPrivateKeyPassword) "隐藏密码" else "显示密码",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                },
                                textStyle = AppTypography.BodySmall
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
                    Checkbox(
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
                        text = "（共享后其他用户可以查看此密钥）",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("取消", style = AppTypography.Caption)
                    }

                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))

                    Button(
                        onClick = {
                            // 验证输入
                            var hasError = false

                            if (name.isBlank()) {
                                nameError = "密钥名称不能为空"
                                hasError = true
                            } else if (KeyManager.isKeyNameExists(name, if (isEditing) key?.id else null)) {
                                nameError = "密钥名称已存在"
                                hasError = true
                            }

                            if (username.isBlank()) {
                                usernameError = "用户名不能为空"
                                hasError = true
                            }

                            when (authType) {
                                AuthType.PASSWORD -> {
                                    if (password.isBlank()) {
                                        passwordError = "密码不能为空"
                                        hasError = true
                                    }
                                }
                                AuthType.KEY -> {
                                    if (privateKeyContent.isBlank()) {
                                        privateKeyError = "私钥内容不能为空"
                                        hasError = true
                                    }
                                }
                            }

                            if (!hasError) {
                                val keyData = KeyData(
                                    id = key?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    username = username.trim(),
                                    authType = authType,
                                    password = password,
                                    privateKeyContent = privateKeyContent,
                                    privateKeyPassphrase = privateKeyPassphrase,
                                    createdAt = key?.createdAt ?: System.currentTimeMillis(),
                                    lastModified = System.currentTimeMillis(),
                                    createdBy = currentUserId,
                                    isShared = isShared
                                )
                                onConfirm(keyData)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(if (isEditing) "保存" else "添加", style = AppTypography.Caption)
                    }
                }
            }
        }
    }
}
