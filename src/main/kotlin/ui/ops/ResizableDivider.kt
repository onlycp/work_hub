package ui.ops

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import theme.AppColors
import java.awt.Cursor

/**
 * 可拖动的垂直分隔条
 * @param onDrag 拖动回调，参数为拖动的像素偏移量
 */
@Composable
fun ResizableDivider(
    onDrag: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(6.dp)
            .background(AppColors.Surface)
            .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            }
    ) {
        // 中间的视觉指示线
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(AppColors.Divider)
                .padding(horizontal = 2.5.dp)
        )
    }
}
