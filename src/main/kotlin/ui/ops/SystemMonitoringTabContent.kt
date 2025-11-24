package ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import data.SSHConfigData
import kotlinx.coroutines.*
import service.SSHSessionManager
import theme.*
import kotlin.math.roundToInt

// 排序枚举
enum class SortField {
    PID, CPU, MEMORY
}

enum class SortDirection {
    ASCENDING, DESCENDING
}

/**
 * 监控Tab类型
 */
enum class MonitoringTab(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    STATUS("状态", Icons.Default.Monitor),
    PROCESSES("进程", Icons.Default.List),
    SERVICES("服务", Icons.Default.Settings)
}

/**
 * 进程信息
 */
data class ProcessInfo(
    val pid: Int = 0,
    val name: String = "",
    val user: String = "",
    val cpu: Float = 0f,
    val memory: Float = 0f,
    val status: String = "",
    val startTime: String = "",
    val workingDir: String = "",
    val command: String = ""
)

/**
 * 系统进程列表
 */
data class ProcessList(
    val processes: List<ProcessInfo> = emptyList(),
    val totalCount: Int = 0,
    val lastUpdate: Long = 0L
)

/**
 * 系统服务信息
 */
data class ServiceInfo(
    val name: String = "",
    val status: String = "",
    val description: String = "",
    val isEnabled: Boolean = false,
    val isRunning: Boolean = false
)

/**
 * 系统服务列表
 */
data class ServiceList(
    val services: List<ServiceInfo> = emptyList(),
    val lastUpdate: Long = 0L
)

/**
 * 扩展的系统监控信息
 */
data class ExtendedSystemMetrics(
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f,
    val memoryUsed: Long = 0L,
    val memoryTotal: Long = 0L,
    val diskUsage: Float = 0f,
    val diskUsed: String = "0G",
    val diskTotal: String = "0G",
    val networkRx: String = "0KB/s",
    val networkTx: String = "0KB/s",
    val loadAverage: String = "0.00",
    val uptime: String = "0天",
    val processCount: Int = 0,
    val lastUpdate: Long = 0L
)

/**
 * 系统监控Tab内容
 */
@Composable
fun SystemMonitoringTabContent(
    config: SSHConfigData,
    isConnected: Boolean
) {
    val selectedTab = remember { mutableStateOf(MonitoringTab.STATUS) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Tab导航栏
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp,
            shape = RoundedCornerShape(AppDimensions.RadiusM)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
            ) {
                MonitoringTab.values().forEach { tab ->
                    TabButton(
                        tab = tab,
                        isSelected = selectedTab.value == tab,
                        onClick = { selectedTab.value = tab }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

        // Tab内容
        when (selectedTab.value) {
            MonitoringTab.STATUS -> {
                SystemStatusTab(config, isConnected)
            }
            MonitoringTab.PROCESSES -> {
                ProcessMonitoringTab(config, isConnected)
            }
            MonitoringTab.SERVICES -> {
                ServiceManagementTab(config, isConnected)
            }
        }
    }
}

/**
 * Tab按钮组件
 */
@Composable
private fun TabButton(
    tab: MonitoringTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        AppColors.Primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val textColor = if (isSelected) {
        AppColors.Primary
    } else {
        AppColors.TextSecondary
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimensions.RadiusS))
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = tab.displayName,
                style = AppTypography.BodyMedium,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * 系统状态Tab
 */
@Composable
private fun SystemStatusTab(
    config: SSHConfigData,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    val systemMetrics = remember { mutableStateOf(ExtendedSystemMetrics()) }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // 系统监控定时任务
    LaunchedEffect(isConnected) {
        if (isConnected) {
            isLoading.value = true
            errorMessage.value = null
            try {
                while (isConnected) {
                    val metrics = fetchSystemMetrics(config)
                    if (metrics != null) {
                        systemMetrics.value = metrics
                        errorMessage.value = null
                    } else {
                        errorMessage.value = "获取系统信息失败"
                    }
                    kotlinx.coroutines.delay(2000) // 每2秒更新一次
                }
            } catch (e: Exception) {
                errorMessage.value = "监控异常: ${e.message}"
                isLoading.value = false
            } finally {
                isLoading.value = false
            }
        } else {
            isLoading.value = false
            systemMetrics.value = ExtendedSystemMetrics()
            errorMessage.value = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 连接状态提示
        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(AppDimensions.RadiusM),
                backgroundColor = AppColors.Warning.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.PaddingM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "请先连接到主机以查看系统状态",
                        style = AppTypography.BodyMedium,
                        color = AppColors.Warning
                    )
                }
            }
        } else if (errorMessage.value != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(AppDimensions.RadiusM),
                backgroundColor = AppColors.Error.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.PaddingM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = errorMessage.value ?: "获取系统信息失败",
                        style = AppTypography.BodyMedium,
                        color = AppColors.Error
                    )
                }
            }
        } else {
            // 系统监控指标
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS),
                contentPadding = PaddingValues(AppDimensions.PaddingM)
            ) {
                // CPU监控
                item {
                    StatusMetricCard(
                        title = "CPU 使用率",
                        icon = Icons.Default.Memory,
                        value = "${systemMetrics.value.cpuUsage.roundToInt()}%",
                        subtitle = "负载: ${systemMetrics.value.loadAverage}",
                        progress = systemMetrics.value.cpuUsage / 100f,
                        color = when {
                            systemMetrics.value.cpuUsage >= 80 -> AppColors.Error
                            systemMetrics.value.cpuUsage >= 60 -> AppColors.Warning
                            else -> AppColors.Success
                        }
                    )
                }

                // 内存监控
                item {
                    StatusMetricCard(
                        title = "内存使用",
                        icon = Icons.Default.Storage,
                        value = "${systemMetrics.value.memoryUsage.roundToInt()}%",
                        subtitle = "${systemMetrics.value.memoryUsed}MB / ${systemMetrics.value.memoryTotal}MB",
                        progress = systemMetrics.value.memoryUsage / 100f,
                        color = when {
                            systemMetrics.value.memoryUsage >= 80 -> AppColors.Error
                            systemMetrics.value.memoryUsage >= 60 -> AppColors.Warning
                            else -> AppColors.Success
                        }
                    )
                }

                // 磁盘监控
                item {
                    StatusMetricCard(
                        title = "磁盘使用",
                        icon = Icons.Default.Storage,
                        value = "${systemMetrics.value.diskUsage.roundToInt()}%",
                        subtitle = "${systemMetrics.value.diskUsed} / ${systemMetrics.value.diskTotal}",
                        progress = systemMetrics.value.diskUsage / 100f,
                        color = when {
                            systemMetrics.value.diskUsage >= 90 -> AppColors.Error
                            systemMetrics.value.diskUsage >= 75 -> AppColors.Warning
                            else -> AppColors.Success
                        }
                    )
                }

                // 系统信息
                item {
                    SystemInfoStatusCard(
                        title = "系统信息",
                        icon = Icons.Default.Info,
                        uptime = systemMetrics.value.uptime,
                        processCount = systemMetrics.value.processCount
                    )
                }
            }
        }
    }
}

/**
 * 进程监控Tab
 */
@Composable
private fun ProcessMonitoringTab(
    config: SSHConfigData,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    val processList = remember { mutableStateOf(ProcessList()) }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // 排序状态
    val sortField = remember { mutableStateOf<SortField?>(null) }
    val sortDirection = remember { mutableStateOf(SortDirection.DESCENDING) }

    // 排序函数
    val updateSort: (SortField) -> Unit = { field ->
        if (sortField.value == field) {
            sortDirection.value = if (sortDirection.value == SortDirection.ASCENDING)
                SortDirection.DESCENDING else SortDirection.ASCENDING
        } else {
            sortField.value = field
            sortDirection.value = SortDirection.DESCENDING
        }
    }

    // 当排序状态改变时，重新排序当前进程列表
    LaunchedEffect(sortField.value, sortDirection.value) {
        if (processList.value.processes.isNotEmpty()) {
            val sortedProcesses = processList.value.copy(
                processes = sortProcesses(processList.value.processes, sortField.value, sortDirection.value)
            )
            processList.value = sortedProcesses
        }
    }

    // 进程监控定时任务
    LaunchedEffect(isConnected) {
        if (isConnected) {
            isLoading.value = true
            errorMessage.value = null
            try {
                while (isConnected) {
                    val processes = fetchProcessList(config)
                    if (processes != null) {
                        // 对进程列表进行排序
                        val sortedProcesses = processes.copy(
                            processes = sortProcesses(processes.processes, sortField.value, sortDirection.value)
                        )
                        processList.value = sortedProcesses
                        errorMessage.value = null
                    } else {
                        errorMessage.value = "获取进程列表失败"
                    }
                    kotlinx.coroutines.delay(3000) // 每3秒更新一次
                }
            } catch (e: Exception) {
                errorMessage.value = "监控异常: ${e.message}"
                isLoading.value = false
            } finally {
                isLoading.value = false
            }
        } else {
            isLoading.value = false
            processList.value = ProcessList()
            errorMessage.value = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 连接状态提示
        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(AppDimensions.RadiusM),
                backgroundColor = AppColors.Warning.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.PaddingM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "请先连接到主机以查看进程列表",
                        style = AppTypography.BodyMedium,
                        color = AppColors.Warning
                    )
                }
            }
        } else if (errorMessage.value != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(AppDimensions.RadiusM),
                backgroundColor = AppColors.Error.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.PaddingM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = errorMessage.value ?: "获取进程列表失败",
                        style = AppTypography.BodyMedium,
                        color = AppColors.Error
                    )
                }
            }
        } else {
            // 进程列表表格
            ProcessListTable(
                processes = processList.value.processes,
                sortField = sortField.value,
                sortDirection = sortDirection.value,
                onSortChange = updateSort,
                onKillProcess = { pid ->
                    scope.launch {
                        killProcess(config, pid)
                        // 重新获取进程列表
                        val updatedProcesses = fetchProcessList(config)
                        if (updatedProcesses != null) {
                            val sortedProcesses = updatedProcesses.copy(
                                processes = sortProcesses(updatedProcesses.processes, sortField.value, sortDirection.value)
                            )
                            processList.value = sortedProcesses
                        }
                    }
                }
            )
        }
    }
}

/**
 * 服务管理Tab
 */
@Composable
private fun ServiceManagementTab(
    config: SSHConfigData,
    isConnected: Boolean
) {
    val scope = rememberCoroutineScope()
    val serviceList = remember { mutableStateOf(ServiceList()) }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val statusResult = remember { mutableStateOf<String?>(null) }
    val showStatusDialog = remember { mutableStateOf(false) }

    // 服务监控定时任务
    LaunchedEffect(isConnected) {
        if (isConnected) {
            isLoading.value = true
            errorMessage.value = null
            try {
                while (isConnected) {
                    val services = fetchServiceList(config)
                    if (services != null) {
                        serviceList.value = services
                        errorMessage.value = null
                    } else {
                        errorMessage.value = "获取服务列表失败"
                    }
                    kotlinx.coroutines.delay(5000) // 每5秒更新一次
                }
            } catch (e: Exception) {
                errorMessage.value = "监控异常: ${e.message}"
                isLoading.value = false
            } finally {
                isLoading.value = false
            }
        } else {
            isLoading.value = false
            serviceList.value = ServiceList()
            errorMessage.value = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 连接状态提示
        if (!isConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(AppDimensions.RadiusM),
                backgroundColor = AppColors.Warning.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.PaddingM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "请先连接到主机以查看服务列表",
                        style = AppTypography.BodyMedium,
                        color = AppColors.Warning
                    )
                }
            }
        } else if (errorMessage.value != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 2.dp,
                shape = RoundedCornerShape(AppDimensions.RadiusM),
                backgroundColor = AppColors.Error.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimensions.PaddingM),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = AppColors.Error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = errorMessage.value ?: "获取服务列表失败",
                        style = AppTypography.BodyMedium,
                        color = AppColors.Error
                    )
                }
            }
        } else {
            // 服务列表
            ServiceListTable(
                services = serviceList.value.services,
                onServiceAction = { serviceName, action ->
                    scope.launch {
                        if (action == "status") {
                            // 对于状态查询，显示结果
                            val result = performServiceStatusCheck(config, serviceName)
                            statusResult.value = "服务 $serviceName 状态:\n\n$result"
                            showStatusDialog.value = true
                        } else {
                            val success = performServiceAction(config, serviceName, action)
                            if (success) {
                                // 重新获取服务列表
                                val updatedServices = fetchServiceList(config)
                                if (updatedServices != null) {
                                    serviceList.value = updatedServices
                                }
                            }
                        }
                    }
                }
            )
        }

        // 服务状态对话框
        if (showStatusDialog.value) {
            AlertDialog(
                onDismissRequest = { showStatusDialog.value = false },
                title = {
                    Text("服务状态", style = AppTypography.TitleMedium)
                },
                text = {
                    Text(
                        text = statusResult.value ?: "获取状态失败",
                        style = AppTypography.BodySmall,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showStatusDialog.value = false }
                    ) {
                        Text("确定")
                    }
                }
            )
        }
    }
}

/**
 * 状态指标卡片
 */
@Composable
private fun StatusMetricCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    subtitle: String,
    progress: Float,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(AppDimensions.RadiusM)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.PaddingM)
        ) {
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = AppTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            // 数值显示
            Text(
                text = value,
                style = AppTypography.TitleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))

            // 副标题
            Text(
                text = subtitle,
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceS))

            // 进度条
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = color,
                backgroundColor = AppColors.BackgroundSecondary
            )
        }
    }
}

/**
 * 系统信息状态卡片
 */
@Composable
private fun SystemInfoStatusCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    uptime: String,
    processCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        shape = RoundedCornerShape(AppDimensions.RadiusM)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.PaddingM)
        ) {
            // 标题行
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.Primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = AppTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL)
            ) {
                // 运行时间
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AppColors.Info,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))
                    Text(
                        text = uptime,
                        style = AppTypography.BodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Info,
                        maxLines = 1
                    )
                    Text(
                        text = "运行时间",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }

                // 进程数量
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = AppColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))
                    Text(
                        text = processCount.toString(),
                        style = AppTypography.BodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Text(
                        text = "进程数",
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * 进程列表表格
 */
@Composable
private fun ProcessListTable(
    processes: List<ProcessInfo>,
    sortField: SortField?,
    sortDirection: SortDirection,
    onSortChange: (SortField) -> Unit,
    onKillProcess: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = 2.dp,
        shape = RoundedCornerShape(AppDimensions.RadiusM)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 表头
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.BackgroundSecondary,
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PID列 - 可排序
                    Row(
                        modifier = Modifier
                            .width(60.dp)
                            .clickable { onSortChange(SortField.PID) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "PID",
                            style = AppTypography.Caption,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        if (sortField == SortField.PID) {
                            Icon(
                                imageVector = if (sortDirection == SortDirection.ASCENDING)
                                    Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "排序",
                                tint = AppColors.Primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Text(
                        text = "进程名",
                        style = AppTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "用户",
                        style = AppTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.width(80.dp)
                    )

                    // CPU列 - 可排序
                    Row(
                        modifier = Modifier
                            .width(60.dp)
                            .clickable { onSortChange(SortField.CPU) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CPU%",
                            style = AppTypography.Caption,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        if (sortField == SortField.CPU) {
                            Icon(
                                imageVector = if (sortDirection == SortDirection.ASCENDING)
                                    Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "排序",
                                tint = AppColors.Primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    // 内存列 - 可排序
                    Row(
                        modifier = Modifier
                            .width(60.dp)
                            .clickable { onSortChange(SortField.MEMORY) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "内存%",
                            style = AppTypography.Caption,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary
                        )
                        if (sortField == SortField.MEMORY) {
                            Icon(
                                imageVector = if (sortDirection == SortDirection.ASCENDING)
                                    Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "排序",
                                tint = AppColors.Primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Text(
                        text = "运行时间",
                        style = AppTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.width(100.dp)
                    )
                }
            }

            // 进程列表
            if (processes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = AppColors.TextDisabled,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "暂无进程信息",
                            style = AppTypography.BodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(processes) { index, process ->
                        ProcessListItem(
                            process = process,
                            index = index,
                            onKillProcess = onKillProcess
                        )
                        Divider(color = AppColors.Divider.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

/**
 * 进程列表项
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ProcessListItem(
    process: ProcessInfo,
    index: Int,
    onKillProcess: (Int) -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }

    // 右键菜单
    if (showContextMenu) {
        Popup(
            offset = IntOffset(contextMenuOffset.x.toInt(), contextMenuOffset.y.toInt()),
            onDismissRequest = { showContextMenu = false },
            properties = PopupProperties(focusable = true)
        ) {
            Card(
                elevation = 8.dp,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.width(120.dp)
                ) {
                    // 结束进程选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onKillProcess(process.pid)
                                showContextMenu = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = AppColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "结束进程",
                            style = AppTypography.Caption,
                            color = AppColors.Error
                        )
                    }
                }
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .background(
                color = when {
                    isHovered -> AppColors.BackgroundSecondary.copy(alpha = 0.3f)
                    index % 2 == 0 -> AppColors.BackgroundSecondary.copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(4.dp)
            )
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.button == PointerButton.Secondary) {
                    contextMenuOffset = event.changes.first().position
                    showContextMenu = true
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // PID
        Text(
            text = process.pid.toString(),
            style = AppTypography.Caption,
            color = AppColors.TextPrimary,
            modifier = Modifier.width(60.dp)
        )

        // 进程名
        Text(
            text = process.name,
            style = AppTypography.Caption,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        // 用户
        Text(
            text = process.user,
            style = AppTypography.Caption,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(80.dp)
        )

        // CPU%
        Text(
            text = "${process.cpu.roundToInt()}%",
            style = AppTypography.Caption,
            color = when {
                process.cpu >= 50 -> AppColors.Error
                process.cpu >= 20 -> AppColors.Warning
                else -> AppColors.Success
            },
            modifier = Modifier.width(60.dp)
        )

        // 内存%
        Text(
            text = "${process.memory.roundToInt()}%",
            style = AppTypography.Caption,
            color = when {
                process.memory >= 50 -> AppColors.Error
                process.memory >= 20 -> AppColors.Warning
                else -> AppColors.Success
            },
            modifier = Modifier.width(60.dp)
        )

        // 运行时间
        Text(
            text = process.startTime,
            style = AppTypography.Caption,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(100.dp)
        )
    }
}

/**
 * 服务列表表格
 */
@Composable
private fun ServiceListTable(
    services: List<ServiceInfo>,
    onServiceAction: (String, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = 2.dp,
        shape = RoundedCornerShape(AppDimensions.RadiusM)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 表头
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.BackgroundSecondary,
                elevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "服务名称",
                        style = AppTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "状态",
                        style = AppTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(
                        text = "类型",
                        style = AppTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.width(60.dp)
                    )
                    Text(
                        text = "自启动",
                        style = AppTypography.Caption,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.width(80.dp)
                    )
                }
            }

            // 服务列表
            if (services.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AppColors.TextDisabled,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "暂无服务信息",
                            style = AppTypography.BodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    itemsIndexed(services) { index, service ->
                        ServiceListItem(
                            service = service,
                            index = index,
                            onServiceAction = onServiceAction
                        )
                        Divider(color = AppColors.Divider.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

/**
 * 服务列表项
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ServiceListItem(
    service: ServiceInfo,
    index: Int,
    onServiceAction: (String, String) -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }

    // 右键菜单
    if (showContextMenu) {
        Popup(
            offset = IntOffset(contextMenuOffset.x.toInt(), contextMenuOffset.y.toInt()),
            onDismissRequest = { showContextMenu = false },
            properties = PopupProperties(focusable = true)
        ) {
            Card(
                elevation = 8.dp,
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier.width(140.dp)
                ) {
                    // 根据服务状态显示不同选项
                    if (service.isRunning) {
                        // 运行中服务：停止、重启
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onServiceAction(service.name, "stop")
                                    showContextMenu = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = null,
                                tint = AppColors.Error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "停止服务",
                                style = AppTypography.Caption,
                                color = AppColors.Error
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onServiceAction(service.name, "restart")
                                    showContextMenu = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = AppColors.Warning,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "重启服务",
                                style = AppTypography.Caption,
                                color = AppColors.Warning
                            )
                        }
                    } else {
                        // 停止服务：启动
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onServiceAction(service.name, "start")
                                    showContextMenu = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = AppColors.Success,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "启动服务",
                                style = AppTypography.Caption,
                                color = AppColors.Success
                            )
                        }
                    }

                    // 分隔线
                    Divider()

                    // 查看状态
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onServiceAction(service.name, "status")
                                showContextMenu = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "查看状态",
                            style = AppTypography.Caption,
                            color = AppColors.TextSecondary
                        )
                    }

                    // 自启动设置
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val action = if (service.isEnabled) "disable" else "enable"
                                onServiceAction(service.name, action)
                                showContextMenu = false
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (service.isEnabled) Icons.Default.Clear else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (service.isEnabled) AppColors.Warning else AppColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (service.isEnabled) "禁用自启动" else "启用自启动",
                            style = AppTypography.Caption,
                            color = if (service.isEnabled) AppColors.Warning else AppColors.Success
                        )
                    }
                }
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .background(
                color = when {
                    isHovered -> AppColors.BackgroundSecondary.copy(alpha = 0.3f)
                    index % 2 == 0 -> AppColors.BackgroundSecondary.copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
                shape = RoundedCornerShape(4.dp)
            )
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.button == PointerButton.Secondary) {
                    contextMenuOffset = event.changes.first().position
                    showContextMenu = true
                }
            }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 服务名称和描述
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = service.name,
                style = AppTypography.Caption,
                color = AppColors.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            if (service.description.isNotEmpty()) {
                Text(
                    text = service.description,
                    style = AppTypography.Caption,
                    color = AppColors.TextSecondary,
                    maxLines = 1
                )
            }
        }

        // 运行状态
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.width(80.dp)
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = RoundedCornerShape(4.dp),
                color = if (service.isRunning) AppColors.Success else AppColors.TextDisabled
            ) {}
            Text(
                text = if (service.isRunning) "运行中" else "已停止",
                style = AppTypography.Caption,
                color = AppColors.TextSecondary
            )
        }

        // 类型
        Text(
            text = "systemd",
            style = AppTypography.Caption,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(60.dp)
        )

        // 自启动状态
        Text(
            text = if (service.isEnabled) "已启用" else "已禁用",
            style = AppTypography.Caption,
            color = if (service.isEnabled) AppColors.Success else AppColors.TextDisabled,
            modifier = Modifier.width(80.dp)
        )
    }
}

/**
 * 获取进程列表
 */
suspend fun fetchProcessList(config: SSHConfigData): ProcessList? {
    return withContext(Dispatchers.IO) {
        try {
            val sessionManager = service.SSHSessionManager.getSession(config.name)
            if (sessionManager == null || !sessionManager.isConnected()) {
                return@withContext null
            }

            // 获取进程信息 (限制前50个最活跃的进程)
            val processResult = sessionManager.executeCommand(
                "ps aux --sort=-%cpu | head -50 | awk 'NR>1 {print \$2,\$1,\$3,\$4,\$9,\$11}'"
            )

            val processes = mutableListOf<ProcessInfo>()
            val lines = processResult.getOrNull()?.trim()?.lines() ?: emptyList()

            for (line in lines) {
                if (line.trim().isNotEmpty()) {
                    val parts = line.trim().split("\\s+".toRegex(), limit = 6)
                    if (parts.size >= 6) {
                        try {
                            val pid = parts[0].toInt()
                            val user = parts[1]
                            val cpu = parts[2].toFloatOrNull() ?: 0f
                            val memory = parts[3].toFloatOrNull() ?: 0f
                            val startTime = parts[4]
                            val command = parts[5]

                            // 获取进程名称（去掉路径）
                            val processName = command.substringAfterLast('/').ifEmpty { command }

                            processes.add(
                                ProcessInfo(
                                    pid = pid,
                                    name = processName,
                                    user = user,
                                    cpu = cpu,
                                    memory = memory,
                                    startTime = startTime,
                                    command = command
                                )
                            )
                        } catch (e: Exception) {
                            // 跳过解析错误的行
                            continue
                        }
                    }
                }
            }

            ProcessList(
                processes = processes.sortedByDescending { it.cpu + it.memory }, // 按CPU+内存使用率排序
                totalCount = processes.size,
                lastUpdate = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 结束进程
 */
suspend fun killProcess(config: SSHConfigData, pid: Int): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val sessionManager = service.SSHSessionManager.getSession(config.name)
            if (sessionManager == null || !sessionManager.isConnected()) {
                return@withContext false
            }

            // 先尝试优雅结束
            val result = sessionManager.executeCommand("kill -TERM $pid")
            if (result.isSuccess) {
                // 等待一会儿再检查进程是否还存在
                kotlinx.coroutines.delay(1000)
                val checkResult = sessionManager.executeCommand("kill -0 $pid 2>/dev/null && echo 'running' || echo 'stopped'")
                val output = checkResult.getOrNull()?.trim()
                if (output == "stopped") {
                    return@withContext true
                }
            }

            // 如果TERM信号无效，尝试KILL信号
            val killResult = sessionManager.executeCommand("kill -KILL $pid")
            killResult.isSuccess
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * 获取系统监控信息
 */
suspend fun fetchSystemMetrics(config: SSHConfigData): ExtendedSystemMetrics? {
    return withContext(Dispatchers.IO) {
        try {
            val sessionManager = service.SSHSessionManager.getSession(config.name)
            if (sessionManager == null || !sessionManager.isConnected()) {
                return@withContext null
            }

            // 获取CPU使用率
            val cpuResult = sessionManager.executeCommand("top -bn1 | grep 'Cpu(s)' | sed 's/.*, *\\([0-9.]*\\)%* id.*/\\1/' | awk '{print 100 - \$1}'")

            // 获取内存信息
            val memResult = sessionManager.executeCommand("free -m | awk 'NR==2{printf \"%.2f %.0f %.0f\", \$3*100/\$2, \$3, \$2}'")

            // 获取磁盘信息
            val diskResult = sessionManager.executeCommand("df -h / | awk 'NR==2{printf \"%.2f %s %s\", \$5, \$3, \$2}' | tr -d '%'")

            // 获取运行时间
            val uptimeResult = sessionManager.executeCommand("uptime -p 2>/dev/null || uptime | awk '{print \$3 \" \" \$4}'")

            // 获取进程数
            val processResult = sessionManager.executeCommand("ps aux 2>/dev/null | wc -l 2>/dev/null || echo '0'")

            var cpuUsage = 0f
            var memoryUsage = 0f
            var memoryUsed = 0L
            var memoryTotal = 0L
            var diskUsage = 0f
            var diskUsed = "0G"
            var diskTotal = "0G"
            var uptime = "未知"

            // 解析CPU信息
            cpuResult.getOrNull()?.trim()?.toFloatOrNull()?.let { cpu ->
                cpuUsage = cpu.coerceIn(0f, 100f)
            }

            // 解析内存信息
            memResult.getOrNull()?.trim()?.split(" ")?.let { parts ->
                if (parts.size >= 3) {
                    memoryUsage = parts[0].toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
                    memoryUsed = parts[1].toLongOrNull() ?: 0L
                    memoryTotal = parts[2].toLongOrNull() ?: 0L
                }
            }

            // 解析磁盘信息
            diskResult.getOrNull()?.trim()?.split(" ")?.let { parts ->
                if (parts.size >= 3) {
                    diskUsage = parts[0].toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
                    diskUsed = parts[1]
                    diskTotal = parts[2]
                }
            }

            // 解析运行时间
            uptimeResult.getOrNull()?.trim()?.let { uptimeStr ->
                uptime = uptimeStr
            }

            ExtendedSystemMetrics(
                cpuUsage = cpuUsage,
                memoryUsage = memoryUsage,
                memoryUsed = memoryUsed,
                memoryTotal = memoryTotal,
                diskUsage = diskUsage,
                diskUsed = diskUsed,
                diskTotal = diskTotal,
                networkRx = "0MB",
                networkTx = "0MB",
                loadAverage = "1.00",
                uptime = uptime,
                processCount = processResult.getOrNull()?.trim()?.toIntOrNull() ?: 0,
                lastUpdate = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 获取服务列表
 */
suspend fun fetchServiceList(config: SSHConfigData): ServiceList? {
    return withContext(Dispatchers.IO) {
        try {
            val sessionManager = service.SSHSessionManager.getSession(config.name)
            if (sessionManager == null || !sessionManager.isConnected()) {
                return@withContext null
            }

            // 检测操作系统类型
            val osResult = sessionManager.executeCommand("uname -s")

            val services = when (osResult.getOrNull()?.trim()?.lowercase()) {
                "linux" -> {
                    // Linux系统服务检测
                    val systemctlResult = sessionManager.executeCommand("systemctl list-units --type=service --all --no-pager --no-legend 2>/dev/null | head -20")
                    val lines = systemctlResult.getOrNull()?.trim()?.lines() ?: emptyList()

                    lines.mapNotNull { line ->
                        val parts = line.trim().split("\\s+".toRegex(), limit = 5)
                        if (parts.size >= 4) {
                            val name = parts[0].removeSuffix(".service")
                            val load = parts[1]
                            val active = parts[2]
                            val sub = parts[3]

                            // 只显示loaded状态的服务
                            if (load == "loaded") {
                                ServiceInfo(
                                    name = name,
                                    status = "$active/$sub",
                                    description = "",
                                    isEnabled = true, // 简化为都认为是启用的
                                    isRunning = active == "active" && sub == "running"
                                )
                            } else null
                        } else null
                    }
                }
                else -> {
                    // 其他系统暂时返回空列表
                    emptyList()
                }
            }

            ServiceList(
                services = services,
                lastUpdate = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 对进程列表进行排序
 */
private fun sortProcesses(
    processes: List<ProcessInfo>,
    field: SortField?,
    direction: SortDirection
): List<ProcessInfo> {
    if (field == null) return processes

    return when (field) {
        SortField.PID -> {
            if (direction == SortDirection.ASCENDING) {
                processes.sortedBy { it.pid }
            } else {
                processes.sortedByDescending { it.pid }
            }
        }
        SortField.CPU -> {
            if (direction == SortDirection.ASCENDING) {
                processes.sortedBy { it.cpu }
            } else {
                processes.sortedByDescending { it.cpu }
            }
        }
        SortField.MEMORY -> {
            if (direction == SortDirection.ASCENDING) {
                processes.sortedBy { it.memory }
            } else {
                processes.sortedByDescending { it.memory }
            }
        }
    }
}

/**
 * 检查服务状态
 */
suspend fun performServiceStatusCheck(config: SSHConfigData, serviceName: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val sessionManager = service.SSHSessionManager.getSession(config.name)
            if (sessionManager == null || !sessionManager.isConnected()) {
                return@withContext "无法连接到主机"
            }

            val result = sessionManager.executeCommand("systemctl status $serviceName")
            result.getOrNull()?.trim() ?: "获取状态失败"
        } catch (e: Exception) {
            "错误: ${e.message}"
        }
    }
}

/**
 * 执行服务操作
 */
suspend fun performServiceAction(config: SSHConfigData, serviceName: String, action: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val sessionManager = service.SSHSessionManager.getSession(config.name)
            if (sessionManager == null || !sessionManager.isConnected()) {
                return@withContext false
            }

            val command = when (action) {
                "start" -> "sudo systemctl start $serviceName"
                "stop" -> "sudo systemctl stop $serviceName"
                "restart" -> "sudo systemctl restart $serviceName"
                "enable" -> "sudo systemctl enable $serviceName"
                "disable" -> "sudo systemctl disable $serviceName"
                "status" -> "systemctl is-active $serviceName"
                else -> return@withContext false
            }

            val result = sessionManager.executeCommand(command)
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
}
