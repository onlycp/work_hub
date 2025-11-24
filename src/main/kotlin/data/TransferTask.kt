package data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * 传输类型枚举
 */
enum class TransferType {
    UPLOAD,    // 上传
    DOWNLOAD   // 下载
}

/**
 * 传输状态枚举
 */
enum class TransferStatus {
    PENDING,   // 等待中
    RUNNING,   // 传输中
    COMPLETED, // 已完成
    FAILED,    // 失败
    CANCELLED  // 已取消
}

/**
 * 传输任务数据类
 */
data class TransferTask(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,           // 本地或远程路径
    val remotePath: String? = null, // 远程路径（上传时使用）
    val localPath: String? = null,  // 本地路径（下载时使用）
    val type: TransferType,
    val status: TransferStatus = TransferStatus.PENDING,
    val totalSize: Long = 0L,       // 总大小（字节）
    val transferredSize: Long = 0L, // 已传输大小（字节）
    val speed: Long = 0L,           // 传输速度（字节/秒）
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val errorMessage: String? = null,
    val isDirectory: Boolean = false
) {
    /**
     * 获取传输进度（0-100）
     */
    val progress: Float
        get() = if (totalSize > 0) {
            (transferredSize.toFloat() / totalSize.toFloat() * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

    /**
     * 格式化文件大小
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * 格式化传输速度
     */
    fun formatSpeed(): String {
        return when {
            speed < 1024 -> "$speed B/s"
            speed < 1024 * 1024 -> "${speed / 1024} KB/s"
            else -> "${speed / (1024 * 1024)} MB/s"
        }
    }
}


