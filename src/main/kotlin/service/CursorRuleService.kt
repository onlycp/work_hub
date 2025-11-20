package service

import data.CursorRuleManager
import data.CursorRuleData
import data.CursorPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.*

/**
 * Cursor规则服务类
 */
object CursorRuleService {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * 创建新规则
     */
    suspend fun createRule(
        name: String,
        platforms: List<CursorPlatform>,
        content: String,
        userId: String
    ): Result<CursorRuleData> {
        return withContext(Dispatchers.IO) {
            try {
                val rule = CursorRuleData(
                    name = name,
                    platforms = platforms,
                    versions = listOf(
                        data.CursorRuleVersion(1, content, System.currentTimeMillis())
                    ),
                    createdBy = userId,
                    isShared = false
                )
                CursorRuleManager.addRule(rule).map { rule }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 更新规则内容（草稿）
     */
    suspend fun updateRuleContent(
        ruleId: String,
        content: String
    ): Result<CursorRuleData> {
        return withContext(Dispatchers.IO) {
            try {
                val rule = CursorRuleManager.getRuleById(ruleId)
                    ?: return@withContext Result.failure(Exception("规则不存在"))

                val updatedRule = rule.copy(
                    versions = rule.versions.map {
                        if (it.version == rule.currentVersion) {
                            it.copy(content = content)
                        } else {
                            it
                        }
                    },
                    lastModified = System.currentTimeMillis()
                )

                CursorRuleManager.updateRule(updatedRule).map { updatedRule }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 发布新版本
     */
    suspend fun publishNewVersion(
        ruleId: String,
        content: String,
        publishedBy: String = "未知用户"
    ): Result<CursorRuleData> {
        return withContext(Dispatchers.IO) {
            CursorRuleManager.publishNewVersion(ruleId, content, publishedBy)
        }
    }

    /**
     * 删除规则
     */
    suspend fun deleteRule(ruleId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            CursorRuleManager.deleteRule(ruleId)
        }
    }

    /**
     * 下载规则文件到指定路径
     */
    suspend fun downloadRuleFile(
        rule: CursorRuleData,
        savePath: String,
        version: Int? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val content = if (version != null) {
                    rule.getVersionContent(version)
                } else {
                    rule.getCurrentContent()
                }

                val fileName = ".cursorrules"
                val targetDir = File(savePath)

                // 确保目录存在
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                val targetFile = File(targetDir, fileName)

                // 如果文件已存在，添加时间戳避免覆盖
                var finalFile = targetFile
                var counter = 1
                while (finalFile.exists()) {
                    val baseName = fileName.substringBeforeLast(".")
                    val extension = fileName.substringAfterLast(".", "")
                    finalFile = if (extension.isNotEmpty()) {
                        File(targetDir, "${baseName}_${counter}.${extension}")
                    } else {
                        File(targetDir, "${baseName}_${counter}")
                    }
                    counter++
                }

                finalFile.writeText(content, Charsets.UTF_8)

                // 尝试打开文件所在目录
                try {
                    Desktop.getDesktop().open(targetDir)
                } catch (e: Exception) {
                    println("无法打开保存目录: ${e.message}")
                }

                Result.success("文件已保存到: ${finalFile.absolutePath}")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * 下载规则文件到默认下载目录（向后兼容）
     */
    suspend fun downloadRuleFile(
        rule: CursorRuleData,
        version: Int? = null
    ): Result<String> {
        val downloadsDir = File(System.getProperty("user.home"), "Downloads").absolutePath
        return downloadRuleFile(rule, downloadsDir, version)
    }

    /**
     * 格式化时间戳
     */
    fun formatTimestamp(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * 获取平台显示名称
     */
    fun getPlatformDisplayNames(platforms: List<CursorPlatform>): String {
        return platforms.joinToString(", ") { getPlatformDisplayName(it) }
    }

    /**
     * 获取单个平台显示名称
     */
    fun getPlatformDisplayName(platform: CursorPlatform): String {
        return when (platform) {
            CursorPlatform.APP -> "APP"
            CursorPlatform.PC -> "PC"
            CursorPlatform.WEB -> "WEB"
        }
    }

    /**
     * 检查规则名称是否存在
     */
    fun isRuleNameExists(name: String, excludeId: String? = null): Boolean {
        return CursorRuleManager.isRuleNameExists(name, excludeId)
    }
}
