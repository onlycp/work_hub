package ui.ops

import data.FileInfo
import data.TransferManager
import data.TransferTask
import data.TransferType
import data.TransferStatus
import service.SFTPFileManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import theme.*
import java.io.File
import javax.swing.JFileChooser
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun SFTPBrowser(
    sftpManager: SFTPFileManager?,
    modifier: Modifier = Modifier
) {
    var currentPath by remember { mutableStateOf("/root") }
    var files by remember { mutableStateOf<List<FileInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFile by remember { mutableStateOf<FileInfo?>(null) }
    var viewMode by remember { mutableStateOf("list") } // "list" 或 "grid"
    var showFileDetails by remember { mutableStateOf(false) }
    var detailFile by remember { mutableStateOf<FileInfo?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameFile by remember { mutableStateOf<FileInfo?>(null) }
    var showTransferList by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    
    // 安全地获取活跃任务数量，避免并发修改异常
    val tasksState = TransferManager.tasks.collectAsState()
    // 使用 derivedStateOf 确保线程安全
    val activeTasks = remember {
        derivedStateOf {
            // 创建列表快照以避免并发修改
            tasksState.value.toList().filter { 
                it.status == TransferStatus.PENDING || it.status == TransferStatus.RUNNING
            }
        }
    }.value
    
    val activeTaskCount = activeTasks.size
    
    // 计算总进度（所有活跃任务的平均进度）
    val overallProgress = remember(activeTasks) {
        if (activeTasks.isEmpty()) {
            0f
        } else {
            activeTasks.map { it.progress }.average().toFloat()
        }
    }

    // 加载文件列表
    fun loadFiles() {
        if (sftpManager == null) {
            errorMessage = "SFTP未连接"
            return
        }

        scope.launch {
            isLoading = true
            errorMessage = null
            val result = sftpManager.listFiles(currentPath)
            result.fold(
                onSuccess = { fileList ->
                    files = fileList
                    isLoading = false
                },
                onFailure = { error ->
                    errorMessage = error.message
                    isLoading = false
                }
            )
        }
    }

    // 初始加载
    LaunchedEffect(sftpManager, currentPath) {
        if (sftpManager != null) {
            loadFiles()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Surface)
    ) {
        // 紧凑型工具栏
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppColors.SurfaceVariant,
            elevation = AppDimensions.ElevationXS
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮 + 路径
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            if (currentPath != "/") {
                                currentPath = File(currentPath).parent ?: "/"
                            }
                        },
                        enabled = currentPath != "/",
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = if (currentPath != "/") AppColors.Primary else AppColors.TextDisabled,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = currentPath,
                        style = AppTypography.Caption,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp),
                        maxLines = 1
                    )
                }

                // 操作按钮组
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 传输列表按钮
                    Box {
                        IconButton(
                            onClick = { showTransferList = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            // 使用上下箭头叠加的图标效果，带进度显示
                            Box(
                                modifier = Modifier.size(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // 如果有活跃任务，显示圆形进度条
                                if (activeTaskCount > 0 && overallProgress > 0f) {
                                    CircularProgressIndicator(
                                        progress = overallProgress / 100f,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = AppColors.Primary,
                                        backgroundColor = AppColors.Divider
                                    )
                                }
                                
                                // 上箭头
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = AppColors.Primary,
                                    modifier = Modifier
                                        .size(10.dp)
                                        .offset(y = (-3).dp)
                                )
                                // 下箭头
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "传输列表",
                                    tint = AppColors.Success,
                                    modifier = Modifier
                                        .size(10.dp)
                                        .offset(y = 3.dp)
                                )
                            }
                        }
                        // 活跃任务数量徽章
                        if (activeTaskCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .offset(x = 12.dp, y = (-4).dp)
                                    .background(
                                        AppColors.Error,
                                        shape = androidx.compose.foundation.shape.CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (activeTaskCount > 9) "9+" else "$activeTaskCount",
                                    style = AppTypography.Caption,
                                    color = Color.White,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }

                    // 视图切换按钮
                    IconButton(
                        onClick = { viewMode = if (viewMode == "list") "grid" else "list" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (viewMode == "list") Icons.Default.GridView else Icons.Default.List,
                            contentDescription = if (viewMode == "list") "切换到图标视图" else "切换到列表视图",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 上传文件按钮
                    IconButton(
                        onClick = {
                            // 文件选择在UI线程执行
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val fileChooser = JFileChooser()
                                    val result = withContext(Dispatchers.Main) {
                                        fileChooser.showOpenDialog(null)
                                    }
                                    
                                    if (result == JFileChooser.APPROVE_OPTION) {
                                        val localFile = fileChooser.selectedFile
                                        val remotePath = "$currentPath/${localFile.name}"
                                        
                                        // 创建传输任务
                                        val task = TransferTask(
                                            fileName = localFile.name,
                                            filePath = localFile.absolutePath,
                                            remotePath = remotePath,
                                            type = TransferType.UPLOAD,
                                            status = TransferStatus.PENDING,
                                            totalSize = localFile.length(),
                                            isDirectory = localFile.isDirectory
                                        )
                                        TransferManager.addTask(task)
                                        
                                        // 更新任务状态为运行中
                                        TransferManager.updateTask(task.id) { 
                                            it.copy(status = TransferStatus.RUNNING)
                                        }
                                        
                                        // 在后台协程中执行上传，不阻塞UI
                                        launch(Dispatchers.IO) {
                                            val startTime = System.currentTimeMillis()
                                            var lastUpdateTime = startTime
                                            var lastTransferred = 0L
                                            
                                            sftpManager?.uploadFile(
                                                localFile.absolutePath,
                                                remotePath
                                            ) { transferred, total ->
                                                // 更新进度
                                                val currentTime = System.currentTimeMillis()
                                                val timeDelta = (currentTime - lastUpdateTime).coerceAtLeast(50) // 降低到50ms

                                                // 总是更新传输大小，但控制速度更新频率
                                                TransferManager.updateTask(task.id) {
                                                    it.copy(transferredSize = transferred)
                                                }

                                                if (timeDelta >= 100) { // 每100ms更新速度
                                                    val speed = ((transferred - lastTransferred) * 1000) / timeDelta
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(speed = speed)
                                                    }
                                                    lastUpdateTime = currentTime
                                                    lastTransferred = transferred
                                                }
                                            }?.fold(
                                                onSuccess = {
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(
                                                            status = TransferStatus.COMPLETED,
                                                            transferredSize = it.totalSize,
                                                            speed = 0L,
                                                            endTime = System.currentTimeMillis()
                                                        )
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        loadFiles()
                                                    }
                                                },
                                                onFailure = { error ->
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(
                                                            status = TransferStatus.FAILED,
                                                            errorMessage = error.message,
                                                            endTime = System.currentTimeMillis()
                                                        )
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        errorMessage = error.message
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "上传失败: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "上传文件",
                            tint = AppColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 上传文件夹按钮
                    IconButton(
                        onClick = {
                            // 文件选择在UI线程执行
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val fileChooser = JFileChooser()
                                    fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                    fileChooser.dialogTitle = "选择要上传的文件夹"
                                    val result = withContext(Dispatchers.Main) {
                                        fileChooser.showOpenDialog(null)
                                    }
                                    
                                    if (result == JFileChooser.APPROVE_OPTION) {
                                        val localFolder = fileChooser.selectedFile
                                        val remotePath = "$currentPath/${localFolder.name}"
                                        
                                        // 在后台计算文件夹大小
                                        val folderSize = withContext(Dispatchers.IO) {
                                            calculateFolderSize(localFolder)
                                        }
                                        
                                        // 创建传输任务
                                        val task = TransferTask(
                                            fileName = localFolder.name,
                                            filePath = localFolder.absolutePath,
                                            remotePath = remotePath,
                                            type = TransferType.UPLOAD,
                                            status = TransferStatus.PENDING,
                                            totalSize = folderSize,
                                            isDirectory = true
                                        )
                                        TransferManager.addTask(task)
                                        
                                        // 更新任务状态为运行中
                                        TransferManager.updateTask(task.id) {
                                            it.copy(status = TransferStatus.RUNNING)
                                        }
                                        
                                        // 在后台协程中执行上传，不阻塞UI
                                        launch(Dispatchers.IO) {
                                            sftpManager?.uploadDirectory(localFolder.absolutePath, remotePath)?.fold(
                                                onSuccess = {
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(
                                                            status = TransferStatus.COMPLETED,
                                                            transferredSize = it.totalSize,
                                                            endTime = System.currentTimeMillis()
                                                        )
                                                    }
                                                    println("✓ 文件夹上传完成: ${localFolder.name}")
                                                    withContext(Dispatchers.Main) {
                                                        loadFiles()
                                                    }
                                                },
                                                onFailure = { error ->
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(
                                                            status = TransferStatus.FAILED,
                                                            errorMessage = error.message,
                                                            endTime = System.currentTimeMillis()
                                                        )
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        errorMessage = error.message
                                                    }
                                                }
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "上传失败: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "上传文件夹",
                            tint = AppColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 刷新按钮
                    IconButton(
                        onClick = { loadFiles() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = AppColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Divider(color = AppColors.Divider)

        // 错误提示
        if (errorMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AppColors.Error.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(AppDimensions.SpaceM),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "错误",
                        tint = AppColors.Error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpaceS))
                    Text(
                        text = errorMessage!!,
                        style = AppTypography.Caption,
                        color = AppColors.Error
                    )
                }
            }
        }

        // 文件列表
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = AppColors.Primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        } else if (sftpManager == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "未连接",
                        tint = AppColors.TextDisabled,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                    Text(
                        text = "请先连接SSH",
                        style = AppTypography.BodyLarge,
                        color = AppColors.TextSecondary
                    )
                }
            }
        } else {
            // 文件操作回调
            val fileOperations = FileOperations(
                sftpManager = sftpManager,
                scope = scope,
                onLoadingChange = { isLoading = it },
                onErrorChange = { errorMessage = it },
                onReload = { loadFiles() }
            )

            if (viewMode == "list") {
                // 列表视图
                FileListView(
                    files = files,
                    selectedFile = selectedFile,
                    onFileClick = { file ->
                        selectedFile = file
                        if (file.isDirectory) {
                            currentPath = file.path
                        }
                    },
                    onShowDetails = { file ->
                        detailFile = file
                        showFileDetails = true
                    },
                    onRename = { file ->
                        renameFile = file
                        showRenameDialog = true
                    },
                    operations = fileOperations
                )
            } else {
                // 图标网格视图
                FileGridView(
                    files = files,
                    selectedFile = selectedFile,
                    onFileClick = { file ->
                        selectedFile = file
                        if (file.isDirectory) {
                            currentPath = file.path
                        }
                    },
                    onShowDetails = { file ->
                        detailFile = file
                        showFileDetails = true
                    },
                    onRename = { file ->
                        renameFile = file
                        showRenameDialog = true
                    },
                    operations = fileOperations
                )
            }
        }

        // 文件详情对话框
        if (showFileDetails && detailFile != null) {
            FileDetailDialog(
                file = detailFile!!,
                onDismiss = {
                    showFileDetails = false
                    detailFile = null
                }
            )
        }

        // 重命名对话框
        if (showRenameDialog && renameFile != null) {
            RenameDialog(
                file = renameFile!!,
                sftpManager = sftpManager!!,
                onDismiss = {
                    showRenameDialog = false
                    renameFile = null
                },
                onSuccess = {
                    showRenameDialog = false
                    renameFile = null
                    loadFiles()
                }
            )
        }

        // 传输列表对话框
        if (showTransferList) {
            TransferListDialog(
                onDismiss = { showTransferList = false }
            )
        }
    }
}

/**
 * 计算文件夹大小
 */
private fun calculateFolderSize(folder: File): Long {
    var size = 0L
    if (folder.isDirectory) {
        folder.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateFolderSize(file)
            } else {
                file.length()
            }
        }
    } else {
        size = folder.length()
    }
    return size
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

// 文件操作封装类
data class FileOperations(
    val sftpManager: SFTPFileManager,
    val scope: kotlinx.coroutines.CoroutineScope,
    val onLoadingChange: (Boolean) -> Unit,
    val onErrorChange: (String?) -> Unit,
    val onReload: () -> Unit
)

// 列表视图
@Composable
fun FileListView(
    files: List<FileInfo>,
    selectedFile: FileInfo?,
    onFileClick: (FileInfo) -> Unit,
    onShowDetails: (FileInfo) -> Unit,
    onRename: (FileInfo) -> Unit,
    operations: FileOperations
) {
    // 表头
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.SurfaceVariant,
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "名称",
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                text = "大小",
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary,
                modifier = Modifier.weight(0.2f)
            )
            Text(
                text = "修改时间",
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary,
                modifier = Modifier.weight(0.3f)
            )
            Text(
                text = "权限",
                fontWeight = FontWeight.Medium,
                color = AppColors.TextSecondary,
                modifier = Modifier.weight(0.1f)
            )
        }
    }

    Divider(color = AppColors.Divider)

    // 文件列表
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(files) { file ->
            FileListItem(
                file = file,
                isSelected = selectedFile == file,
                onFileClick = onFileClick,
                onShowDetails = onShowDetails,
                onRename = onRename,
                operations = operations
            )
        }
    }
}

// 列表项组件
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FileListItem(
    file: FileInfo,
    isSelected: Boolean,
    onFileClick: (FileInfo) -> Unit,
    onShowDetails: (FileInfo) -> Unit,
    onRename: (FileInfo) -> Unit,
    operations: FileOperations
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .onPointerEvent(PointerEventType.Press) { event ->
                if (event.button == PointerButton.Secondary) {
                    showContextMenu = true
                }
            }
            .clickable { onFileClick(file) },
        color = if (isSelected) AppColors.Primary.copy(alpha = 0.06f) else AppColors.Surface
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 名称
                Row(
                    modifier = Modifier.weight(0.4f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = getFileIcon(file),
                        contentDescription = null,
                        tint = getFileIconTint(file),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = file.name,
                        style = AppTypography.Caption,
                        color = AppColors.TextPrimary,
                        fontWeight = if (file.isDirectory) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1
                    )
                }

                // 大小
                Text(
                    text = if (file.isDirectory) "--" else formatFileSize(file.size),
                    color = AppColors.TextSecondary,
                    modifier = Modifier.weight(0.2f)
                )

                // 修改时间
                Text(
                    text = formatModifiedTime(file.modifiedTime),
                    color = AppColors.TextSecondary,
                    modifier = Modifier.weight(0.3f)
                )

                // 权限
                Text(
                    text = file.permissions,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.weight(0.1f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            // 右键菜单（文件和文件夹都支持）
            if (showContextMenu) {
                FileContextMenu(
                    file = file,
                    onDismiss = { showContextMenu = false },
                    onEdit = {
                        showContextMenu = false
                        operations.scope.launch {
                            try {
                                val tempDir = System.getProperty("java.io.tmpdir")
                                val tempFile = File(tempDir, file.name)
                                operations.onLoadingChange(true)

                                operations.sftpManager.downloadFile(file.path, tempFile.absolutePath).fold(
                                    onSuccess = {
                                        operations.onLoadingChange(false)
                                        try {
                                            java.awt.Desktop.getDesktop().open(tempFile)

                                            // 文件监听逻辑
                                            operations.scope.launch {
                                                try {
                                                    val originalModified = tempFile.lastModified()
                                                    kotlinx.coroutines.delay(2000)

                                                    var keepWatching = true
                                                    while (keepWatching) {
                                                        kotlinx.coroutines.delay(1000)
                                                        if (tempFile.lastModified() > originalModified && tempFile.exists()) {
                                                            operations.sftpManager.uploadFile(tempFile.absolutePath, file.path).fold(
                                                                onSuccess = {
                                                                    println("✓ 文件已自动保存: ${file.name}")
                                                                    keepWatching = false
                                                                },
                                                                onFailure = {
                                                                    println("✗ 自动上传失败")
                                                                    keepWatching = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    println("文件监听出错: ${e.message}")
                                                }
                                            }
                                        } catch (e: Exception) {
                                            operations.onErrorChange("无法打开文件: ${e.message}")
                                        }
                                    },
                                    onFailure = { error ->
                                        operations.onErrorChange(error.message)
                                        operations.onLoadingChange(false)
                                    }
                                )
                            } catch (e: Exception) {
                                operations.onErrorChange("编辑失败: ${e.message}")
                                operations.onLoadingChange(false)
                            }
                        }
                    },
                    onDownload = {
                        showContextMenu = false
                        // 先显示文件选择对话框，然后再启动协程执行下载
                        try {
                            val fileChooser = JFileChooser()
                            if (file.isDirectory) {
                                fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                                fileChooser.dialogTitle = "选择保存文件夹的位置"
                            } else {
                                fileChooser.selectedFile = File(file.name)
                            }
                            val dialogResult = fileChooser.showSaveDialog(null)
                            if (dialogResult == JFileChooser.APPROVE_OPTION) {
                                val selectedFile = fileChooser.selectedFile
                                val localPath = selectedFile.absolutePath

                                // 对于文件下载，如果用户选择了目录，需要添加文件名
                                val targetPath = if (selectedFile.isDirectory) {
                                    "$localPath/${file.name}"
                                } else {
                                    localPath
                                }

                                // 在后台协程中执行下载，不阻塞UI
                                operations.scope.launch(Dispatchers.IO) {
                                    try {
                                        // 创建传输任务
                                        val task = TransferTask(
                                            fileName = file.name,
                                            filePath = file.path,
                                            localPath = targetPath,
                                            type = TransferType.DOWNLOAD,
                                            status = TransferStatus.PENDING,
                                            totalSize = file.size,
                                            isDirectory = file.isDirectory
                                        )
                                        TransferManager.addTask(task)

                                        // 更新任务状态为运行中
                                        TransferManager.updateTask(task.id) {
                                            it.copy(status = TransferStatus.RUNNING)
                                        }

                                        val startTime = System.currentTimeMillis()
                                        var lastUpdateTime = startTime
                                        var lastTransferred = 0L

                                        if (file.isDirectory) {
                                            // 对于文件夹下载，确保目标路径包含文件夹名称
                                            operations.sftpManager.downloadDirectory(file.path, targetPath).fold(
                                                onSuccess = {
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(
                                                            status = TransferStatus.COMPLETED,
                                                            transferredSize = it.totalSize,
                                                            endTime = System.currentTimeMillis()
                                                        )
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        operations.onErrorChange(null)
                                                    }
                                                    println("✓ 文件夹下载完成: ${file.name}")
                                                },
                                                onFailure = { error ->
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(
                                                            status = TransferStatus.FAILED,
                                                            errorMessage = error.message,
                                                            endTime = System.currentTimeMillis()
                                                        )
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        operations.onErrorChange(error.message)
                                                    }
                                                }
                                            )
                                        } else {
                                            operations.sftpManager.downloadFile(
                                                file.path,
                                                targetPath
                                            ) { transferred, total ->
                                                // 更新进度
                                                val currentTime = System.currentTimeMillis()
                                                val timeDelta = (currentTime - lastUpdateTime).coerceAtLeast(50) // 降低到50ms

                                                // 总是更新传输大小，但控制速度更新频率
                                                TransferManager.updateTask(task.id) {
                                                    it.copy(transferredSize = transferred)
                                                }

                                                if (timeDelta >= 100) { // 每100ms更新速度
                                                    val speed = ((transferred - lastTransferred) * 1000) / timeDelta
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(speed = speed)
                                                    }
                                                    lastUpdateTime = currentTime
                                                    lastTransferred = transferred
                                                }
                                            }.fold(
                                                onSuccess = {
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(
                                                            status = TransferStatus.COMPLETED,
                                                            transferredSize = it.totalSize,
                                                            speed = 0L,
                                                            endTime = System.currentTimeMillis()
                                                        )
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        operations.onErrorChange(null)
                                                    }
                                                },
                                                onFailure = { error ->
                                                    TransferManager.updateTask(task.id) {
                                                        it.copy(
                                                            status = TransferStatus.FAILED,
                                                            errorMessage = error.message,
                                                            endTime = System.currentTimeMillis()
                                                        )
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        operations.onErrorChange(error.message)
                                                    }
                                                }
                                            )
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            operations.onErrorChange("下载失败: ${e.message}")
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            operations.onErrorChange("选择保存位置失败: ${e.message}")
                        }
                    },
                    onRename = {
                        showContextMenu = false
                        onRename(file)
                    },
                    onDelete = {
                        showContextMenu = false
                        operations.scope.launch {
                            try {
                                operations.onLoadingChange(true)
                                operations.sftpManager.deleteFile(file.path).fold(
                                    onSuccess = {
                                        operations.onReload()
                                    },
                                    onFailure = { error ->
                                        operations.onErrorChange(error.message)
                                        operations.onLoadingChange(false)
                                    }
                                )
                            } catch (e: Exception) {
                                operations.onErrorChange("删除失败: ${e.message}")
                                operations.onLoadingChange(false)
                            }
                        }
                    },
                    onShowDetails = {
                        showContextMenu = false
                        onShowDetails(file)
                    }
                )
            }
        }
    }

    Divider(color = AppColors.Divider.copy(alpha = 0.3f), thickness = 0.5.dp)
}

// 右键菜单
@Composable
fun FileContextMenu(
    file: FileInfo,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShowDetails: () -> Unit
) {
    androidx.compose.material.DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        // 编辑（仅文件）
        if (!file.isDirectory) {
            androidx.compose.material.DropdownMenuItem(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = AppColors.Success)
                Spacer(modifier = Modifier.width(8.dp))
                Text("编辑", style = AppTypography.Caption)
            }
        }

        // 下载（文件和文件夹）
        androidx.compose.material.DropdownMenuItem(onClick = onDownload) {
            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp), tint = AppColors.Primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (file.isDirectory) "下载文件夹" else "下载", style = AppTypography.Caption)
        }

        // 重命名（所有文件）
        androidx.compose.material.DropdownMenuItem(onClick = onRename) {
            Icon(Icons.Default.DriveFileRenameOutline, null, modifier = Modifier.size(16.dp), tint = AppColors.Warning)
            Spacer(modifier = Modifier.width(8.dp))
            Text("重命名", style = AppTypography.Caption)
        }

        Divider()

        // 拷贝文件名（所有文件）
        androidx.compose.material.DropdownMenuItem(onClick = {
            try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(file.name)
                clipboard.setContents(selection, null)
                onDismiss()
            } catch (e: Exception) {
                // 拷贝失败时不显示错误，因为用户可能不关心
                onDismiss()
            }
        }) {
            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp), tint = AppColors.TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("拷贝文件名", style = AppTypography.Caption)
        }

        // 拷贝路径（所有文件）
        androidx.compose.material.DropdownMenuItem(onClick = {
            try {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                val selection = StringSelection(file.path)
                clipboard.setContents(selection, null)
                onDismiss()
            } catch (e: Exception) {
                // 拷贝失败时不显示错误，因为用户可能不关心
                onDismiss()
            }
        }) {
            Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp), tint = AppColors.TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("拷贝路径", style = AppTypography.Caption)
        }

        Divider()

        // 详细信息（所有文件）
        androidx.compose.material.DropdownMenuItem(onClick = onShowDetails) {
            Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = AppColors.TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("详细信息", style = AppTypography.Caption)
        }

        Divider()

        // 删除（所有文件）
        androidx.compose.material.DropdownMenuItem(onClick = onDelete) {
            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = AppColors.Error)
            Spacer(modifier = Modifier.width(8.dp))
            Text("删除", style = AppTypography.Caption, color = AppColors.Error)
        }
    }
}

// 文件详细信息对话框
@Composable
fun FileDetailDialog(
    file: FileInfo,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            color = AppColors.Surface,
            elevation = AppDimensions.ElevationMedium
        ) {
            Column(
                modifier = Modifier
                    .width(400.dp)
                    .padding(AppDimensions.SpaceL)
            ) {
                // 标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "文件详细信息",
                        style = AppTypography.TitleMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = AppColors.TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))
                Divider(color = AppColors.Divider)
                Spacer(modifier = Modifier.height(AppDimensions.SpaceM))

                // 详细信息
                DetailRow("文件名", file.name)
                DetailRow("路径", file.path)
                DetailRow("类型", if (file.isDirectory) "文件夹" else "文件")
                if (!file.isDirectory) {
                    DetailRow("大小", formatFileSize(file.size))
                }
                DetailRow("修改时间", formatModifiedTime(file.modifiedTime))
                DetailRow("权限", file.permissions)

                Spacer(modifier = Modifier.height(AppDimensions.SpaceL))

                // 关闭按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppColors.Primary
                    )
                ) {
                    Text("关闭", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = "$label：",
            style = AppTypography.BodySmall,
            color = AppColors.TextSecondary,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = AppTypography.BodySmall,
            color = AppColors.TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}


// 获取文件图标的工具函数
private fun getFileIcon(file: FileInfo): androidx.compose.ui.graphics.vector.ImageVector {
    return if (file.isDirectory) {
        Icons.Default.Folder
    } else {
        val extension = file.name.substringAfterLast('.', "").lowercase()
        when (extension) {
            // 图片文件
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico" -> Icons.Default.Image
            // 视频文件
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> Icons.Default.Videocam
            // 音频文件
            "mp3", "wav", "flac", "aac", "ogg", "m4a" -> Icons.Default.Audiotrack
            // 文档文件
            "pdf" -> Icons.Default.PictureAsPdf
            "doc", "docx" -> Icons.Default.Description
            "xls", "xlsx" -> Icons.Default.TableChart
            "ppt", "pptx" -> Icons.Default.Slideshow
            // 代码文件
            "kt", "java", "py", "js", "ts", "html", "css", "xml", "json", "yml", "yaml" -> Icons.Default.Code
            "sh", "bat", "cmd" -> Icons.Default.Terminal
            // 压缩文件
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> Icons.Default.Archive
            // 文本文件
            "txt", "md", "log" -> Icons.Default.TextSnippet
            // 默认文件
            else -> Icons.Default.InsertDriveFile
        }
    }
}

// 获取文件图标颜色的工具函数
private fun getFileIconTint(file: FileInfo): androidx.compose.ui.graphics.Color {
    return if (file.isDirectory) {
        AppColors.Warning
    } else {
        val extension = file.name.substringAfterLast('.', "").lowercase()
        when (extension) {
            // 图片文件 - 绿色
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico" -> AppColors.Success
            // 视频文件 - 红色
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> AppColors.Error
            // 音频文件 - 紫色
            "mp3", "wav", "flac", "aac", "ogg", "m4a" -> AppColors.Primary.copy(alpha = 0.8f)
            // 文档文件 - 蓝色
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> AppColors.Info ?: AppColors.Primary
            // 代码文件 - 青色
            "kt", "java", "py", "js", "ts", "html", "css", "xml", "json", "yml", "yaml" -> AppColors.Success.copy(alpha = 0.8f)
            // 脚本文件 - 橙色
            "sh", "bat", "cmd" -> AppColors.Warning.copy(alpha = 0.8f)
            // 压缩文件 - 灰色
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> AppColors.TextSecondary
            // 文本文件 - 深灰色
            "txt", "md", "log" -> AppColors.TextPrimary
            // 默认文件 - 灰色
            else -> AppColors.TextSecondary
        }
    }
}


// 图标网格视图
@Composable
fun FileGridView(
    files: List<FileInfo>,
    selectedFile: FileInfo?,
    onFileClick: (FileInfo) -> Unit,
    onShowDetails: (FileInfo) -> Unit,
    onRename: (FileInfo) -> Unit,
    operations: FileOperations
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 90.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files) { file ->
            FileGridItem(
                file = file,
                isSelected = selectedFile == file,
                onFileClick = onFileClick,
                onShowDetails = onShowDetails,
                onRename = onRename,
                operations = operations
            )
        }
    }
}

// 图标网格项
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FileGridItem(
    file: FileInfo,
    isSelected: Boolean,
    onFileClick: (FileInfo) -> Unit,
    onShowDetails: (FileInfo) -> Unit,
    onRename: (FileInfo) -> Unit,
    operations: FileOperations
) {
    var showContextMenu by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .onPointerEvent(PointerEventType.Press) { event ->
                    if (event.button == PointerButton.Secondary) {
                        showContextMenu = true
                    }
                }
                .clickable { onFileClick(file) },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = if (isSelected)
                AppColors.Primary.copy(alpha = 0.1f)
            else
                AppColors.Surface,
            border = if (isSelected)
                androidx.compose.foundation.BorderStroke(2.dp, AppColors.Primary.copy(alpha = 0.3f))
            else
                null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 紧凑图标
                Icon(
                    imageVector = getFileIcon(file),
                    contentDescription = file.name,
                    tint = getFileIconTint(file),
                    modifier = Modifier
                        .size(32.dp)
                        .padding(bottom = 4.dp)
                )

                // 文件名（紧凑显示）
                Text(
                    text = file.name,
                    style = AppTypography.Caption,
                    color = AppColors.TextPrimary,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 右键菜单（文件和文件夹都支持）
        if (showContextMenu) {
            FileContextMenu(
                file = file,
                onDismiss = { showContextMenu = false },
                onEdit = {
                    showContextMenu = false
                    operations.scope.launch {
                        try {
                            val tempDir = System.getProperty("java.io.tmpdir")
                            val tempFile = File(tempDir, file.name)
                            operations.onLoadingChange(true)

                            operations.sftpManager.downloadFile(file.path, tempFile.absolutePath).fold(
                                onSuccess = {
                                    operations.onLoadingChange(false)
                                    try {
                                        java.awt.Desktop.getDesktop().open(tempFile)

                                        // 文件监听
                                        operations.scope.launch {
                                            try {
                                                val originalModified = tempFile.lastModified()
                                                kotlinx.coroutines.delay(2000)

                                                var keepWatching = true
                                                while (keepWatching) {
                                                    kotlinx.coroutines.delay(1000)
                                                    if (tempFile.lastModified() > originalModified && tempFile.exists()) {
                                                        operations.sftpManager.uploadFile(tempFile.absolutePath, file.path).fold(
                                                            onSuccess = {
                                                                println("✓ 文件已自动保存: ${file.name}")
                                                                keepWatching = false
                                                            },
                                                            onFailure = {
                                                                println("✗ 自动上传失败")
                                                                keepWatching = false
                                                            }
                                                        )
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                println("文件监听出错: ${e.message}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        operations.onErrorChange("无法打开文件: ${e.message}")
                                    }
                                },
                                onFailure = { error ->
                                    operations.onErrorChange(error.message)
                                    operations.onLoadingChange(false)
                                }
                            )
                        } catch (e: Exception) {
                            operations.onErrorChange("编辑失败: ${e.message}")
                            operations.onLoadingChange(false)
                        }
                    }
                },
                onDownload = {
                    showContextMenu = false
                    // 先显示文件选择对话框，然后再启动协程执行下载
                    try {
                        val fileChooser = JFileChooser()
                        if (file.isDirectory) {
                            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                            fileChooser.dialogTitle = "选择保存文件夹的位置"
                        } else {
                            fileChooser.selectedFile = File(file.name)
                        }
                        val dialogResult = fileChooser.showSaveDialog(null)
                        if (dialogResult == JFileChooser.APPROVE_OPTION) {
                            val selectedFile = fileChooser.selectedFile
                            val localPath = selectedFile.absolutePath

                            // 对于文件下载，如果用户选择了目录，需要添加文件名
                            val targetPath = if (selectedFile.isDirectory) {
                                "$localPath/${file.name}"
                            } else {
                                localPath
                            }

                            // 在后台协程中执行下载，不阻塞UI
                            operations.scope.launch(Dispatchers.IO) {
                                try {
                                    // 创建传输任务
                                    val task = TransferTask(
                                        fileName = file.name,
                                        filePath = file.path,
                                        localPath = targetPath,
                                        type = TransferType.DOWNLOAD,
                                        status = TransferStatus.PENDING,
                                        totalSize = file.size,
                                        isDirectory = file.isDirectory
                                    )
                                    TransferManager.addTask(task)

                                    // 更新任务状态为运行中
                                    TransferManager.updateTask(task.id) {
                                        it.copy(status = TransferStatus.RUNNING)
                                    }

                                    val startTime = System.currentTimeMillis()
                                    var lastUpdateTime = startTime
                                    var lastTransferred = 0L

                                    if (file.isDirectory) {
                                        // 对于文件夹下载，确保目标路径包含文件夹名称
                                        operations.sftpManager.downloadDirectory(file.path, targetPath).fold(
                                            onSuccess = {
                                                TransferManager.updateTask(task.id) {
                                                    it.copy(
                                                        status = TransferStatus.COMPLETED,
                                                        transferredSize = it.totalSize,
                                                        endTime = System.currentTimeMillis()
                                                    )
                                                }
                                                withContext(Dispatchers.Main) {
                                                    operations.onErrorChange(null)
                                                }
                                                println("✓ 文件夹下载完成: ${file.name}")
                                            },
                                            onFailure = { error ->
                                                TransferManager.updateTask(task.id) {
                                                    it.copy(
                                                        status = TransferStatus.FAILED,
                                                        errorMessage = error.message,
                                                        endTime = System.currentTimeMillis()
                                                    )
                                                }
                                                withContext(Dispatchers.Main) {
                                                    operations.onErrorChange(error.message)
                                                }
                                            }
                                        )
                                    } else {
                                        operations.sftpManager.downloadFile(
                                            file.path,
                                            targetPath
                                        ) { transferred, total ->
                                            // 更新进度
                                            val currentTime = System.currentTimeMillis()
                                            val timeDelta = (currentTime - lastUpdateTime).coerceAtLeast(50) // 降低到50ms

                                            // 总是更新传输大小，但控制速度更新频率
                                            TransferManager.updateTask(task.id) {
                                                it.copy(transferredSize = transferred)
                                            }

                                            if (timeDelta >= 100) { // 每100ms更新速度
                                                val speed = ((transferred - lastTransferred) * 1000) / timeDelta
                                                TransferManager.updateTask(task.id) {
                                                    it.copy(speed = speed)
                                                }
                                                lastUpdateTime = currentTime
                                                lastTransferred = transferred
                                            }
                                        }.fold(
                                            onSuccess = {
                                                TransferManager.updateTask(task.id) {
                                                    it.copy(
                                                        status = TransferStatus.COMPLETED,
                                                        transferredSize = it.totalSize,
                                                        speed = 0L,
                                                        endTime = System.currentTimeMillis()
                                                    )
                                                }
                                                withContext(Dispatchers.Main) {
                                                    operations.onErrorChange(null)
                                                }
                                            },
                                            onFailure = { error ->
                                                TransferManager.updateTask(task.id) {
                                                    it.copy(
                                                        status = TransferStatus.FAILED,
                                                        errorMessage = error.message,
                                                        endTime = System.currentTimeMillis()
                                                    )
                                                }
                                                withContext(Dispatchers.Main) {
                                                    operations.onErrorChange(error.message)
                                                }
                                            }
                                        )
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        operations.onErrorChange("下载失败: ${e.message}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        operations.onErrorChange("选择保存位置失败: ${e.message}")
                    }
                },
                onRename = {
                    showContextMenu = false
                    onRename(file)
                },
                onDelete = {
                    showContextMenu = false
                    operations.scope.launch {
                        try {
                            operations.onLoadingChange(true)
                            operations.sftpManager.deleteFile(file.path).fold(
                                onSuccess = {
                                    operations.onReload()
                                },
                                onFailure = { error ->
                                    operations.onErrorChange(error.message)
                                    operations.onLoadingChange(false)
                                }
                            )
                        } catch (e: Exception) {
                            operations.onErrorChange("删除失败: ${e.message}")
                            operations.onLoadingChange(false)
                        }
                    }
                },
                onShowDetails = {
                    showContextMenu = false
                    onShowDetails(file)
                }
            )
        }
    }
}


// 获取文件图标颜色的工具函数
private fun getFileIconColor(file: FileInfo): androidx.compose.ui.graphics.Color {
    return if (file.isDirectory) {
        AppColors.Warning
    } else {
        val extension = file.name.substringAfterLast('.', "").lowercase()
        when (extension) {
            // 图片文件 - 绿色
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico" -> AppColors.Success
            // 视频文件 - 红色
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm" -> AppColors.Error
            // 音频文件 - 紫色
            "mp3", "wav", "flac", "aac", "ogg", "m4a" -> AppColors.Primary.copy(alpha = 0.8f)
            // 文档文件 - 蓝色
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> AppColors.Info ?: AppColors.Primary
            // 代码文件 - 青色
            "kt", "java", "py", "js", "ts", "html", "css", "xml", "json", "yml", "yaml" -> AppColors.Success.copy(alpha = 0.8f)
            // 脚本文件 - 橙色
            "sh", "bat", "cmd" -> AppColors.Warning.copy(alpha = 0.8f)
            // 压缩文件 - 灰色
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz" -> AppColors.TextSecondary
            // 文本文件 - 深灰色
            "txt", "md", "log" -> AppColors.TextPrimary
            // 默认文件 - 灰色
            else -> AppColors.TextSecondary
        }
    }
}


private fun formatModifiedTime(timestamp: Long): String {
    if (timestamp == 0L) return "--"
    val date = java.util.Date(timestamp * 1000)
    val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}
