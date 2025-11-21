package data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

/**
 * 报销管理器
 * 报销数据完全共享，所有用户都能查看和编辑
 */
object ExpenseManager {
    private var currentUserId: String = ""
    private val _expenses = MutableStateFlow<List<ExpenseData>>(emptyList())
    val expenses: StateFlow<List<ExpenseData>> = _expenses.asStateFlow()

    /**
     * 数据文件路径
     */
    private fun getDataFile(): File {
        val userDataDir = File(System.getProperty("user.home"), ".workhub")
        if (!userDataDir.exists()) {
            userDataDir.mkdirs()
        }
        return File(userDataDir, "expenses.json")
    }

    /**
     * 设置当前用户
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
        loadExpenses()
    }

    /**
     * 加载报销数据
     */
    private fun loadExpenses() {
        try {
            val file = getDataFile()
            if (file.exists()) {
                val jsonText = file.readText()
                val expensesList = Json.decodeFromString<List<ExpenseData>>(jsonText)
                _expenses.value = expensesList
                println("报销数据加载完成，共 ${expensesList.size} 条记录")
            } else {
                _expenses.value = emptyList()
                println("报销数据文件不存在，初始化为空列表")
            }
        } catch (e: Exception) {
            println("加载报销数据失败: ${e.message}")
            _expenses.value = emptyList()
        }
    }

    /**
     * 保存报销数据
     */
    private fun saveExpenses(expenses: List<ExpenseData>) {
        try {
            val file = getDataFile()
            val jsonText = Json { prettyPrint = true }.encodeToString(expenses)
            file.writeText(jsonText)
            println("报销数据保存成功，共 ${expenses.size} 条记录")
        } catch (e: Exception) {
            println("保存报销数据失败: ${e.message}")
            throw e
        }
    }

    /**
     * 添加报销记录
     */
    suspend fun addExpense(expense: ExpenseData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentExpenses = _expenses.value
            val newExpenses = currentExpenses + expense
            saveExpenses(newExpenses)
            _expenses.value = newExpenses
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 更新报销记录
     */
    suspend fun updateExpense(expenseId: String, updatedExpense: ExpenseData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentExpenses = _expenses.value
            val newExpenses = currentExpenses.map {
                if (it.id == expenseId) updatedExpense else it
            }
            saveExpenses(newExpenses)
            _expenses.value = newExpenses
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 删除报销记录
     */
    suspend fun deleteExpense(expenseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentExpenses = _expenses.value
            val newExpenses = currentExpenses.filter { it.id != expenseId }
            saveExpenses(newExpenses)
            _expenses.value = newExpenses
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 根据ID获取报销记录
     */
    fun getExpenseById(expenseId: String): ExpenseData? {
        return _expenses.value.find { it.id == expenseId }
    }

    /**
     * 获取所有报销记录
     */
    fun getAllExpenses(): List<ExpenseData> {
        return _expenses.value
    }

    /**
     * 根据用户ID获取报销记录
     */
    fun getExpensesByUserId(userId: String): List<ExpenseData> {
        return _expenses.value.filter { it.userId == userId }
    }

    /**
     * 根据条件查询报销记录
     */
    fun queryExpenses(
        userName: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        type: ExpenseType? = null
    ): List<ExpenseData> {
        return _expenses.value.filter { expense ->
            (userName.isNullOrBlank() || expense.userName.contains(userName, ignoreCase = true)) &&
            (startDate == null || expense.date >= startDate) &&
            (endDate == null || expense.date <= endDate) &&
            (type == null || expense.type == type)
        }
    }

    /**
     * 获取个人统计信息
     */
    fun getPersonalStats(userId: String): ExpenseStats {
        val userExpenses = getExpensesByUserId(userId)
        return ExpenseStats(
            count = userExpenses.size,
            totalAmount = userExpenses.sumOf { it.amount }
        )
    }

    /**
     * 获取团队统计信息
     */
    fun getTeamStats(): ExpenseStats {
        val allExpenses = getAllExpenses()
        return ExpenseStats(
            count = allExpenses.size,
            totalAmount = allExpenses.sumOf { it.amount }
        )
    }

    /**
     * 根据查询条件获取统计信息
     */
    fun getStatsWithFilter(
        userName: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        type: ExpenseType? = null
    ): Pair<ExpenseStats, ExpenseStats> {
        val filteredExpenses = queryExpenses(userName, startDate, endDate, type)

        // 个人统计（如果指定了用户名，则按用户名过滤，否则按当前用户）
        val personalExpenses = if (!userName.isNullOrBlank()) {
            filteredExpenses.filter { it.userName.contains(userName, ignoreCase = true) }
        } else if (currentUserId.isNotEmpty()) {
            filteredExpenses.filter { it.userId == currentUserId }
        } else {
            emptyList()
        }

        val personalStats = ExpenseStats(
            count = personalExpenses.size,
            totalAmount = personalExpenses.sumOf { it.amount }
        )

        val teamStats = ExpenseStats(
            count = filteredExpenses.size,
            totalAmount = filteredExpenses.sumOf { it.amount }
        )

        return Pair(personalStats, teamStats)
    }

    /**
     * 获取当前用户信息
     */
    fun getCurrentUserId(): String = currentUserId

    /**
     * 获取当前用户姓名（已废弃，现在使用成员选择）
     */
    @Deprecated("Use member selection instead")
    fun getCurrentUserName(): String {
        return CurrentUserManager.getCurrentUserName()
    }
}

/**
 * 统计数据类
 */
data class ExpenseStats(
    val count: Int,
    val totalAmount: Double
)
