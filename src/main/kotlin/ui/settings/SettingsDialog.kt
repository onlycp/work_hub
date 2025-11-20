package ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import data.UserSettings
import data.UserSettingsManager
import theme.*

enum class SettingsTab(
    val title: String,
    val icon: ImageVector
) {
    GENERAL("通用", Icons.Default.Settings),
    AI("AI助手", Icons.Default.Star)
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    var settings by remember { mutableStateOf(UserSettingsManager.getCurrentSettings()) }
    var selectedTab by remember { mutableStateOf(SettingsTab.GENERAL) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(900.dp)
                .height(650.dp),
            shape = RoundedCornerShape(12.dp),
            color = AppColors.Background,
            elevation = 8.dp
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // 左侧边栏
                SettingsSidebar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                // 右侧内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // 标题栏
                    SettingsHeader(
                        title = selectedTab.title,
                        onClose = onDismiss
                    )

                    Divider(color = AppColors.Divider)

                    // 内容区域
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (selectedTab) {
                            SettingsTab.GENERAL -> GeneralSettingsContent(
                                settings = settings,
                                onSettingsChange = { settings = it }
                            )
                            SettingsTab.AI -> AISettingsContent(
                                settings = settings,
                                onSettingsChange = { settings = it }
                            )
                        }
                    }

                    Divider(color = AppColors.Divider)

                    // 底部按钮
                    SettingsFooter(
                        onCancel = onDismiss,
                        onSave = {
                            UserSettingsManager.updateSettings(settings)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSidebar(
    selectedTab: SettingsTab,
    onTabSelected: (SettingsTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight(),
        color = AppColors.SurfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = AppDimensions.SpaceL)
        ) {
            SettingsTab.values().forEach { tab ->
                SettingsSidebarItem(
                    tab = tab,
                    isSelected = tab == selectedTab,
                    onClick = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
fun SettingsSidebarItem(
    tab: SettingsTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.SpaceS)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected)
                AppColors.Primary.copy(alpha = 0.1f)
            else
                Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            modifier = Modifier.size(18.dp),
            tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
        Text(
            text = tab.title,
            fontSize = AppTypography.BodySmall.fontSize,
            color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun SettingsHeader(
    title: String,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
                .padding(AppDimensions.SpaceM),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = AppTypography.TitleMedium.fontSize,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "关闭",
                modifier = Modifier.size(16.dp),
                tint = AppColors.TextSecondary
            )
        }
    }
}

@Composable
fun SettingsFooter(
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
                .padding(AppDimensions.SpaceM),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(
            onClick = onCancel,
            colors = ButtonDefaults.textButtonColors(
                contentColor = AppColors.TextSecondary
            )
        ) {
            Text("取消", style = AppTypography.BodySmall)
        }

        Spacer(modifier = Modifier.width(AppDimensions.SpaceS))

        Button(
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = AppColors.Primary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("保存", style = AppTypography.BodySmall)
        }
    }
}

@Composable
fun GeneralSettingsContent(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
                .padding(AppDimensions.SpaceL)
    ) {
        // 基础设置
        SettingSection(title = "基础设置") {
            // 自动保存
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自动保存",
                        fontSize = AppTypography.BodyLarge.fontSize,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "编辑时自动保存到本地",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }
                Switch(
                    checked = settings.autoSave,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(autoSave = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.Primary,
                        checkedTrackColor = AppColors.Primary.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            // 退出时自动同步
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "退出时自动同步",
                        fontSize = AppTypography.BodyLarge.fontSize,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "退出应用时自动同步数据到Git仓库",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }
                Switch(
                    checked = settings.autoSyncOnExit,
                    onCheckedChange = {
                        onSettingsChange(settings.copy(autoSyncOnExit = it))
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AppColors.Primary,
                        checkedTrackColor = AppColors.Primary.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun AppearanceSettingsContent(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
                .padding(AppDimensions.SpaceL)
    ) {
        // 界面字体设置
        FontSettingSection(
            title = "界面字体",
            description = "应用于按钮、标签、菜单等界面元素",
            fontSettings = settings.uiFontSettings,
            onFontSettingsChange = {
                onSettingsChange(settings.copy(uiFontSettings = it))
            }
        )
    }
}

@Composable
fun AISettingsContent(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
                .padding(AppDimensions.SpaceL)
    ) {
        SettingSection(title = "API配置") {
            SettingTextField(
                label = "API地址",
                value = settings.aiSettings.apiUrl,
                onValueChange = {
                    onSettingsChange(settings.copy(aiSettings = settings.aiSettings.copy(apiUrl = it)))
                }
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            SettingTextField(
                label = "API密钥",
                value = settings.aiSettings.apiKey,
                onValueChange = {
                    onSettingsChange(settings.copy(aiSettings = settings.aiSettings.copy(apiKey = it)))
                },
                isPassword = true
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            SettingTextField(
                label = "模型名称",
                value = settings.aiSettings.model,
                onValueChange = {
                    onSettingsChange(settings.copy(aiSettings = settings.aiSettings.copy(model = it)))
                }
            )
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceXL))

        Text(
            text = "AI助手可以根据日报自动生成周报",
            style = AppTypography.Caption,
            color = AppColors.TextSecondary
        )
    }
}

@Composable
fun GitSettingsContent(
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit
) {
    val scope = rememberCoroutineScope()
    var testingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
                .padding(AppDimensions.SpaceL)
    ) {
        SettingSection(title = "Git同步配置") {
            Text(
                text = "Git同步已启用，用于同步数据到远程仓库",
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            Text(
                text = "同步的数据包括：工作报告、应用设置",
                style = AppTypography.Caption,
                color = AppColors.TextSecondary
            )
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = AppTypography.TitleMedium.fontSize,
            fontWeight = FontWeight.Medium,
            color = AppColors.TextPrimary
        )

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        content()
    }
}

@Composable
fun SettingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = AppTypography.BodyMedium,
            color = AppColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = AppColors.TextPrimary,
                backgroundColor = AppColors.Surface,
                focusedBorderColor = AppColors.Primary,
                unfocusedBorderColor = AppColors.Border
            ),
            shape = RoundedCornerShape(4.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}

@Composable
fun SearchableDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    placeholder: String = "请选择..."
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // 过滤选项
    val filteredOptions = remember(searchQuery, options) {
        if (searchQuery.isBlank()) {
            options
        } else {
            options.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    // 当输入框获得焦点时展开下拉菜单
    LaunchedEffect(expanded) {
        if (expanded) {
            focusRequester.requestFocus()
        }
    }

    Column {
        Text(
            text = label,
            style = AppTypography.BodyMedium,
            color = AppColors.TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedTextField(
                value = if (expanded) searchQuery else selectedValue,
                onValueChange = { query ->
                    searchQuery = query
                    if (!expanded) expanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(placeholder, style = AppTypography.BodyMedium) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = AppColors.TextPrimary,
                    backgroundColor = AppColors.Surface,
                    focusedBorderColor = AppColors.Primary,
                    unfocusedBorderColor = AppColors.Border
                ),
                shape = RoundedCornerShape(4.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                trailingIcon = {
                    Row {
                        if (selectedValue.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    onValueChange("")
                                    searchQuery = ""
                                },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除",
                                    tint = AppColors.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "选择",
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                if (filteredOptions.isEmpty() && searchQuery.isNotBlank()) {
                    DropdownMenuItem(onClick = {}) {
                        Text(
                            text = "未找到匹配项",
                            style = AppTypography.BodySmall,
                            color = AppColors.TextDisabled
                        )
                    }
                } else {
                    filteredOptions.forEach { option ->
                        DropdownMenuItem(
                            onClick = {
                                onValueChange(option)
                                searchQuery = ""
                                expanded = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    fontSize = AppTypography.BodyLarge.fontSize,
                                    color = if (option == selectedValue) AppColors.Primary else AppColors.TextPrimary
                                )
                                if (option == selectedValue) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "已选中",
                                        modifier = Modifier.size(16.dp),
                                        tint = AppColors.Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FontSettingSection(
    title: String,
    description: String,
    fontSettings: data.FontSettings,
    onFontSettingsChange: (data.FontSettings) -> Unit
) {
    // 获取系统所有可用字体
    val allSystemFonts = remember {
        try {
            val ge = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
            if (ge.isHeadlessInstance) {
                listOf("System", "Monospace", "Serif", "SansSerif")
            } else {
                ge.availableFontFamilyNames.toList().sorted()
            }
        } catch (e: Exception) {
            println("获取系统字体失败: ${e.message}")
            e.printStackTrace()
            listOf("System", "Monospace", "Serif", "SansSerif")
        }
    }

    // 常用字体放在前面
    val fontFamilies = remember {
        val common = listOf(
            "System",
            "Monospace",
            "JetBrains Mono",
            "Fira Code",
            "Source Code Pro",
            "Monaco",
            "Consolas",
            "Arial",
            "Helvetica",
            "PingFang SC",
            "Microsoft YaHei",
            "SimSun"
        ).filter { it in allSystemFonts || it in listOf("System", "Monospace", "Serif", "SansSerif") }

        val others = allSystemFonts.filter { it !in common }
        (common + others).distinct()
    }

    SettingSection(title = title) {
        Text(
            text = description,
            style = AppTypography.Caption,
            color = AppColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 字体家族选择
        SearchableDropdown(
            label = "字体家族",
            selectedValue = fontSettings.family,
            options = fontFamilies,
            onValueChange = { onFontSettingsChange(fontSettings.copy(family = it)) },
            placeholder = "选择字体..."
        )

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 字体大小
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "字体大小",
                fontSize = AppTypography.BodySmall.fontSize,
                color = AppColors.TextSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (fontSettings.size > 8) {
                            onFontSettingsChange(fontSettings.copy(size = fontSettings.size - 1))
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "减小",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${fontSettings.size}pt",
                    fontSize = AppTypography.BodyLarge.fontSize,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.width(55.dp),
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = {
                        if (fontSettings.size < 32) {
                            onFontSettingsChange(fontSettings.copy(size = fontSettings.size + 1))
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "增大",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 行高
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "行高",
                fontSize = AppTypography.BodySmall.fontSize,
                color = AppColors.TextSecondary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (fontSettings.lineHeight > 1.0f) {
                            onFontSettingsChange(fontSettings.copy(lineHeight = (fontSettings.lineHeight - 0.1f).coerceAtLeast(1.0f)))
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "减小",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = String.format("%.1f", fontSettings.lineHeight),
                    fontSize = AppTypography.BodyLarge.fontSize,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.width(55.dp),
                    fontWeight = FontWeight.Medium
                )

                IconButton(
                    onClick = {
                        if (fontSettings.lineHeight < 3.0f) {
                            onFontSettingsChange(fontSettings.copy(lineHeight = (fontSettings.lineHeight + 0.1f).coerceAtMost(3.0f)))
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "增大",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
