package ui.ops

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import data.CommandRuleData
import theme.*

/**
 * 命令按钮组件 - 简单按钮设计
 */
@Composable
fun CommandButton(
    commandRule: CommandRuleData,
    isConnected: Boolean,
    onExecuteCommand: (CommandRuleData) -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val isExecuting by remember { mutableStateOf(false) } // TODO: 从状态管理获取

    Button(
        onClick = {
            if (isConnected) {
                onExecuteCommand(commandRule)
            } else {
                onStatusMessage("请先连接到主机")
            }
        },
        enabled = isConnected && !isExecuting,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = commandRule.name,
            style = AppTypography.BodySmall
        )
    }
}
