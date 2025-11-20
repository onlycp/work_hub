package service

import data.FileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.File

/**
 * SFTP文件管理器
 */
class SFTPFileManager(private val sshClient: SSHClient) {

    private var sftpClient: SFTPClient? = null

    init {
        // 在构造函数中初始化 SFTP 客户端
        try {
            sftpClient = sshClient.newSFTPClient()
        } catch (e: Exception) {
            println("SFTP初始化失败: ${e.message}")
        }
    }

    suspend fun listFiles(path: String): Result<List<FileInfo>> = withContext(Dispatchers.IO) {
        try {
            val sftp = sftpClient ?: return@withContext Result.failure(Exception("SFTP未连接"))
            val files = sftp.ls(path).map { remote ->
                FileInfo(
                    name = remote.name,
                    path = remote.path,
                    isDirectory = remote.isDirectory,
                    size = remote.attributes.size,
                    modifiedTime = remote.attributes.mtime,
                    permissions = formatPermissions(remote.attributes.permissions)
                )
            }.sortedWith(compareBy<FileInfo> { !it.isDirectory }.thenBy { it.name })

            Result.success(files)
        } catch (e: Exception) {
            Result.failure(Exception("列出文件失败: ${e.message}", e))
        }
    }

    suspend fun downloadFile(remotePath: String, localPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = sftpClient ?: return@withContext Result.failure(Exception("SFTP未连接"))
            sftp.get(remotePath, localPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("下载文件失败: ${e.message}", e))
        }
    }

    suspend fun uploadFile(localPath: String, remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = sftpClient ?: return@withContext Result.failure(Exception("SFTP未连接"))
            sftp.put(localPath, remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("上传文件失败: ${e.message}", e))
        }
    }

    suspend fun deleteFile(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = sftpClient ?: return@withContext Result.failure(Exception("SFTP未连接"))

            // 先检查是否是目录
            val stat = sftp.stat(path)
            val isDirectory = java.io.File(path).isDirectory || stat.type == net.schmizz.sshj.sftp.FileMode.Type.DIRECTORY

            if (isDirectory) {
                // 递归删除目录
                deleteDirectoryRecursive(sftp, path)
            } else {
                // 删除文件
                sftp.rm(path)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("删除失败: ${e.message}", e))
        }
    }

    private fun deleteDirectoryRecursive(sftp: SFTPClient, path: String) {
        try {
            val files = sftp.ls(path)
            for (file in files) {
                if (file.name == "." || file.name == "..") continue

                val filePath = "$path/${file.name}"

                if (file.isDirectory) {
                    deleteDirectoryRecursive(sftp, filePath)
                } else {
                    sftp.rm(filePath)
                }
            }
            // 删除空目录
            sftp.rmdir(path)
        } catch (e: Exception) {
            // 尝试直接删除目录（适用于空目录）
            try {
                sftp.rmdir(path)
            } catch (dirException: Exception) {
                throw e
            }
        }
    }

    suspend fun createDirectory(path: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = sftpClient ?: return@withContext Result.failure(Exception("SFTP未连接"))
            sftp.mkdir(path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("创建目录失败: ${e.message}", e))
        }
    }

    suspend fun renameFile(oldPath: String, newPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = sftpClient ?: return@withContext Result.failure(Exception("SFTP未连接"))
            sftp.rename(oldPath, newPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("重命名失败: ${e.message}", e))
        }
    }

    suspend fun downloadDirectory(remotePath: String, localPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = sftpClient ?: return@withContext Result.failure(Exception("SFTP未连接"))
            val localDir = File(localPath)
            if (!localDir.exists()) {
                localDir.mkdirs()
            }

            // 递归下载文件夹
            downloadDirectoryRecursive(sftp, remotePath, localPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("下载文件夹失败: ${e.message}", e))
        }
    }

    private fun downloadDirectoryRecursive(sftp: SFTPClient, remotePath: String, localPath: String) {
        val files = sftp.ls(remotePath)
        for (file in files) {
            if (file.name == "." || file.name == "..") continue

            val remoteFilePath = "$remotePath/${file.name}"
            val localFilePath = "$localPath/${file.name}"

            if (file.isDirectory) {
                File(localFilePath).mkdirs()
                downloadDirectoryRecursive(sftp, remoteFilePath, localFilePath)
            } else {
                sftp.get(remoteFilePath, localFilePath)
            }
        }
    }

    suspend fun uploadDirectory(localPath: String, remotePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sftp = sftpClient ?: return@withContext Result.failure(Exception("SFTP未连接"))
            val localDir = File(localPath)
            if (!localDir.exists() || !localDir.isDirectory) {
                return@withContext Result.failure(Exception("本地文件夹不存在"))
            }

            // 创建远程文件夹
            try {
                sftp.mkdir(remotePath)
            } catch (e: Exception) {
                // 文件夹可能已存在，忽略错误
            }

            // 递归上传文件夹
            uploadDirectoryRecursive(sftp, localPath, remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("上传文件夹失败: ${e.message}", e))
        }
    }

    private fun uploadDirectoryRecursive(sftp: SFTPClient, localPath: String, remotePath: String) {
        val localDir = File(localPath)
        val files = localDir.listFiles() ?: return

        for (file in files) {
            val remoteFilePath = "$remotePath/${file.name}"

            if (file.isDirectory) {
                try {
                    sftp.mkdir(remoteFilePath)
                } catch (e: Exception) {
                    // 文件夹可能已存在，忽略错误
                }
                uploadDirectoryRecursive(sftp, file.absolutePath, remoteFilePath)
            } else {
                sftp.put(file.absolutePath, remoteFilePath)
            }
        }
    }

    fun disconnect() {
        try {
            sftpClient?.close()
            sftpClient = null
        } catch (e: Exception) {
            println("SFTP断开连接失败: ${e.message}")
        }
    }

    // 格式化权限为字符串（如：rwxr-xr-x）
    private fun formatPermissions(perms: Set<net.schmizz.sshj.xfer.FilePermission>): String {
        val result = StringBuilder()

        // 用户权限
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.USR_R)) 'r' else '-')
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.USR_W)) 'w' else '-')
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.USR_X)) 'x' else '-')

        // 组权限
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.GRP_R)) 'r' else '-')
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.GRP_W)) 'w' else '-')
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.GRP_X)) 'x' else '-')

        // 其他用户权限
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.OTH_R)) 'r' else '-')
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.OTH_W)) 'w' else '-')
        result.append(if (perms.contains(net.schmizz.sshj.xfer.FilePermission.OTH_X)) 'x' else '-')

        return result.toString()
    }
}
