package ui.ops

import data.FileInfo
import service.SFTPFileManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import theme.*
import java.io.File

// 重命名对话框
@Composable
fun RenameDialog(
    file: FileInfo,
    sftpManager: SFTPFileManager,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var newName by remember { mutableStateOf(file.name) }
    var isRenaming by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = AppColors.Surface,
            elevation = AppDimensions.ElevationMedium
        ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .padding(AppDimensions.SpaceL)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "重命名",
                        style = AppTypography.TitleMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                Divider(color = AppColors.Divider)
                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                // 输入框
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("新名称", style = AppTypography.BodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRenaming,
                    isError = errorMessage != null,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = AppColors.TextPrimary,
                        backgroundColor = AppColors.Surface,
                        focusedBorderColor = AppColors.Primary,
                        unfocusedBorderColor = AppColors.Divider
                    )
                )

                // 错误提示
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                    Text(
                        text = errorMessage!!,
                        style = AppTypography.Caption,
                        color = AppColors.Error
                    )
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isRenaming
                    ) {
                        Text("取消", style = AppTypography.BodyLarge)
                    }

                    Spacer(modifier = Modifier.width(AppDimensions.SpaceM))

                    Button(
                        onClick = {
                            scope.launch {
                                isRenaming = true
                                errorMessage = null

                                val parentPath = File(file.path).parent ?: ""
                                val newPath = if (parentPath.isEmpty()) newName else "$parentPath/$newName"

                                sftpManager.renameFile(file.path, newPath).fold(
                                    onSuccess = {
                                        println("✓ 文件重命名成功: ${file.name} -> $newName")
                                        onSuccess()
                                    },
                                    onFailure = { error ->
                                        errorMessage = error.message
                                        isRenaming = false
                                    }
                                )
                            }
                        },
                        enabled = !isRenaming && newName.isNotBlank() && newName != file.name,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AppColors.Primary
                        )
                    ) {
                        if (isRenaming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isRenaming) "重命名中..." else "确定",
                            color = androidx.compose.ui.graphics.Color.White,
                            style = AppTypography.BodyLarge
                        )
                    }
                }
            }
        }
    }
}
