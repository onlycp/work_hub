package ui.members

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
import data.MemberData
import data.MemberManager
import theme.*
import java.util.*

/**
 * 成员对话框
 */
@Composable
fun MemberDialog(
    member: MemberData?,
    onDismiss: () -> Unit,
    onConfirm: (MemberData) -> Unit,
    currentUserId: String = ""
) {
    var name by remember { mutableStateOf(member?.name ?: "") }
    var password by remember { mutableStateOf(member?.password ?: "") }
    var number by remember { mutableStateOf(member?.number ?: "") }
    var introduction by remember { mutableStateOf(member?.introduction ?: "") }

    var showPassword by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var numberError by remember { mutableStateOf<String?>(null) }

    val isEditing = member != null

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
                    text = if (isEditing) "编辑成员" else "添加成员",
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

                    // 编号
                    OutlinedTextField(
                        value = number,
                        onValueChange = {
                            number = it
                            numberError = null
                        },
                        label = { Text("编号", style = AppTypography.Caption) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = numberError != null,
                        textStyle = AppTypography.BodySmall
                    )
                    if (numberError != null) {
                        Text(
                            text = numberError!!,
                            style = AppTypography.Caption,
                            color = AppColors.Error,
                            modifier = Modifier.padding(start = AppDimensions.SpaceS, top = 4.dp)
                        )
                    }

                    // 密码
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
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
                        textStyle = AppTypography.BodySmall
                    )

                    // 个人介绍
                    OutlinedTextField(
                        value = introduction,
                        onValueChange = { introduction = it },
                        label = { Text("个人介绍", style = AppTypography.Caption) },
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

                            if (name.isBlank()) {
                                nameError = "姓名不能为空"
                                hasError = true
                            } else if (MemberManager.isMemberNameExists(name, if (isEditing) member?.id else null)) {
                                nameError = "姓名已存在"
                                hasError = true
                            }

                            if (number.isBlank()) {
                                numberError = "编号不能为空"
                                hasError = true
                            } else if (MemberManager.isMemberNumberExists(number, if (isEditing) member?.id else null)) {
                                numberError = "编号已存在"
                                hasError = true
                            }

                            if (!hasError) {
                                val memberData = MemberData(
                                    id = member?.id ?: UUID.randomUUID().toString(),
                                    name = name.trim(),
                                    password = password,
                                    number = number.trim(),
                                    introduction = introduction.trim(),
                                    createdAt = member?.createdAt ?: System.currentTimeMillis(),
                                    lastModified = System.currentTimeMillis(),
                                    createdBy = currentUserId
                                )
                                onConfirm(memberData)
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

