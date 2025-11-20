package ui.ops

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import data.*
import theme.*

/**
 * SSH配置列表视图（分组显示）
 */
@Composable
fun SSHConfigListView(
    configs: List<SSHConfigData>,
    selectedConfig: String?,
    currentUserId: String,
    onConfigSelected: (String) -> Unit,
    onConfigEdit: (String) -> Unit,
    onConfigDelete: (String) -> Unit,
    onConfigConnect: (String) -> Unit,
    onConfigShare: (String) -> Unit,
    onAddNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 按分组组织配置
    val groupedConfigs = remember(configs) {
        configs.groupBy { it.group }.toSortedMap()
    }

    // 展开/收起状态
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    // 当分组变化时，确保新分组默认展开
    LaunchedEffect(groupedConfigs.keys) {
        groupedConfigs.keys.forEach { group ->
            if (!expandedGroups.containsKey(group)) {
                expandedGroups[group] = true
            }
        }
    }

    Surface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Surface,
                elevation = AppDimensions.ElevationXS
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceS),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "主机管理 (${configs.size})",
                        style = AppTypography.BodyMedium,
                        color = AppColors.TextPrimary
                    )

                    IconButton(
                        onClick = onAddNew,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加配置",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // 配置列表（分组显示）
            if (configs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(AppDimensions.SpaceL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = AppColors.TextDisabled
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                        Text(
                            text = "暂无主机配置",
                            style = AppTypography.BodySmall,
                            color = AppColors.TextDisabled
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = AppDimensions.SpaceXS)
                ) {
                    groupedConfigs.forEach { (group, groupConfigs) ->
                        // 分组标题
                        item(key = "group_$group") {
                            GroupHeader(
                                groupName = group,
                                count = groupConfigs.size,
                                isExpanded = expandedGroups[group] ?: true,
                                onToggle = {
                                    expandedGroups[group] = !(expandedGroups[group] ?: true)
                                }
                            )
                        }

                        // 分组下的配置项
                        if (expandedGroups[group] == true) {
                            items(groupConfigs, key = { it.id }) { config ->
                                SSHConfigItem(
                                    config = config,
                                    isSelected = selectedConfig == config.id,
                                    currentUserId = currentUserId,
                                    onSelected = { onConfigSelected(config.id) },
                                    onEdit = { onConfigEdit(config.id) },
                                    onDelete = { onConfigDelete(config.id) },
                                    onConnect = { onConfigConnect(config.id) },
                                    onShare = { onConfigShare(config.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 分组标题
 */
@Composable
fun GroupHeader(
    groupName: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        color = AppColors.SurfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.SpaceM, vertical = AppDimensions.SpaceXS),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "收起" else "展开",
                modifier = Modifier.size(16.dp),
                tint = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(AppDimensions.SpaceXS))
            Text(
                text = groupName,
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(AppDimensions.SpaceXS))
            Text(
                text = "($count)",
                style = AppTypography.Caption,
                color = AppColors.TextDisabled
            )
        }
    }
}

/**
 * SSH配置项（简化版：只显示名称和IP）
 */
@Composable
fun SSHConfigItem(
    config: SSHConfigData,
    isSelected: Boolean,
    currentUserId: String,
    onSelected: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConnect: () -> Unit,
    onShare: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    // 权限检查
    val canEdit = PermissionManager.canEdit(config.baseData)
    val canDelete = PermissionManager.canDelete(config.baseData)
    val canShare = PermissionManager.canShare(config.shareableData)
    val isShared = config.isShared
    val isOwner = config.createdBy == currentUserId

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimensions.SpaceXS, vertical = 2.dp)
            .clickable(
                onClick = { onSelected() }
            ),
        color = if (isSelected)
            AppColors.Primary.copy(alpha = 0.08f)
        else
            Color.Transparent,
        shape = RoundedCornerShape(AppDimensions.CornerSmall)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.SpaceS, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 状态圆点（小）
            Surface(
                modifier = Modifier.size(6.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (isSelected) AppColors.Primary else AppColors.TextDisabled
            ) {}

            Spacer(modifier = Modifier.width(AppDimensions.SpaceS))

            // 配置信息
            Column(modifier = Modifier.weight(1f)) {
                // 第一行：名称和主机
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = config.name,
                        style = AppTypography.BodySmall,
                        color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceXS))
                    Text(
                        text = config.host,
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // 第二行：共享状态和创建者信息
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isShared) {
                        // 共享指示器
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "已共享",
                            tint = AppColors.Success,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isOwner) "已共享" else "共享的",
                            style = AppTypography.Caption,
                            color = AppColors.Success
                        )
                    }

                    if (!isOwner) {
                        Spacer(modifier = Modifier.width(AppDimensions.SpaceXS))
                        Text(
                            text = "来自 ${config.createdBy}",
                            style = AppTypography.Caption,
                            color = AppColors.TextDisabled,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 菜单按钮（小）
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // 连接选项（所有用户都可以连接）
                    DropdownMenuItem(
                        onClick = {
                            showMenu = false
                            onConnect()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "连接",
                            modifier = Modifier.size(14.dp),
                            tint = AppColors.Success
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("连接", style = AppTypography.BodySmall)
                    }

                    // 编辑选项（只有创建者能编辑）
                    if (canEdit) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("编辑", style = AppTypography.BodySmall)
                        }
                    }

                    // 共享选项（只有创建者能控制共享）
                    if (canShare) {
                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onShare()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowForward,
                                contentDescription = if (isShared) "取消共享" else "共享",
                                modifier = Modifier.size(14.dp),
                                tint = AppColors.Primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isShared) "取消共享" else "共享", style = AppTypography.BodySmall)
                        }
                    }

                    // 删除选项（只有创建者能删除）
                    if (canDelete) {
                        Divider(modifier = Modifier.padding(vertical = 2.dp))

                        DropdownMenuItem(
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = AppColors.Error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("删除", style = AppTypography.BodySmall, color = AppColors.Error)
                        }
                    }
                }
            }
        }
    }
}
