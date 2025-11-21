package data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 报销类型枚举
 */
@Serializable
enum class ExpenseType(val displayName: String) {
    DINING("餐饮"),
    BEVERAGE("饮品"),
    TAXI("打车"),
    OFFICE_SUPPLIES("办公用具"),
    SERVICE_SUBSCRIPTION("服务订阅"),
    SOFTWARE_PURCHASE("软件购买"),
    OTHER("其他")
}

/**
 * 报销数据类
 */
@Serializable
data class ExpenseData(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,                      // 报销人员ID
    val userName: String,                    // 报销人员姓名
    val type: ExpenseType,                   // 报销类型
    val amount: Double,                      // 金额
    val date: Long,                          // 报销日期时间戳
    val receiptPath: String? = null,         // 报销凭证文件路径（图片或PDF）
    val receiptFileName: String? = null,     // 报销凭证文件名
    val remarks: String = "",                // 备注说明
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val createdBy: String                    // 创建者ID（可能与userId不同）
)



