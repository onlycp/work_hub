package ui.profile

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
import data.CurrentUserManager
import data.MemberData
import data.MemberManager
import kotlinx.coroutines.launch
import theme.*

/**
 * 个人信息编辑对话框
 */
@Composable
fun ProfileEditDialog(
    onDismiss: () -> Unit,
    onConfirm: (MemberData) -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentUserId = CurrentUserManager.getCurrentUserId()
    val currentMember = remember { MemberManager.getMemberById(currentUserId) }

    var name by remember { mutableStateOf(currentMember?.name ?: "") }
    var introduction by remember { mutableStateOf(currentMember?.introduction ?: "") }

    var nameError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(AppDimensions.RadiusL),
            elevation = 8.dp,
            modifier = Modifier
                .width(500.dp)
                .heightIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(AppDimensions.PaddingL)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "编辑个人信息",
                        style = AppTypography.BodyLarge,
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

                // 内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
                ) {
                    // 姓名
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = { Text("姓名", style = AppTypography.Caption) },
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

                    // 用户ID（只读）
                    OutlinedTextField(
                        value = currentUserId,
                        onValueChange = {},
                        label = { Text("用户ID", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        textStyle = AppTypography.BodySmall,
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = AppColors.TextSecondary,
                            backgroundColor = AppColors.BackgroundSecondary
                        )
                    )

                    // 个人介绍
                    OutlinedTextField(
                        value = introduction,
                        onValueChange = { introduction = it },
                        label = { Text("个人介绍（可选）", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        textStyle = AppTypography.BodySmall
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

                            if (name.trim().isEmpty()) {
                                nameError = "姓名不能为空"
                                hasError = true
                            }

                            if (!hasError) {
                                // 更新成员信息
                                currentMember?.let { member ->
                                    val updatedMember = member.copy(
                                        name = name.trim(),
                                        introduction = introduction.trim(),
                                        lastModified = System.currentTimeMillis()
                                    )
                                    onConfirm(updatedMember)
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("保存", style = AppTypography.Caption)
                    }
                }
            }
        }
    }
}
