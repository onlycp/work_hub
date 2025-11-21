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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.CurrentUserManager
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
    System.out.println("MembersContent组件被创建")
    val scope = rememberCoroutineScope()
    val members by MemberManager.members.collectAsState()
    val currentUserId = CurrentUserManager.getCurrentUserId()
    System.out.println("当前成员数量: ${members.size}")
    var showMemberDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<MemberData?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<MemberData?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // 日期格式化器
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 过滤后的成员列表
    val filteredMembers = remember(members, searchQuery) {
        if (searchQuery.isBlank()) {
            members
        } else {
            members.filter { member ->
                member.name.contains(searchQuery, ignoreCase = true) ||
                member.number.contains(searchQuery, ignoreCase = true) ||
                member.introduction.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimensions.PaddingScreen)
    ) {

        // 操作栏
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "成员列表 (${filteredMembers.size}${if (searchQuery.isNotBlank()) "/${members.size}" else ""})",
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

            Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

            // 搜索框
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "搜索成员（姓名、编号、个人介绍等）",
                            style = AppTypography.BodySmall,
                            color = AppColors.TextDisabled
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = AppColors.TextDisabled,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = if (searchQuery.isNotBlank()) {
                        {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除搜索",
                                    tint = AppColors.TextDisabled,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .weight(1f)
                        .height(AppDimensions.InputHeightSmall),
                    textStyle = TextStyle(fontSize = 14.sp),
                    singleLine = true,
                    shape = RoundedCornerShape(AppDimensions.CornerSmall)
                )
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 成员表格
        Card(
            modifier = Modifier.weight(1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(AppDimensions.RadiusL)
        ) {
            if (filteredMembers.isEmpty()) {
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
                        items(filteredMembers) { member ->
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
        // 在lambda外部捕获editingMember的值，避免在lambda中捕获可变状态
        val isEditing = editingMember != null
        
        MemberDialog(
            member = editingMember,
            onDismiss = {
                showMemberDialog = false
                editingMember = null
            },
            onConfirm = { member ->
                System.out.println("onConfirm回调被调用，成员: ${member.name}")
                scope.launch {
                    try {
                        System.out.println("开始${if (isEditing) "更新" else "添加"}成员: ${member.name}")

                        val result = if (isEditing) {
                            MemberManager.updateMember(member)
                        } else {
                            MemberManager.addMember(member)
                        }

                        System.out.println("成员操作结果: ${result.isSuccess}")
                        if (result.isSuccess) {
                            System.out.println("设置UI状态: showMemberDialog = false, editingMember = null")
                            onStatusMessage("✅ 成员${if (isEditing) "更新" else "添加"}成功")
                            // 强制重组UI状态
                            showMemberDialog = false
                            editingMember = null
                            System.out.println("成员操作成功完成")
                        } else {
                            val errorMsg = "❌ 成员操作失败: ${result.exceptionOrNull()?.message}"
                            onStatusMessage(errorMsg)
                            System.out.println(errorMsg)
                        }
                    } catch (e: Exception) {
                        val errorMsg = "❌ 成员操作异常: ${e.message}"
                        onStatusMessage(errorMsg)
                        System.out.println(errorMsg)
                        e.printStackTrace()
                    }
                }
            },
            currentUserId = currentUserId
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




