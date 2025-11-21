package ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import data.*
import kotlinx.coroutines.launch
import theme.*

/**
 * 仓库设置对话框
 * 用于配置GIT仓库连接信息
 */
@Composable
fun RepositorySettingsDialog(
    onDismiss: () -> Unit
) {
    var settings by remember { mutableStateOf(RepositorySettingsManager.getCurrentSettings()) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var savingStatus by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(600.dp)
                .height(500.dp),
            shape = MaterialTheme.shapes.medium,
            elevation = AppDimensions.ElevationDialog
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.SpaceL)
            ) {
                // 标题
                Text(
                    text = "仓库设置",
                    style = AppTypography.TitleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = AppDimensions.SpaceL)
                )

                // 内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                        // 仓库地址
                        OutlinedTextField(
                            value = settings.repositoryUrl,
                            onValueChange = { settings = settings.copy(repositoryUrl = it) },
                            label = { Text("仓库地址", style = AppTypography.BodySmall) },
                            placeholder = { Text("https://github.com/username/workhub-data.git", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = "仓库地址",
                                    tint = AppColors.TextSecondary
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                        // 用户名
                        OutlinedTextField(
                            value = settings.username,
                            onValueChange = { settings = settings.copy(username = it) },
                            label = { Text("用户名", style = AppTypography.BodySmall) },
                            placeholder = { Text("GitHub用户名", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "用户名",
                                    tint = AppColors.TextSecondary
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                        // 密码/Token
                        OutlinedTextField(
                            value = settings.password,
                            onValueChange = { settings = settings.copy(password = it) },
                            label = { Text("密码或Token", style = AppTypography.BodySmall) },
                            placeholder = { Text("GitHub密码或Personal Access Token", style = AppTypography.BodySmall) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(fontSize = 14.sp),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "密码",
                                    tint = AppColors.TextSecondary
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPassword = !showPassword },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                        tint = AppColors.TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                        // 测试连接按钮
                        Button(
                            onClick = {
                                scope.launch {
                                    isTestingConnection = true
                                    testResult = null

                                    try {
                                        val result = GitDataManager.testRepositoryConnection(
                                            repositoryUrl = settings.repositoryUrl,
                                            username = settings.username.takeIf { it.isNotBlank() },
                                            password = settings.password.takeIf { it.isNotBlank() }
                                        )

                                        testResult = if (result.isSuccess) {
                                            result.getOrNull() ?: "连接成功"
                                        } else {
                                            "连接失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"
                                        }
                                    } catch (e: Exception) {
                                        testResult = "连接失败: ${e.message}"
                                    } finally {
                                        isTestingConnection = false
                                    }
                                }
                            },
                            enabled = !isTestingConnection && settings.repositoryUrl.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = AppColors.Success
                            ),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                                Text("测试中...", style = AppTypography.BodySmall)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "测试连接",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                                Text("测试连接", style = AppTypography.BodySmall)
                            }
                        }

                        // 测试结果
                        testResult?.let { result ->
                            Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                            Text(
                                text = result,
                                style = AppTypography.Caption,
                                color = if (result.contains("失败")) AppColors.Error else AppColors.Success
                            )
                        }

                        Spacer(modifier = Modifier.height(AppDimensions.SpaceXL))

                        Text(
                            text = "设置完成后，应用会自动拉取所有分支并按成员姓名创建对应的本地目录。",
                            style = AppTypography.Caption,
                            color = AppColors.TextSecondary
                        )

                        // 保存状态提示
                        savingStatus?.let { status ->
                            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AppColors.Primary
                                    )
                                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                                }
                                Text(
                                    text = status,
                                    style = AppTypography.BodySmall,
                                    color = if (status.contains("失败") || status.contains("⚠️")) AppColors.Warning else AppColors.TextPrimary
                                )
                            }
                        }
                }

                Divider(modifier = Modifier.padding(vertical = AppDimensions.SpaceM))

                // 底部按钮
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
                            scope.launch {
                                isSaving = true
                                savingStatus = "正在保存设置..."
                                try {
                                    // 保存设置
                                    val updatedSettings = settings.copy(enabled = true)
                                    RepositorySettingsManager.updateSettings(updatedSettings)

                                    savingStatus = "正在连接远程仓库..."
                                    // 执行初始化动作：发现并同步所有远程用户分支
                                    val discoverResult = GitDataManager.discoverAndSyncUserBranches()
                                    if (discoverResult.isSuccess) {
                                        savingStatus = "✅ 仓库设置完成，已同步所有用户分支"

                                        // 强制刷新所有管理器的数据（如果有已登录用户）
                                        val currentUser = CurrentUserManager.getCurrentUserId()
                                        if (currentUser.isNotEmpty()) {
                                            SSHConfigManager.setCurrentUser(currentUser)
                                            KeyManager.setCurrentUser(currentUser)
                                            CursorRuleManager.setCurrentUser(currentUser)
                                            MemberManager.setCurrentUser(currentUser)
                                            ExpenseManager.setCurrentUser(currentUser)
                                        }
                                    } else {
                                        savingStatus = "⚠️ 设置已保存，但同步失败: ${discoverResult.exceptionOrNull()?.message}"
                                        // 不影响设置保存，即使同步失败
                                    }

                                    // 短暂延迟让用户看到结果
                                    kotlinx.coroutines.delay(1500)
                                    onDismiss()
                                } finally {
                                    isSaving = false
                                    savingStatus = null
                                }
                            }
                        },
                        enabled = !isSaving && settings.repositoryUrl.isNotBlank() && settings.username.isNotBlank() && settings.password.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AppColors.Primary
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                            Text("保存中...", style = AppTypography.BodySmall)
                        } else {
                            Text("保存", style = AppTypography.BodySmall)
                        }
                    }
                }
            }
        }
    }
}
