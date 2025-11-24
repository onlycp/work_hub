package data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 传输任务管理器
 * 管理所有文件传输任务的状态
 */
object TransferManager {
    private val _tasks = MutableStateFlow<List<TransferTask>>(emptyList())
    val tasks: StateFlow<List<TransferTask>> = _tasks.asStateFlow()

    /**
     * 添加传输任务
     */
    fun addTask(task: TransferTask) {
        // 创建新列表以避免并发修改
        _tasks.value = _tasks.value.toList() + task
    }

    /**
     * 更新任务状态
     */
    fun updateTask(taskId: String, update: (TransferTask) -> TransferTask) {
        // 创建新列表以避免并发修改
        _tasks.value = _tasks.value.toList().map { task ->
            if (task.id == taskId) {
                update(task)
            } else {
                task
            }
        }
    }

    /**
     * 移除任务
     */
    fun removeTask(taskId: String) {
        // 创建新列表以避免并发修改
        _tasks.value = _tasks.value.toList().filter { it.id != taskId }
    }

    /**
     * 清除所有已完成的任务
     */
    fun clearCompleted() {
        // 创建新列表以避免并发修改
        _tasks.value = _tasks.value.toList().filter { 
            it.status != TransferStatus.COMPLETED && it.status != TransferStatus.FAILED
        }
    }

    /**
     * 清除所有任务
     */
    fun clearAll() {
        _tasks.value = emptyList()
    }

    /**
     * 获取活跃任务数量
     */
    fun getActiveTaskCount(): Int {
        return _tasks.value.count { 
            it.status == TransferStatus.PENDING || it.status == TransferStatus.RUNNING
        }
    }
}

