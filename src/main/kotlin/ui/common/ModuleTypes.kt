package ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 模块类型枚举
 */
enum class ModuleType(
    val displayName: String,
    val icon: ImageVector
) {
    HOME("首页", Icons.Default.Home),
    OPS("主机", Icons.Default.Computer),
    KEYS("密钥", Icons.Default.VpnKey),
    CURSOR("Cursor", Icons.Default.SmartToy),
    MEMBERS("成员", Icons.Default.People),
    EXPENSE("报销", Icons.Default.Receipt),
    LOGS("日志", Icons.Default.Description),
    PROFILE("个人", Icons.Default.AccountCircle),
    SETTINGS("设置", Icons.Default.Settings),
    HUBLINK("代理", Icons.Default.VpnLock)
}

/**
 * 运维抽屉标签枚举
 */
enum class OpsDrawerTab(
    val displayName: String,
    val icon: ImageVector
) {
    COMMANDS("命令", Icons.Default.Terminal),
    PORT_FORWARDING("端口", Icons.Default.Cable),
    FILE_MANAGER("文件", Icons.Default.Folder)
}
