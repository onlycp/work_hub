package ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.AwtWindow
import data.ExpenseData
import data.ExpenseManager
import data.ExpenseReportData
import data.ExpenseStats
import data.ExpenseType
import data.MemberData
import data.MemberManager
import data.CurrentUserManager
import kotlinx.coroutines.launch
import theme.AppColors
import theme.AppDimensions
import theme.AppTypography
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

/**
 * 获取凭证文件存储目录
 * 文件存储在当前用户的Git仓库目录下的receipts文件夹中
 */
private fun getReceiptsDirectory(): File {
    // 获取当前用户名
    val currentUserName = CurrentUserManager.getCurrentUserId()

    // 如果没有登录用户，返回默认目录
    if (currentUserName.isEmpty()) {
        val fallbackDir = File(System.getProperty("user.home"), ".workhub")
        val receiptsDir = File(fallbackDir, "receipts")
        if (!receiptsDir.exists()) {
            receiptsDir.mkdirs()
        }
        return receiptsDir
    }

    // 获取用户的数据目录（Git仓库目录）
    val workhubDir = File(System.getProperty("user.home"), ".workhub")
    val usersDir = File(workhubDir, "users")
    val userDir = File(usersDir, currentUserName)
    val receiptsDir = File(userDir, "receipts")

    // 确保目录存在
    if (!receiptsDir.exists()) {
        receiptsDir.mkdirs()
    }

    return receiptsDir
}

/**
 * 复制文件到凭证目录
 */
private fun copyFileToReceiptsDirectory(sourceFile: File): Result<File> {
    return try {
        val receiptsDir = getReceiptsDirectory()
        // 生成唯一文件名以避免冲突
        val fileExtension = sourceFile.extension
        val timestamp = System.currentTimeMillis()
        val uniqueFileName = "${timestamp}_${sourceFile.nameWithoutExtension}.$fileExtension"
        val targetFile = File(receiptsDir, uniqueFileName)

        // 复制文件
        Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

        Result.success(targetFile)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * 获取凭证文件的完整路径
 */
fun getReceiptFilePath(fileName: String?): File? {
    return fileName?.let {
        val receiptsDir = getReceiptsDirectory()
        File(receiptsDir, it)
    }
}

/**
 * 报销标签页枚举
 */
enum class ExpenseTab(
    val displayName: String,
    val icon: ImageVector
) {
    REGISTRATION("登记", Icons.Default.Add),
    REPORT("报表", Icons.Default.Assessment)
}

/**
 * 日常报销内容
 */
@Composable
fun ExpenseContent() {
    var selectedTab by remember { mutableStateOf(ExpenseTab.REGISTRATION) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab栏 - 使用自定义样式，参考主机模块的实现
        ExpenseTabsHeader(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // 内容区域
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                ExpenseTab.REGISTRATION -> ExpenseRegistrationContent()
                ExpenseTab.REPORT -> ExpenseReportContent()
            }
        }
    }
}

/**
 * 报销Tab标题栏 - 参考主机模块的MultipleTabsHeader实现
 */
@Composable
private fun ExpenseTabsHeader(
    selectedTab: ExpenseTab,
    onTabSelected: (ExpenseTab) -> Unit
) {
    // 使用自定义的水平滚动Row
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.BackgroundSecondary
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = AppDimensions.SpaceS, vertical = AppDimensions.SpaceXS)
        ) {
            ExpenseTab.values().forEach { tab ->
                val isSelected = selectedTab == tab

                // 自定义Tab样式
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 1.dp, vertical = 1.dp)
                        .clickable { onTabSelected(tab) },
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) AppColors.Primary.copy(alpha = 0.1f) else Color.Transparent,
                    border = if (isSelected) BorderStroke(
                        1.dp,
                        AppColors.Primary.copy(alpha = 0.3f)
                    ) else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.displayName,
                            tint = if (isSelected) AppColors.Primary else AppColors.TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )

                        Text(
                            text = tab.displayName,
                            style = AppTypography.Caption,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            color = if (isSelected) AppColors.Primary else AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * 报销登记内容
 */
@Composable
private fun ExpenseRegistrationContent() {
    val scope = rememberCoroutineScope()
    val expenses by ExpenseManager.expenses.collectAsState()
    val members by MemberManager.members.collectAsState()
    val currentUserId = CurrentUserManager.getCurrentUserId()
    val currentUserName = CurrentUserManager.getCurrentUserName()

    // 查询条件状态
    var searchMember by remember { mutableStateOf<MemberData?>(null) }
    var selectedType by remember { mutableStateOf<ExpenseType?>(null) }
    var startDate by remember { mutableStateOf<Long?>(null) }
    var endDate by remember { mutableStateOf<Long?>(null) }

    // 对话框状态
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseData?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<ExpenseData?>(null) }

    // 分页状态
    val pageSize = 10
    var currentPage by remember { mutableStateOf(1) }

    // 日期格式化器
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dateTimeFormatter = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    // 根据查询条件过滤数据
    val filteredExpenses = remember(expenses, searchMember, selectedType, startDate, endDate) {
        ExpenseManager.queryExpenses(
            userName = searchMember?.name?.takeIf { it.isNotBlank() },
            startDate = startDate,
            endDate = endDate,
            type = selectedType
        )
    }

    // 分页数据
    val totalPages = ceil(filteredExpenses.size.toDouble() / pageSize).toInt()
    val paginatedExpenses = remember(filteredExpenses, currentPage) {
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, filteredExpenses.size)
        if (startIndex < filteredExpenses.size) {
            filteredExpenses.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    // 统计数据
    val (personalStats, teamStats) = remember(filteredExpenses, currentUserId, searchMember) {
        ExpenseManager.getStatsWithFilter(searchMember?.name ?: "", startDate, endDate, selectedType)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 上部分：统计面板
        ExpenseStatsPanel(personalStats = personalStats, teamStats = teamStats)

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 查询条件和操作栏
        ExpenseQueryAndActionBar(
            members = members,
            searchMember = searchMember,
            onSearchMemberChange = { searchMember = it },
            selectedType = selectedType,
            onTypeSelected = { selectedType = it },
            startDate = startDate,
            endDate = endDate,
            onDateRangeSelected = { start, end ->
                startDate = start
                endDate = end
            },
            onAddExpense = {
                editingExpense = null
                showExpenseDialog = true
            }
        )

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 下部分：报销表格
        ExpenseTable(
            expenses = paginatedExpenses,
            totalCount = filteredExpenses.size,
            currentPage = currentPage,
            totalPages = totalPages,
            pageSize = pageSize,
            onPageChange = { currentPage = it },
            onEditExpense = { expense ->
                editingExpense = expense
                showExpenseDialog = true
            },
            onDeleteExpense = { expense ->
                showDeleteConfirmDialog = expense
            },
            dateFormatter = dateFormatter,
            dateTimeFormatter = dateTimeFormatter
        )
    }

    // 报销对话框
    if (showExpenseDialog) {
                ExpenseDialog(
                    expense = editingExpense,
                    currentUserId = currentUserName, // 传递用户名而不是用户ID
                    onDismiss = {
                        showExpenseDialog = false
                        editingExpense = null
                    },
                    onConfirm = { expense ->
                        scope.launch {
                            try {
                                val result = if (editingExpense != null) {
                                    ExpenseManager.updateExpense(expense.id, expense)
                                } else {
                                    ExpenseManager.addExpense(expense)
                                }

                                if (result.isSuccess) {
                                    showExpenseDialog = false
                                    editingExpense = null
                                } else {
                                    // 这里应该显示错误提示，但暂时使用println
                                    println("保存失败: ${result.exceptionOrNull()?.message}")
                                }
                            } catch (e: Exception) {
                                println("操作失败: ${e.message}")
                            }
                        }
                    }
                )
    }

    // 删除确认对话框
    showDeleteConfirmDialog?.let { expense ->
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
                    text = "确定要删除报销记录 \"${expense.type.displayName} - ¥${expense.amount}\" 吗？此操作不可撤销。",
                    style = AppTypography.BodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val result = ExpenseManager.deleteExpense(expense.id)
                            if (result.isSuccess) {
                                showDeleteConfirmDialog = null
                            } else {
                                println("删除失败: ${result.exceptionOrNull()?.message}")
                            }
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

/**
 * 统计面板组件
 */
@Composable
private fun ExpenseStatsPanel(
    personalStats: ExpenseStats,
    teamStats: ExpenseStats
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
    ) {
        // 个人统计卡片
        StatsCard(
            title = "个人统计",
            count = personalStats.count,
            amount = personalStats.totalAmount,
            modifier = Modifier.weight(1f)
        )

        // 团队统计卡片
        StatsCard(
            title = "团队统计",
            count = teamStats.count,
            amount = teamStats.totalAmount,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 统计卡片组件
 */
@Composable
private fun StatsCard(
    title: String,
    count: Int,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = 4.dp,
        shape = RoundedCornerShape(AppDimensions.RadiusM)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.SpaceM),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))

            // 笔数和金额在同一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 笔数
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
                ) {
                    Text(
                        text = "笔数:",
                        style = AppTypography.Caption,
                        color = AppColors.TextDisabled
                    )
                    Text(
                        text = count.toString(),
                        style = AppTypography.BodyMedium,
                        color = AppColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 总额
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
                ) {
                    Text(
                        text = "金额:",
                        style = AppTypography.Caption,
                        color = AppColors.TextDisabled
                    )
                    Text(
                        text = "¥${String.format("%.2f", amount)}",
                        style = AppTypography.BodyMedium,
                        color = AppColors.Success,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 查询条件和操作栏组件
 */
@Composable
private fun ExpenseQueryAndActionBar(
    members: List<MemberData>,
    searchMember: MemberData?,
    onSearchMemberChange: (MemberData?) -> Unit,
    selectedType: ExpenseType?,
    onTypeSelected: (ExpenseType?) -> Unit,
    startDate: Long?,
    endDate: Long?,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    onAddExpense: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {

        // 第二行：查询条件
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
        ) {
            // 人员搜索
            MemberDropdown(
                members = members,
                selectedMember = searchMember,
                onMemberSelected = onSearchMemberChange,
                label = "搜索报销人员",
                modifier = Modifier
                    .weight(1f)
                    .height(AppDimensions.InputHeightSmall)
            )

            // 类型选择
            ExpenseTypeDropdown(
                selectedType = selectedType,
                onTypeSelected = onTypeSelected,
                modifier = Modifier.width(140.dp).height(AppDimensions.InputHeightSmall)
            )

            // 日期范围选择
            DateRangeSelector(
                startDate = startDate,
                endDate = endDate,
                onDateRangeSelected = onDateRangeSelected,
                modifier = Modifier.width(200.dp)
            )

            // 添加报销按钮
            Button(
                onClick = onAddExpense,
                modifier = Modifier.height(AppDimensions.InputHeightSmall),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加报销",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("添加报销", style = AppTypography.Caption)
            }
        }
    }
}

/**
 * 类型下拉选择组件
 */
@Composable
private fun ExpenseTypeDropdown(
    selectedType: ExpenseType?,
    onTypeSelected: (ExpenseType?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CornerSmall)
        ) {
            Text(
                text = selectedType?.displayName ?: "选择类型",
                style = AppTypography.BodySmall,
                color = if (selectedType != null) AppColors.TextPrimary else AppColors.TextDisabled
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(onClick = {
                onTypeSelected(null)
                expanded = false
            }) {
                Text("全部类型", style = AppTypography.BodySmall)
            }
            ExpenseType.values().forEach { type ->
                DropdownMenuItem(onClick = {
                    onTypeSelected(type)
                    expanded = false
                }) {
                    Text(type.displayName, style = AppTypography.BodySmall)
                }
            }
        }
    }
}

/**
 * 日期范围选择组件
 */
@Composable
private fun DateRangeSelector(
    startDate: Long?,
    endDate: Long?,
    onDateRangeSelected: (Long?, Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val displayText = when {
        startDate != null && endDate != null -> "${dateFormatter.format(Date(startDate))} ~ ${dateFormatter.format(Date(endDate))}"
        else -> "选择日期范围"
    }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CornerSmall)
        ) {
            Text(
                text = displayText,
                style = AppTypography.BodySmall,
                color = if (startDate != null) AppColors.TextPrimary else AppColors.TextDisabled
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val calendar = Calendar.getInstance()

            // 今天
            DropdownMenuItem(onClick = {
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, 0)
                today.set(Calendar.MINUTE, 0)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)
                val start = today.timeInMillis

                today.set(Calendar.HOUR_OF_DAY, 23)
                today.set(Calendar.MINUTE, 59)
                today.set(Calendar.SECOND, 59)
                today.set(Calendar.MILLISECOND, 999)
                val end = today.timeInMillis

                onDateRangeSelected(start, end)
                expanded = false
            }) {
                Text("今天", style = AppTypography.BodySmall)
            }

            // 昨天
            DropdownMenuItem(onClick = {
                val yesterday = Calendar.getInstance()
                yesterday.add(Calendar.DAY_OF_MONTH, -1)
                yesterday.set(Calendar.HOUR_OF_DAY, 0)
                yesterday.set(Calendar.MINUTE, 0)
                yesterday.set(Calendar.SECOND, 0)
                yesterday.set(Calendar.MILLISECOND, 0)
                val start = yesterday.timeInMillis

                yesterday.set(Calendar.HOUR_OF_DAY, 23)
                yesterday.set(Calendar.MINUTE, 59)
                yesterday.set(Calendar.SECOND, 59)
                yesterday.set(Calendar.MILLISECOND, 999)
                val end = yesterday.timeInMillis

                onDateRangeSelected(start, end)
                expanded = false
            }) {
                Text("昨天", style = AppTypography.BodySmall)
            }

            // 最近7天
            DropdownMenuItem(onClick = {
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                endCalendar.set(Calendar.MILLISECOND, 999)
                val end = endCalendar.timeInMillis

                val startCalendar = Calendar.getInstance()
                startCalendar.add(Calendar.DAY_OF_MONTH, -6)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val start = startCalendar.timeInMillis

                onDateRangeSelected(start, end)
                expanded = false
            }) {
                Text("最近7天", style = AppTypography.BodySmall)
            }

            // 最近30天
            DropdownMenuItem(onClick = {
                val endCalendar = Calendar.getInstance()
                endCalendar.set(Calendar.HOUR_OF_DAY, 23)
                endCalendar.set(Calendar.MINUTE, 59)
                endCalendar.set(Calendar.SECOND, 59)
                endCalendar.set(Calendar.MILLISECOND, 999)
                val end = endCalendar.timeInMillis

                val startCalendar = Calendar.getInstance()
                startCalendar.add(Calendar.DAY_OF_MONTH, -29)
                startCalendar.set(Calendar.HOUR_OF_DAY, 0)
                startCalendar.set(Calendar.MINUTE, 0)
                startCalendar.set(Calendar.SECOND, 0)
                startCalendar.set(Calendar.MILLISECOND, 0)
                val start = startCalendar.timeInMillis

                onDateRangeSelected(start, end)
                expanded = false
            }) {
                Text("最近30天", style = AppTypography.BodySmall)
            }

            // 本月
            DropdownMenuItem(onClick = {
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val monthEnd = calendar.timeInMillis

                onDateRangeSelected(monthStart, monthEnd)
                expanded = false
            }) {
                Text("本月", style = AppTypography.BodySmall)
            }

            // 上月
            DropdownMenuItem(onClick = {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val lastMonthStart = calendar.timeInMillis

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val lastMonthEnd = calendar.timeInMillis

                onDateRangeSelected(lastMonthStart, lastMonthEnd)
                expanded = false
            }) {
                Text("上月", style = AppTypography.BodySmall)
            }

            // 清除选择
            DropdownMenuItem(onClick = {
                onDateRangeSelected(null, null)
                expanded = false
            }) {
                Text("清除选择", style = AppTypography.BodySmall)
            }
        }
    }
}

/**
 * 报销表格组件
 */
@Composable
private fun ExpenseTable(
    expenses: List<ExpenseData>,
    totalCount: Int,
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    onPageChange: (Int) -> Unit,
    onEditExpense: (ExpenseData) -> Unit,
    onDeleteExpense: (ExpenseData) -> Unit,
    dateFormatter: SimpleDateFormat,
    dateTimeFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(AppDimensions.RadiusL)
    ) {
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
                        text = "报销人员",
                        modifier = Modifier.weight(1.2f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "类型",
                        modifier = Modifier.weight(1f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "金额",
                        modifier = Modifier.weight(0.8f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "日期",
                        modifier = Modifier.weight(1f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "凭证",
                        modifier = Modifier.weight(0.8f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "备注",
                        modifier = Modifier.weight(1.5f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "操作",
                        modifier = Modifier.width(100.dp),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // 表格内容
            if (expenses.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(AppDimensions.PaddingL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = AppColors.TextDisabled
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                        Text(
                            text = "暂无报销记录",
                            style = AppTypography.BodyLarge,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                        Text(
                            text = "添加报销记录以开始管理",
                            style = AppTypography.Caption,
                            color = AppColors.TextDisabled
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(expenses) { expense ->
                        ExpenseTableRow(
                            expense = expense,
                            onEdit = { onEditExpense(expense) },
                            onDelete = { onDeleteExpense(expense) },
                            dateFormatter = dateFormatter,
                            dateTimeFormatter = dateTimeFormatter
                        )
                    }
                }
            }

            // 分页控件
            if (totalPages > 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.BackgroundSecondary,
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第 $currentPage 页，共 $totalPages 页 (共 $totalCount 条记录)",
                            style = AppTypography.Caption,
                            color = AppColors.TextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                                enabled = currentPage > 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "上一页",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "$currentPage",
                                style = AppTypography.Caption,
                                color = AppColors.Primary,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                                enabled = currentPage < totalPages,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "下一页",
                                    modifier = Modifier.size(16.dp)
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
 * 报销表格行组件
 */
@Composable
private fun ExpenseTableRow(
    expense: ExpenseData,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dateFormatter: SimpleDateFormat,
    dateTimeFormatter: SimpleDateFormat
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
            // 报销人员
            Text(
                text = expense.userName,
                modifier = Modifier.weight(1.2f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 类型
            Text(
                text = expense.type.displayName,
                modifier = Modifier.weight(1f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 金额
            Text(
                text = "¥${String.format("%.2f", expense.amount)}",
                modifier = Modifier.weight(0.8f),
                style = AppTypography.BodySmall,
                color = AppColors.Success,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 日期
            Text(
                text = dateFormatter.format(Date(expense.date)),
                modifier = Modifier.weight(1f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 凭证
            Text(
                text = if (expense.receiptFileName != null) "✓" else "无",
                modifier = Modifier.weight(0.8f),
                style = AppTypography.BodySmall,
                color = if (expense.receiptFileName != null) AppColors.Success else AppColors.TextDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 备注
            Text(
                text = expense.remarks.takeIf { it.isNotBlank() } ?: "无备注",
                modifier = Modifier.weight(1.5f),
                style = AppTypography.BodySmall,
                color = if (expense.remarks.isBlank()) AppColors.TextDisabled else AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 操作按钮
            Row(
                modifier = Modifier.width(100.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = AppColors.Primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = AppColors.Error,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 报销对话框
 */
@Composable
private fun ExpenseDialog(
    expense: ExpenseData?,
    currentUserId: String,
    onDismiss: () -> Unit,
    onConfirm: (ExpenseData) -> Unit
) {
    val members by MemberManager.members.collectAsState()
    // 使用用户名而不是ID来查找当前成员，因为成员ID是UUID，而用户ID是用户名
    val currentMember = members.find { it.name == currentUserId }

    var selectedMember by remember { mutableStateOf<MemberData?>(null) }

    // 初始化选中成员
    LaunchedEffect(expense, members, currentMember) {
        selectedMember = when {
            // 编辑模式：选择报销记录中的人员（根据用户名查找）
            expense != null -> members.find { it.name == expense.userId }
            // 新建模式：默认选择当前用户
            else -> currentMember
        }
    }
    var selectedType by remember { mutableStateOf(expense?.type ?: ExpenseType.DINING) }
    var amountText by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var date by remember { mutableStateOf(expense?.date ?: System.currentTimeMillis()) }
    var remarks by remember { mutableStateOf(expense?.remarks ?: "") }
    var receiptPath by remember { mutableStateOf(expense?.receiptPath) }
    var receiptFileName by remember { mutableStateOf(expense?.receiptFileName) }
    var showFileDialog by remember { mutableStateOf(false) }

    val isEditing = expense != null
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // 文件选择器
    if (showFileDialog) {
        FileDialog(
            onCloseRequest = { selectedFile ->
                if (selectedFile != null) {
                    // 复制文件到应用目录
                    val copyResult = copyFileToReceiptsDirectory(selectedFile)
                    if (copyResult.isSuccess) {
                        val copiedFile = copyResult.getOrNull()!!
                        // 保存相对路径（相对于凭证目录）
                        receiptPath = copiedFile.name  // 只保存文件名，因为文件已经在凭证目录中
                        receiptFileName = selectedFile.name  // 保存原始文件名用于显示
                    } else {
                        // 文件复制失败，可以在这里显示错误提示
                        println("文件复制失败: ${copyResult.exceptionOrNull()?.message}")
                    }
                }
                showFileDialog = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "编辑报销" else "添加报销",
                style = AppTypography.BodyLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
            ) {
                // 报销人员选择
                MemberDropdown(
                    members = members,
                    selectedMember = selectedMember,
                    onMemberSelected = { selectedMember = it },
                    label = "报销人员",
                    modifier = Modifier.fillMaxWidth()
                )

                // 类型选择
                ExpenseTypeDropdown(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it ?: ExpenseType.DINING },
                    modifier = Modifier.fillMaxWidth()
                )

                // 金额
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("金额", style = AppTypography.Caption) },
                    placeholder = { Text("0.00", style = AppTypography.BodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 14.sp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )

                // 日期
                OutlinedTextField(
                    value = dateFormatter.format(Date(date)),
                    onValueChange = { },
                    label = { Text("报销日期", style = AppTypography.Caption) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 14.sp),
                    singleLine = true,
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            // 这里可以添加日期选择器，暂时保持只读
                        }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "选择日期",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                // 报销凭证上传
                OutlinedButton(
                    onClick = {
                        showFileDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppDimensions.CornerSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "上传凭证",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (receiptFileName != null) "已上传: $receiptFileName" else "上传报销凭证 (支持图片和PDF)",
                        style = AppTypography.BodySmall
                    )
                }

                // 备注说明
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("备注说明", style = AppTypography.Caption) },
                    placeholder = { Text("可选", style = AppTypography.BodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 14.sp),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount != null && amount > 0 && selectedMember != null) {
                            val expenseData = ExpenseData(
                                id = expense?.id ?: "",
                                userId = selectedMember!!.name, // 存储用户名作为userId
                                userName = selectedMember!!.name, // 存储成员姓名
                                type = selectedType,
                                amount = amount,
                                date = date,
                                receiptPath = receiptPath,
                                receiptFileName = receiptFileName,
                                remarks = remarks,
                                createdBy = currentUserId
                            )
                            onConfirm(expenseData)
                        }
                    }
                ) {
                    Text(if (isEditing) "保存" else "添加", style = AppTypography.Caption)
                }
            },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("取消", style = AppTypography.Caption)
            }
        }
    )
}

/**
 * 成员下拉选择组件（与类型选择保持相同样式）
 */
@Composable
private fun MemberDropdown(
    members: List<MemberData>,
    selectedMember: MemberData?,
    onMemberSelected: (MemberData?) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CornerSmall)
        ) {
            Text(
                text = selectedMember?.let { "${it.name} (${it.number})" } ?: "请选择${label.lowercase()}",
                style = AppTypography.BodySmall,
                color = if (selectedMember != null) AppColors.TextPrimary else AppColors.TextDisabled
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 清除选择选项
            DropdownMenuItem(onClick = {
                onMemberSelected(null)
                expanded = false
            }) {
                Text("全部成员", style = AppTypography.BodySmall)
            }
            members.forEach { member ->
                DropdownMenuItem(
                    onClick = {
                        onMemberSelected(member)
                        expanded = false
                    }
                ) {
                    Text(
                        text = "${member.name} (${member.number})",
                        style = AppTypography.BodySmall
                    )
                }
            }
        }
    }
}

/**
 * 文件选择对话框组件
 */
@Composable
private fun FileDialog(
    onCloseRequest: (File?) -> Unit
) {
    AwtWindow(
        create = {
            object : FileDialog(null as Frame?, "选择报销凭证文件") {
                init {
                    // 设置文件过滤器，支持图片和PDF
                    file = "*.jpg;*.jpeg;*.png;*.gif;*.bmp;*.pdf"
                    mode = FileDialog.LOAD

                    // 设置为模态对话框
                    isModal = true
                    isResizable = false

                    // 添加窗口关闭监听器
                    addWindowListener(object : java.awt.event.WindowAdapter() {
                        override fun windowClosing(e: java.awt.event.WindowEvent?) {
                            onCloseRequest(null)
                        }
                    })
                }

                override fun setVisible(visible: Boolean) {
                    super.setVisible(visible)
                    if (visible) {
                        // 等待用户选择文件
                        val selectedFile = if (file != null) File(directory, file) else null
                        onCloseRequest(selectedFile)
                    }
                }
            }
        },
        dispose = { fileDialog ->
            fileDialog.dispose()
        }
    )
}

/**
 * 报表统计面板组件
 */
@Composable
private fun ExpenseReportStatsPanel(stats: ExpenseStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(AppDimensions.RadiusM)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppDimensions.SpaceM),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "报表统计",
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(AppDimensions.SpaceXS))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceL),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 总记录数
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
                ) {
                    Text(
                        text = "记录数:",
                        style = AppTypography.Caption,
                        color = AppColors.TextDisabled
                    )
                    Text(
                        text = stats.count.toString(),
                        style = AppTypography.BodyMedium,
                        color = AppColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 总金额
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceXS)
                ) {
                    Text(
                        text = "总金额:",
                        style = AppTypography.Caption,
                        color = AppColors.TextDisabled
                    )
                    Text(
                        text = "¥${String.format("%.2f", stats.totalAmount)}",
                        style = AppTypography.BodyMedium,
                        color = AppColors.Success,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 报表查询条件栏组件
 */
@Composable
private fun ExpenseReportQueryBar(
    members: List<MemberData>,
    availableMonths: List<String>,
    searchMember: MemberData?,
    onSearchMemberChange: (MemberData?) -> Unit,
    selectedMonth: String?,
    onMonthSelected: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceM)
    ) {
        // 成员筛选
        MemberDropdown(
            members = members,
            selectedMember = searchMember,
            onMemberSelected = onSearchMemberChange,
            label = "筛选成员",
            modifier = Modifier
                .weight(1f)
                .height(AppDimensions.InputHeightSmall)
        )

        // 月份筛选
        MonthDropdown(
            availableMonths = availableMonths,
            selectedMonth = selectedMonth,
            onMonthSelected = onMonthSelected,
            modifier = Modifier
                .weight(1f)
                .height(AppDimensions.InputHeightSmall)
        )
    }
}

/**
 * 月份下拉选择组件
 * 支持选择具体月份或全部月份
 */
@Composable
private fun MonthDropdown(
    availableMonths: List<String>,
    selectedMonth: String?,
    onMonthSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "选择月份"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CornerSmall)
        ) {
            Text(
                text = selectedMonth?.let { "${it}月" } ?: label,
                style = AppTypography.BodySmall,
                color = if (selectedMonth != null) AppColors.TextPrimary else AppColors.TextDisabled
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // 全部月份选项
            DropdownMenuItem(onClick = {
                onMonthSelected(null)
                expanded = false
            }) {
                Text("全部月份", style = AppTypography.BodySmall)
            }

            // 分隔线
            Divider()

            // 具体月份选项
            availableMonths.forEach { month ->
                DropdownMenuItem(onClick = {
                    onMonthSelected(month)
                    expanded = false
                }) {
                    Text("${month}月", style = AppTypography.BodySmall)
                }
            }
        }
    }
}

/**
 * 报表表格组件
 */
@Composable
private fun ExpenseReportTable(
    reportData: List<ExpenseReportData>,
    totalCount: Int,
    currentPage: Int,
    totalPages: Int,
    pageSize: Int,
    onPageChange: (Int) -> Unit,
    monthYearFormatter: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        shape = RoundedCornerShape(AppDimensions.RadiusL)
    ) {
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
                        text = "成员",
                        modifier = Modifier.weight(1f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "月份",
                        modifier = Modifier.weight(0.8f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "记录数",
                        modifier = Modifier.weight(0.6f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "总额",
                        modifier = Modifier.weight(0.8f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        text = "类型明细",
                        modifier = Modifier.weight(2f),
                        style = AppTypography.BodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }
            }

            // 表格内容
            if (reportData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(AppDimensions.PaddingL),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = AppColors.TextDisabled
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                        Text(
                            text = "暂无报表数据",
                            style = AppTypography.BodyLarge,
                            color = AppColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(AppDimensions.SpaceS))
                        Text(
                            text = "请添加报销记录后查看报表",
                            style = AppTypography.Caption,
                            color = AppColors.TextDisabled
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(reportData) { reportItem ->
                        ExpenseReportTableRow(
                            reportData = reportItem,
                            monthYearFormatter = monthYearFormatter
                        )
                    }
                }
            }

            // 分页控件
            if (totalPages > 1) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AppColors.BackgroundSecondary,
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppDimensions.PaddingM, vertical = AppDimensions.SpaceS),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第 $currentPage 页，共 $totalPages 页 (共 $totalCount 条记录)",
                            style = AppTypography.Caption,
                            color = AppColors.TextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpaceS),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                                enabled = currentPage > 1,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "上一页",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "$currentPage",
                                style = AppTypography.Caption,
                                color = AppColors.Primary,
                                fontWeight = FontWeight.Medium
                            )

                            IconButton(
                                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                                enabled = currentPage < totalPages,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "下一页",
                                    modifier = Modifier.size(16.dp)
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
 * 报表表格行组件
 */
@Composable
private fun ExpenseReportTableRow(
    reportData: ExpenseReportData,
    monthYearFormatter: SimpleDateFormat
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
            // 成员
            Text(
                text = reportData.memberName,
                modifier = Modifier.weight(1f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 月份
            Text(
                text = "${reportData.month}月",
                modifier = Modifier.weight(0.8f),
                style = AppTypography.BodySmall,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 记录数
            Text(
                text = reportData.recordCount.toString(),
                modifier = Modifier.weight(0.6f),
                style = AppTypography.BodySmall,
                color = AppColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 总额
            Text(
                text = "¥${String.format("%.2f", reportData.totalAmount)}",
                modifier = Modifier.weight(0.8f),
                style = AppTypography.BodySmall,
                color = AppColors.Success,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 类型明细
            Column(modifier = Modifier.weight(2f)) {
                reportData.typeAmounts.entries
                    .sortedByDescending { it.value } // 按金额降序排序
                    .take(3) // 只显示前3个类型
                    .forEach { (type, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = type.displayName,
                                style = AppTypography.Caption,
                                color = AppColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "¥${String.format("%.2f", amount)}",
                                style = AppTypography.Caption,
                                color = AppColors.Success,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                if (reportData.typeAmounts.size > 3) {
                    Text(
                        text = "...等${reportData.typeAmounts.size}个类型",
                        style = AppTypography.Caption,
                        color = AppColors.TextDisabled
                    )
                }
            }
        }
    }
}

/**
 * 报销报表内容
 */
@Composable
private fun ExpenseReportContent() {
    val scope = rememberCoroutineScope()
    val members by MemberManager.members.collectAsState()
    val availableMonths = remember { ExpenseManager.getAvailableMonths() }

    // 查询条件状态
    var searchMember by remember { mutableStateOf<MemberData?>(null) }
    var selectedMonth by remember { mutableStateOf<String?>(null) }

    // 分页状态
    val pageSize = 10
    var currentPage by remember { mutableStateOf(1) }

    // 日期格式化器
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val monthYearFormatter = remember { SimpleDateFormat("yyyy年MM月", Locale.getDefault()) }

    // 根据查询条件生成报表数据
    val reportData = remember(searchMember, selectedMonth) {
        ExpenseManager.generateExpenseReport(
            memberNameFilter = searchMember?.name,
            monthFilter = selectedMonth
        )
    }

    // 分页数据
    val totalPages = ceil(reportData.size.toDouble() / pageSize).toInt()
    val paginatedReportData = remember(reportData, currentPage) {
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, reportData.size)
        if (startIndex < reportData.size) {
            reportData.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
    }

    // 总统计数据
    val totalStats = remember(reportData) {
        ExpenseStats(
            count = reportData.sumOf { it.recordCount },
            totalAmount = reportData.sumOf { it.totalAmount }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 报表统计面板
        ExpenseReportStatsPanel(stats = totalStats)

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 查询条件栏
        ExpenseReportQueryBar(
            members = members,
            availableMonths = availableMonths,
            searchMember = searchMember,
            onSearchMemberChange = { searchMember = it },
            selectedMonth = selectedMonth,
            onMonthSelected = { selectedMonth = it }
        )

        Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

        // 报表表格
        ExpenseReportTable(
            reportData = paginatedReportData,
            totalCount = reportData.size,
            currentPage = currentPage,
            totalPages = totalPages,
            pageSize = pageSize,
            onPageChange = { currentPage = it },
            monthYearFormatter = monthYearFormatter
        )
    }
}
