package data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Cursor规则平台枚举
 */
@Serializable
enum class CursorPlatform {
    APP, PC, WEB
}

/**
 * Cursor规则版本数据类
 */
@Serializable
data class CursorRuleVersion(
    val version: Int,
    val content: String, // markdown格式的规则内容
    val publishedAt: Long = System.currentTimeMillis(),
    val publishedBy: String = "未知用户"
)

/**
 * Cursor规则数据类
 */
@Serializable
data class CursorRuleData(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val platforms: List<CursorPlatform> = emptyList(),
    val currentVersion: Int = 1,
    val versions: List<CursorRuleVersion> = listOf(
        CursorRuleVersion(1, "", System.currentTimeMillis())
    ),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    // 创建者ID - 只有创建者能编辑
    val createdBy: String,
    // 是否共享给其他用户查看
    val isShared: Boolean = false
) {
    /**
     * 获取当前版本的规则内容
     */
    fun getCurrentContent(): String {
        return versions.find { it.version == currentVersion }?.content ?: ""
    }

    /**
     * 获取指定版本的规则内容
     */
    fun getVersionContent(version: Int): String {
        return versions.find { it.version == version }?.content ?: ""
    }

    /**
     * 获取指定版本的发布时间
     */
    fun getVersionPublishTime(version: Int): Long {
        return versions.find { it.version == version }?.publishedAt ?: 0L
    }

    /**
     * 发布新版本
     */
    fun publishNewVersion(content: String, publishedBy: String = "未知用户"): CursorRuleData {
        val newVersion = currentVersion + 1
        val newVersionData = CursorRuleVersion(
            version = newVersion,
            content = content,
            publishedAt = System.currentTimeMillis(),
            publishedBy = publishedBy
        )

        return copy(
            currentVersion = newVersion,
            versions = versions + newVersionData,
            lastModified = System.currentTimeMillis()
        )
    }
}


