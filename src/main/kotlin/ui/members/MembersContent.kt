package ui.members

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.MemberData
import data.MemberManager
import kotlinx.coroutines.launch
import theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 成员表格行
 */
@Composable
private fun MemberTableRow(
    member: MemberData,
    dateFormatter: SimpleDateFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 姓名
            Text(
                text = member.name,
                modifier = Modifier.weight(1.5f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 编号
            Text(
                text = member.number,
                modifier = Modifier.weight(1f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 个人介绍
            Text(
                text = member.introduction.takeIf { it.isNotBlank() } ?: "暂无介绍",
                modifier = Modifier.weight(2f),
                style = AppTypography.BodySmall,
                color = if (member.introduction.isBlank()) AppColors.TextDisabled else AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 创建时间
            Text(
                text = dateFormatter.format(Date(member.createdAt)),
                modifier = Modifier.weight(1.2f),
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 操作按钮
            Row(
                modifier = Modifier.width(120.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = AppColors.Error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 成员管理主界面
 */
@Composable
fun MembersContent(
    onStatusMessage: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val members by MemberManager.members.collectAsState()
    var showMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<MemberData?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<MemberData?>(null) }

    // 日期格式化器
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimensions.PaddingScreen)
    ) {
        // 页面标题
        Text(
            text = "成员管理",
            style = AppTypography.TitleLarge,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = AppDimensions.SpaceL)
        )

        // 操作栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "成员列表 (${members.size})",
                style = AppTypography.BodyLarge,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )

            Button(
                onClick = {
                    editingMember = null
                    showMemberDialog = true
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加成员",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加成员", style = AppTypography.Caption)
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 成员表格
        Card(
            modifier = Modifier.weight(1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(AppDimensions.RadiusL)
        ) {
            if (members.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimensions.PaddingL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = AppColors.TextDisabled
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                        Text(
                            text = "暂无成员配置",
                            style = AppTypography.BodyLarge,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                        Text(
                            text = "添加成员以便管理团队信息",
                            style = AppTypography.Caption,
                            color = AppColors.TextDisabled
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 表头
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.BackgroundSecondary,
                        elevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "姓名",
                                modifier = Modifier.weight(1.5f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "编号",
                                modifier = Modifier.weight(1f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "个人介绍",
                                modifier = Modifier.weight(2f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "创建时间",
                                modifier = Modifier.weight(1.2f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "操作",
                                modifier = Modifier.width(120.dp),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    // 表格内容
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(members) { member ->
                            MemberTableRow(
                                member = member,
                                dateFormatter = dateFormatter,
                                onEdit = {
                                    editingMember = member
                                    showMemberDialog = true
                                },
                                onDelete = {
                                    showDeleteConfirmDialog = member
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 成员对话框
    if (showMemberDialog) {
        MemberDialog(
            member = editingMember,
            onDismiss = {
                showMemberDialog = false
                editingMember = null
            },
            onConfirm = { member ->
                scope.launch {
                    val result = if (editingMember != null) {
                        MemberManager.updateMember(member)
                    } else {
                        MemberManager.addMember(member)
                    }

                    if (result.isSuccess) {
                        onStatusMessage("✅ 成员${if (editingMember != null) "更新" else "添加"}成功")
                        showMemberDialog = false
                        editingMember = null
                    } else {
                        onStatusMessage("❌ 成员操作失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
        )
    }

    // 删除确认对话框
    showDeleteConfirmDialog?.let { member ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = {
                Text(
                    text = "确认删除",
                    style = AppTypography.BodyLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "确定要删除成员 \"${member.name}\" 吗？此操作不可撤销。",
                    style = AppTypography.BodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = MemberManager.deleteMember(member.id)
                            if (result.isSuccess) {
                                onStatusMessage("✅ 成员删除成功")
                            } else {
                                onStatusMessage("❌ 成员删除失败: ${result.exceptionOrNull()?.message}")
                            }
                            showDeleteConfirmDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppColors.Error,
                        contentColor = Color.White
                    )
                ) {
                    Text("删除", style = AppTypography.Caption)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("取消", style = AppTypography.Caption)
                }
            }
        )
    }
}




