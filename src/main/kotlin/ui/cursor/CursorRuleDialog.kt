package ui.cursor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import data.CursorPlatform
import data.CursorRuleData
import service.CursorRuleService
import theme.*

/**
 * 平台选择Chip
 */
@Composable
private fun PlatformChip(
    platform: CursorPlatform,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onToggle),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else AppColors.BackgroundSecondary,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) AppColors.Primary else AppColors.BackgroundTertiary
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = CursorRuleService.getPlatformDisplayName(platform),
                style = AppTypography.Caption,
                color = if (isSelected) AppColors.Primary else AppColors.TextSecondary
            )
        }
    }
}

/**
 * Cursor规则对话框
 */
@Composable
fun CursorRuleDialog(
    rule: CursorRuleData? = null,
    onDismiss: () -> Unit,
    onConfirm: (CursorRuleData) -> Unit,
    currentUserId: String = ""
) {
    var name by remember { mutableStateOf(rule?.name ?: "") }
    var selectedPlatforms by remember { mutableStateOf(rule?.platforms ?: emptyList<CursorPlatform>()) }
    var content by remember { mutableStateOf(rule?.getCurrentContent() ?: "") }
    var isShared by remember { mutableStateOf(rule?.isShared ?: false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var contentError by remember { mutableStateOf<String?>(null) }

    val isEditing = rule != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(800.dp)
                .height(700.dp),
            shape = RoundedCornerShape(AppDimensions.RadiusL),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.PaddingL)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "编辑Cursor规则" else "添加Cursor规则",
                        style = AppTypography.TitleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 表单内容
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // 规则名称
                    Text(
                        text = "规则名称",
                        style = AppTypography.BodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(bottom = AppDimensions.SpaceS)
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入规则名称", style = AppTypography.BodySmall) },
                        isError = nameError != null,
                        singleLine = true
                    )

                    nameError?.let {
                        Text(
                            text = it,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                    // 适用平台
                    Text(
                        text = "适用平台",
                        style = AppTypography.BodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(bottom = AppDimensions.SpaceS)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CursorPlatform.entries.forEach { platform ->
                            PlatformChip(
                                platform = platform,
                                isSelected = platform in selectedPlatforms,
                                onToggle = {
                                    selectedPlatforms = if (platform in selectedPlatforms) {
                                        selectedPlatforms - platform
                                    } else {
                                        selectedPlatforms + platform
                                    }
                                }
                            )
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
                        text = "（共享后其他用户可以查看此规则）",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                    // 规则内容
                    Text(
                        text = "规则内容 (Markdown格式)",
                        style = AppTypography.BodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.padding(bottom = AppDimensions.SpaceS)
                    )

                    OutlinedTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            contentError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        placeholder = { Text("输入Markdown格式的规则内容", style = AppTypography.BodySmall) },
                        isError = contentError != null,
                        maxLines = Int.MAX_VALUE
                    )

                    contentError?.let {
                        Text(
                            text = it,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text("取消", style = AppTypography.Caption)
                    }

                    Spacer(modifier = Modifier.width(AppDimensions.SpaceM))

                    Button(
                        onClick = {
                            // 验证输入
                            var hasError = false

                            if (name.trim().isEmpty()) {
                                nameError = "规则名称不能为空"
                                hasError = true
                            } else if (CursorRuleService.isRuleNameExists(name.trim(), rule?.id)) {
                                nameError = "规则名称已存在"
                                hasError = true
                            }

                            if (selectedPlatforms.isEmpty()) {
                                // 这里可以显示平台选择的错误，但暂时不处理
                            }

                            if (content.trim().isEmpty()) {
                                contentError = "规则内容不能为空"
                                hasError = true
                            }

                            if (!hasError) {
                                val updatedRule = rule?.copy(
                                    name = name.trim(),
                                    platforms = selectedPlatforms,
                                    versions = rule.versions.map {
                                        if (it.version == rule.currentVersion) {
                                            it.copy(content = content.trim())
                                        } else {
                                            it
                                        }
                                    },
                                    lastModified = System.currentTimeMillis(),
                                    isShared = isShared
                                ) ?: CursorRuleData(
                                    name = name.trim(),
                                    platforms = selectedPlatforms,
                                    versions = listOf(
                                        data.CursorRuleVersion(1, content.trim(), System.currentTimeMillis())
                                    ),
                                    createdBy = currentUserId,
                                    isShared = isShared
                                )

                                onConfirm(updatedRule)
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(if (isEditing) "保存" else "创建", style = AppTypography.Caption)
                    }
                }
            }
        }
    }
}


