package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import data.AppInitializer
import data.LoginSettingsManager
import data.RepositorySettingsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import theme.*

/**
 * 用户登录对话框
 * 现代化设计的登录界面
 */
@Composable
fun UserLoginDialog(
    onLoginSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var userName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRepoSettings by remember { mutableStateOf(false) }
    var loginSuccess by remember { mutableStateOf(false) }

    // 登录选项状态
    var rememberPassword by remember { mutableStateOf(false) }
    var autoLogin by remember { mutableStateOf(false) }

    // 初始化时加载记住的登录信息
    LaunchedEffect(Unit) {
        val loginSettings = LoginSettingsManager.getCurrentSettings()
        if (loginSettings.rememberPassword && loginSettings.rememberedUsername.isNotBlank()) {
            userName = loginSettings.rememberedUsername
            password = loginSettings.rememberedPassword
            rememberPassword = true
            autoLogin = loginSettings.autoLogin
        }
    }

    val scope = rememberCoroutineScope()

    // 注意：rememberCoroutineScope() 创建的 scope 不应该被手动取消
    // 它会随着 Composable 的生命周期自动管理

    // 检查仓库是否已配置
    val isRepositoryConfigured by remember { derivedStateOf { RepositorySettingsManager.isRepositoryConfigured() } }

    // 登录成功动画效果
    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            delay(1200) // 增加延迟时间，确保数据加载完成
            onLoginSuccess()
        }
    }

    // 全窗口遮挡层
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE3F2FD).copy(alpha = 0.9f),
                        Color(0xFFF3E5F5).copy(alpha = 0.9f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 背景装饰
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 400f
                    )
                )
        )

        // 主卡片
        Card(
            modifier = Modifier
                .width(360.dp)
                .wrapContentHeight()
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = Color.Black.copy(alpha = 0.25f)
                ),
            shape = RoundedCornerShape(20.dp),
            backgroundColor = Color.White.copy(alpha = 0.95f),
            elevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 顶部设置按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { showRepoSettings = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "仓库设置",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 应用图标和标题
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF90CAF9),
                                    Color(0xFFCE93D8)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = "WorkHub",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "欢迎使用 WorkHub",
                    style = AppTypography.TitleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1A1A1A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "您的效率工作台",
                    style = AppTypography.BodyMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (!isRepositoryConfigured) {
                    // 仓库未配置时的提示界面
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "需要配置",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "需要先配置仓库",
                            style = AppTypography.TitleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF1A1A1A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "请点击右上角设置按钮配置GIT仓库信息，配置完成后才能登录。",
                            style = AppTypography.BodyMedium.copy(
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // 仓库已配置时的登录表单
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 用户名输入框
                        OutlinedTextField(
                            value = userName,
                            onValueChange = { userName = it },
                            label = {
                                Text(
                                    "用户名",
                                    style = AppTypography.BodySmall.copy(fontSize = 12.sp)
                                )
                            },
                            placeholder = {
                                Text(
                                    "输入您的用户名",
                                    style = AppTypography.BodySmall.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF999999)
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(
                                fontSize = 14.sp,
                                color = Color(0xFF1A1A1A),
                                lineHeight = 18.sp
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "用户名",
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = Color(0xFFF8F9FA),
                                focusedBorderColor = Color(0xFF90CAF9),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                cursorColor = Color(0xFF90CAF9)
                            )
                        )


                        // 密码输入框
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = {
                                Text(
                                    "密码",
                                    style = AppTypography.BodySmall.copy(fontSize = 12.sp)
                                )
                            },
                            placeholder = {
                                Text(
                                    "输入您的密码",
                                    style = AppTypography.BodySmall.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF999999)
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            singleLine = true,
                            textStyle = AppTypography.BodySmall.copy(
                                fontSize = 14.sp,
                                color = Color(0xFF1A1A1A),
                                lineHeight = 18.sp
                            ),
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "密码",
                                    tint = Color(0xFF666666),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showPassword = !showPassword },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = Color(0xFFF8F9FA),
                                focusedBorderColor = Color(0xFF90CAF9),
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                cursorColor = Color(0xFF90CAF9)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 登录选项
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 记住密码
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = rememberPassword,
                                    onCheckedChange = {
                                        rememberPassword = it
                                        if (!it) {
                                            autoLogin = false // 取消记住密码时自动取消自动登录
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF90CAF9),
                                        uncheckedColor = Color(0xFFCCCCCC)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "记住密码",
                                    style = AppTypography.BodySmall.copy(
                                        fontSize = 12.sp,
                                        color = Color(0xFF666666)
                                    )
                                )
                            }

                            // 自动登录
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = autoLogin && rememberPassword,
                                    onCheckedChange = { autoLogin = it },
                                    enabled = rememberPassword,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF90CAF9),
                                        uncheckedColor = Color(0xFFCCCCCC)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "自动登录",
                                    style = AppTypography.BodySmall.copy(
                                        fontSize = 12.sp,
                                        color = if (rememberPassword) Color(0xFF666666) else Color(0xFFCCCCCC)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 错误信息
                        errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                backgroundColor = Color(0xFFFFEBEE),
                                shape = RoundedCornerShape(8.dp),
                                elevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "错误",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        style = AppTypography.BodySmall.copy(
                                            fontSize = 12.sp,
                                            color = Color(0xFFD32F2F)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 登录按钮
                        Button(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    errorMessage = null

                                    try {
                                        if (userName.isBlank()) {
                                            errorMessage = "请输入用户名"
                                            return@launch
                                        }

                                        if (password.isBlank()) {
                                            errorMessage = "请输入密码"
                                            return@launch
                                        }

                                        // 模拟登录过程
                                        delay(1000) // 添加一点延迟让动画更自然

                                        val result = AppInitializer.loginUser(userName)
                                        if (result.isSuccess) {
                                            try {
                                                // 登录成功，保存登录设置（同步执行，确保完成后再设置成功状态）
                                                LoginSettingsManager.saveLoginSettings(
                                                    username = userName,
                                                    password = password,
                                                    rememberPassword = rememberPassword,
                                                    autoLogin = autoLogin
                                                )
                                                loginSuccess = true
                                            } catch (e: Exception) {
                                                println("保存登录设置失败: ${e.message}")
                                                // 保存设置失败不影响登录，继续设置成功状态
                                                loginSuccess = true
                                            }
                                        } else {
                                            errorMessage = result.exceptionOrNull()?.message ?: "登录失败"
                                        }
                                    } catch (e: Exception) {
                                        println("登录过程异常: ${e.message}")
                                        errorMessage = "登录失败: ${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            enabled = !isLoading && userName.isNotBlank() && password.isNotBlank() && !loginSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color.Transparent,
                                disabledBackgroundColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (!isLoading && !loginSuccess) {
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF90CAF9),
                                                    Color(0xFFCE93D8)
                                                )
                                            )
                                        } else if (loginSuccess) {
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFF4CAF50),
                                                    Color(0xFF66BB6A)
                                                )
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color(0xFFCCCCCC),
                                                    Color(0xFFAAAAAA)
                                                )
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "登录中...",
                                            style = AppTypography.BodySmall.copy(
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        )
                                    }
                                } else if (loginSuccess) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "成功",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            "登录成功",
                                            style = AppTypography.BodySmall.copy(
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        )
                                    }
                                } else {
                                    Text(
                                        "登录",
                                        style = AppTypography.BodySmall.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 仓库设置对话框（显示在最顶层）
        if (showRepoSettings) {
            RepositorySettingsDialog(
                onDismiss = { showRepoSettings = false }
            )
        }
    }
}