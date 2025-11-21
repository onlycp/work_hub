package theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 应用统一设计系统
 * 所有UI组件必须使用这些设计令牌，禁止硬编码样式值
 */

// 颜色系统
object AppColors {
    // 主色调（macOS风格的柔和蓝色）
    val Primary = Color(0xFF007AFF)  // macOS标准蓝色
    val PrimaryVariant = Color(0xFF005BD3)
    val PrimaryLight = Color(0xFF4DA3FF)
    val Secondary = Color(0xFF34C759)  // macOS绿色
    val SecondaryVariant = Color(0xFF28CD41)

    // 背景色（macOS风格的层次）
    val Background = Color(0xFFF5F5F7)  // macOS浅灰背景
    val Surface = Color(0xFFFFFFFF)    // 纯白色表面
    val SurfaceVariant = Color(0xFFF2F2F7)  // 更浅的变体
    val SurfaceElevated = Color(0xFFFFFFFF)

    // 文字颜色（macOS文字层次）
    val TextPrimary = Color(0xFF1D1D1F)    // macOS深色文字
    val TextSecondary = Color(0xFF86868B)  // macOS次级文字
    val TextDisabled = Color(0xFFAEAEB2)   // macOS禁用文字
    val TextInverse = Color(0xFFFFFFFF)

    // 终端色（保持现代深色主题）
    val TerminalBackground = Color(0xFF1E1E1E)
    val TerminalText = Color(0xFFD4D4D4)
    val TerminalPrompt = Color(0xFF4EC9B0)
    val TerminalInput = Color(0xFF2D2D2D)

    // 状态颜色（macOS风格）
    val Success = Color(0xFF34C759)  // macOS绿色
    val Warning = Color(0xFFFF9500)  // macOS橙色
    val Error = Color(0xFFFF3B30)    // macOS红色
    val Info = Color(0xFF007AFF)     // macOS蓝色

    // 边框和分隔线（macOS风格的细线）
    val Divider = Color(0xFFE5E5EA)  // macOS分隔线
    val Border = Color(0xFFC6C6C8)   // macOS边框
    val BorderHover = Color(0xFF86868B)

    // Tab相关颜色
    val TabSelected = Primary
    val TabUnselected = TextSecondary
    val TabBackground = Surface
    val TabIndicator = Primary
    val TabHover = Color(0xFFF2F2F7)  // macOS悬停色

    // 交互状态颜色（macOS风格）
    val Hover = Color(0xFFF2F2F7)    // macOS悬停背景
    val Active = Color(0xFFE5E5EA)   // macOS激活状态
    val Focus = Primary.copy(alpha = 0.1f)

    // 科技感渐变色（macOS风格的柔和渐变）
    val GradientStart = Color(0xFF007AFF)
    val GradientEnd = Color(0xFF5856D6)
    val GlowPrimary = Primary.copy(alpha = 0.1f)
    val GlowWarning = Warning.copy(alpha = 0.1f)
    val AccentCyan = Color(0xFF64D2FF)
    val AccentPurple = Color(0xFFAF52DE)

    // 向后兼容的别名
    val BackgroundPrimary = Background
    val BackgroundSecondary = SurfaceVariant
    val BackgroundTertiary = Hover
    val BorderFocus = Primary.copy(alpha = 0.1f)
    val Shadow = Color(0x1F000000)
}

// 字体系统
object AppTypography {
    // 标题字体（macOS风格 - 稍微大一点的字体）
    val TitleLarge = TextStyle(
        fontSize = 16.sp,  // macOS大标题：16sp
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default
    )
    val TitleMedium = TextStyle(
        fontSize = 14.sp,  // macOS中标题：14sp
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default
    )
    val TitleSmall = TextStyle(
        fontSize = 13.sp,  // macOS小标题：13sp
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default
    )

    // 正文字体（macOS风格）
    val BodyLarge = TextStyle(
        fontSize = 13.sp,  // macOS正文：13sp
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    )
    val BodyMedium = TextStyle(
        fontSize = 12.sp,  // macOS正文：12sp
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    )
    val BodySmall = TextStyle(
        fontSize = 11.sp,  // macOS小正文：11sp
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    )

    // 说明文字（macOS风格）
    val Caption = TextStyle(
        fontSize = 10.sp,  // macOS说明文字：10sp
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    )
    val Overline = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default,
        letterSpacing = 1.5.sp
    )

    // 按钮字体（macOS风格）
    val Button = TextStyle(
        fontSize = 13.sp,  // macOS按钮：13sp
        fontWeight = FontWeight.SemiBold, // macOS按钮使用半粗体
        fontFamily = FontFamily.Default
    )

    // 向后兼容的别名（macOS风格）
    val TitleFontSize = 16.sp        // 标题
    val SubtitleFontSize = 14.sp     // 副标题
    val BodyFontSize = 13.sp         // 正文
    val BodySmallFontSize = 11.sp    // 小号正文 (默认字体)
    val TerminalFontSize = 11.sp     // 终端文字
}

// 间距系统
object AppDimensions {
    // 间距（macOS风格 - 更舒适的间距）
    val SpaceXXS = 4.dp
    val SpaceXS = 6.dp     // macOS稍微增加小间距
    val SpaceS = 10.dp     // macOS标准间距
    val SpaceM = 16.dp     // macOS中等间距
    val SpaceL = 20.dp     // macOS大间距
    val SpaceXL = 28.dp    // macOS特大间距
    val SpaceXXL = 36.dp   // macOS极大间距

    // 组件特定间距（macOS风格）
    val PaddingScreen = 12.dp   // 内容面板内边距，适中大小
    val PaddingCard = 18.dp     // macOS卡片内边距
    val PaddingButton = 14.dp   // macOS按钮内边距
    val PaddingInput = 14.dp    // macOS输入框内边距
    val PaddingInputSmall = 10.dp
    val PaddingM = 16.dp
    val PaddingL = 20.dp
    val PaddingXL = 28.dp

    // 圆角（macOS风格 - 更多的圆角）
    val CornerXS = 6.dp      // macOS最小圆角
    val CornerSmall = 8.dp   // macOS小圆角
    val CornerMedium = 10.dp // macOS中等圆角
    val CornerLarge = 12.dp  // macOS大圆角
    val CornerXL = 16.dp     // macOS特大圆角
    val CornerRound = 999.dp // 完全圆角

    // 阴影（macOS风格 - 更柔和的阴影）
    val ElevationNone = 0.dp
    val ElevationXS = 0.5.dp    // macOS极小阴影
    val ElevationSmall = 1.dp   // macOS小阴影
    val ElevationMedium = 2.dp  // macOS中等阴影
    val ElevationLarge = 4.dp   // macOS大阴影
    val ElevationXL = 6.dp      // macOS特大阴影
    val ElevationDialog = 8.dp  // macOS对话框阴影

    // 边框
    val BorderThin = 0.5.dp
    val BorderNormal = 1.dp
    val BorderThick = 2.dp

    // 组件尺寸
    val IconSmall = 16.dp
    val IconMedium = 20.dp
    val IconLarge = 24.dp
    val IconButtonSize = 36.dp
    val ButtonHeight = 36.dp
    val InputHeight = 40.dp
    val InputHeightSmall = 48.dp

    // 工具栏高度
    val ToolbarHeight = 48.dp

    // 侧边栏宽度
    val SidebarWidth = 56.dp

    // 状态栏高度
    val StatusBarHeight = 28.dp

    // 向后兼容的别名
    val RadiusS = CornerXS
    val RadiusM = CornerMedium
    val RadiusL = CornerLarge
    val RadiusXL = CornerXL
    val IconSizeS = IconSmall
    val IconSizeM = IconMedium
    val IconSizeL = IconLarge
    val IconSizeXL = 32.dp
}

// 透明度系统
object AppAlpha {
    const val Hover = 0.08f
    const val Selected = 0.12f
    const val Badge = 0.15f
    const val Disabled = 0.38f
    const val Hint = 0.6f
    const val Divider = 0.12f
    const val Shadow = 0.2f
}

// 主题配置
private val LightColors = lightColors(
    primary = AppColors.Primary,
    primaryVariant = AppColors.PrimaryVariant,
    secondary = AppColors.Secondary,
    secondaryVariant = AppColors.SecondaryVariant,
    background = AppColors.BackgroundPrimary,
    surface = AppColors.Surface,
    error = AppColors.Error,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = AppColors.TextPrimary,
    onSurface = AppColors.TextPrimary,
    onError = Color.White
)

private val DarkColors = darkColors(
    primary = AppColors.Primary,
    primaryVariant = AppColors.PrimaryVariant,
    secondary = AppColors.Secondary,
    secondaryVariant = AppColors.SecondaryVariant,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = AppColors.Error,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colors = colors,
        typography = Typography(
            h1 = AppTypography.TitleLarge,
            h2 = AppTypography.TitleMedium,
            h3 = AppTypography.TitleSmall,
            body1 = AppTypography.BodyLarge,
            body2 = AppTypography.BodyMedium,
            caption = AppTypography.Caption,
            button = AppTypography.Button
        ),
        content = content
    )
}





