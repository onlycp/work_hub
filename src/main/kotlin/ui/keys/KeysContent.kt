package ui.keys

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
import data.AuthType
import data.BaseData
import data.KeyData
import data.KeyManager
import data.PermissionManager
import kotlinx.coroutines.launch
import theme.*
import ui.ops.KeyDialog
import java.text.SimpleDateFormat
import java.util.*

/**
 * 密钥表格行
 */
@Composable
private fun KeyTableRow(
    key: KeyData,
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
            // 名称
            Text(
                text = key.name,
                modifier = Modifier.weight(1.5f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 用户名
            Text(
                text = key.username,
                modifier = Modifier.weight(1f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 认证类型
            Row(
                modifier = Modifier.weight(0.8f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = when (key.authType) {
                        AuthType.PASSWORD -> AppColors.Success.copy(alpha = 0.1f)
                        AuthType.KEY -> AppColors.Primary.copy(alpha = 0.1f)
                    },
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Text(
                        text = when (key.authType) {
                            AuthType.PASSWORD -> "密码"
                            AuthType.KEY -> "密钥"
                        },
                        style = AppTypography.Caption,
                        color = when (key.authType) {
                            AuthType.PASSWORD -> AppColors.Success
                            AuthType.KEY -> AppColors.Primary
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 共享状态
            Row(
                modifier = Modifier.weight(0.8f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (key.isShared) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.Success.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "共享",
                            style = AppTypography.Caption,
                            color = AppColors.Success,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = AppColors.TextDisabled.copy(alpha = 0.1f),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            text = "私有",
                            style = AppTypography.Caption,
                            color = AppColors.TextDisabled,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 所属人
            Text(
                text = if (key.createdBy == data.CurrentUserManager.getCurrentUserId()) {
                    "我创建的"
                } else {
                    "来自 ${key.createdBy}"
                },
                modifier = Modifier.weight(1f),
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 创建时间
            Text(
                text = dateFormatter.format(Date(key.createdAt)),
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
                // 编辑按钮 - 只有创建者才能显示
                if (PermissionManager.canEdit(with(key) {
                    object : data.BaseData {
                        override val id: String = this@with.id
                        override val createdBy: String = this@with.createdBy
                        override val createdAt: Long = this@with.createdAt
                        override val lastModified: Long = this@with.lastModified
                    }
                })) {
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
                } else {
                    // 占位符，保持布局一致
                    Spacer(modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 删除按钮 - 只有创建者才能显示
                if (PermissionManager.canDelete(with(key) {
                    object : data.BaseData {
                        override val id: String = this@with.id
                        override val createdBy: String = this@with.createdBy
                        override val createdAt: Long = this@with.createdAt
                        override val lastModified: Long = this@with.lastModified
                    }
                })) {
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
                } else {
                    // 占位符，保持布局一致
                    Spacer(modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

/**
 * 密钥管理主界面
 */
@Composable
fun KeysContent(
    onStatusMessage: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val keys by KeyManager.keys.collectAsState()
    var showKeyDialog by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<KeyData?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<KeyData?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // 日期格式化器
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 过滤后的密钥列表
    val filteredKeys = remember(keys, searchQuery) {
        if (searchQuery.isBlank()) {
            keys
        } else {
            keys.filter { key ->
                key.name.contains(searchQuery, ignoreCase = true) ||
                key.username.contains(searchQuery, ignoreCase = true) ||
                key.createdBy.contains(searchQuery, ignoreCase = true) ||
                when (key.authType) {
                    AuthType.PASSWORD -> "密码".contains(searchQuery, ignoreCase = true)
                    AuthType.KEY -> "密钥".contains(searchQuery, ignoreCase = true)
                } ||
                (key.isShared && "共享".contains(searchQuery, ignoreCase = true)) ||
                (!key.isShared && "私有".contains(searchQuery, ignoreCase = true))
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
                    text = "密钥列表 (${filteredKeys.size}${if (searchQuery.isNotBlank()) "/${keys.size}" else ""})",
                    style = AppTypography.BodyLarge,
                    color = AppColors.TextPrimary,
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = {
                        editingKey = null
                        showKeyDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加密钥",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加密钥", style = AppTypography.Caption)
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
                            text = "搜索密钥（名称、用户名、认证类型、共享状态等）",
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

        // 密钥表格
        Card(
            modifier = Modifier.weight(1f),
            elevation = 4.dp,
            shape = RoundedCornerShape(AppDimensions.RadiusL)
        ) {
            if (filteredKeys.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimensions.PaddingL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = AppColors.TextDisabled
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                        Text(
                            text = "暂无密钥配置",
                            style = AppTypography.BodyLarge,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                        Text(
                            text = "添加密钥以便在主机配置中重复使用",
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
                                text = "名称",
                                modifier = Modifier.weight(1.5f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "用户名",
                                modifier = Modifier.weight(1f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "认证类型",
                                modifier = Modifier.weight(0.8f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "共享状态",
                                modifier = Modifier.weight(0.8f),
                                style = AppTypography.BodySmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary
                            )
                            Text(
                                text = "所属人",
                                modifier = Modifier.weight(1f),
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
                        items(filteredKeys) { key ->
                            KeyTableRow(
                                key = key,
                                dateFormatter = dateFormatter,
                                onEdit = {
                                    editingKey = key
                                    showKeyDialog = true
                                },
                                onDelete = {
                                    showDeleteConfirmDialog = key
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 密钥对话框
    if (showKeyDialog) {
        KeyDialog(
            key = editingKey,
            onDismiss = {
                showKeyDialog = false
                editingKey = null
            },
            onConfirm = { key ->
                scope.launch {
                    val result = if (editingKey != null) {
                        KeyManager.updateKey(key)
                    } else {
                        KeyManager.addKey(key)
                    }

                    if (result.isSuccess) {
                        onStatusMessage("✅ 密钥${if (editingKey != null) "更新" else "添加"}成功")
                        showKeyDialog = false
                        editingKey = null
                    } else {
                        onStatusMessage("❌ 密钥操作失败: ${result.exceptionOrNull()?.message}")
                    }
                }
            }
        )
    }

    // 删除确认对话框
    showDeleteConfirmDialog?.let { key ->
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
                    text = "确定要删除密钥 \"${key.name}\" 吗？此操作不可撤销。",
                    style = AppTypography.BodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = KeyManager.deleteKey(key.id)
                            if (result.isSuccess) {
                                onStatusMessage("✅ 密钥删除成功")
                            } else {
                                onStatusMessage("❌ 密钥删除失败: ${result.exceptionOrNull()?.message}")
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
