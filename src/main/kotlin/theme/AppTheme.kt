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
    // 主色调（更现代的蓝色）
    val Primary = Color(0xFF0066FF)
    val PrimaryVariant = Color(0xFF0052CC)
    val PrimaryLight = Color(0xFF4D94FF)
    val Secondary = Color(0xFF00C9A7)
    val SecondaryVariant = Color(0xFF00BFA5)

    // 背景色（更柔和的层次）
    val Background = Color(0xFFF8F9FA)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceVariant = Color(0xFFF0F2F5)
    val SurfaceElevated = Color(0xFFFFFFFF)

    // 文字颜色
    val TextPrimary = Color(0xFF1C1E21)
    val TextSecondary = Color(0xFF65676B)
    val TextDisabled = Color(0xFFB0B3B8)
    val TextInverse = Color(0xFFFFFFFF)

    // 终端色（更现代的深色主题）
    val TerminalBackground = Color(0xFF1E1E1E)
    val TerminalText = Color(0xFFD4D4D4)
    val TerminalPrompt = Color(0xFF4EC9B0)
    val TerminalInput = Color(0xFF2D2D2D)

    // 状态颜色
    val Success = Color(0xFF00BA88)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF0066FF)

    // 边框和分隔线
    val Divider = Color(0xFFE4E6EB)
    val Border = Color(0xFFCED0D4)
    val BorderHover = Color(0xFFB0B3B8)

    // Tab相关颜色
    val TabSelected = Primary
    val TabUnselected = TextSecondary
    val TabBackground = Surface
    val TabIndicator = Primary
    val TabHover = Color(0xFFF0F2F5)

    // 交互状态颜色
    val Hover = Color(0xFFF0F2F5)
    val Active = Color(0xFFE4E6EB)
    val Focus = Primary.copy(alpha = 0.1f)

    // 科技感渐变色
    val GradientStart = Color(0xFF0066FF)
    val GradientEnd = Color(0xFF00C9A7)
    val GlowPrimary = Primary.copy(alpha = 0.15f)
    val GlowWarning = Warning.copy(alpha = 0.15f)
    val AccentCyan = Color(0xFF00E5FF)
    val AccentPurple = Color(0xFF7C4DFF)

    // 向后兼容的别名
    val BackgroundPrimary = Background
    val BackgroundSecondary = SurfaceVariant
    val BackgroundTertiary = Hover
    val BorderFocus = Primary.copy(alpha = 0.1f)
    val Shadow = Color(0x1F000000)
}

// 字体系统
object AppTypography {
    // 标题字体（匹配 HomeApp 字体层级）
    val TitleLarge = TextStyle(
        fontSize = 15.sp,  // 标题：15sp
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default
    )
    val TitleMedium = TextStyle(
        fontSize = 13.sp,  // 副标题：13sp
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default
    )
    val TitleSmall = TextStyle(
        fontSize = 12.sp,  // 正文：12sp
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Default
    )

    // 正文字体
    val BodyLarge = TextStyle(
        fontSize = 12.sp,  // 正文：12sp
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    )
    val BodyMedium = TextStyle(
        fontSize = 11.sp,  // 默认字体：11sp
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    )
    val BodySmall = TextStyle(
        fontSize = 11.sp,  // 默认字体：11sp（与 HomeApp BodySmall 一致）
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    )

    // 说明文字
    val Caption = TextStyle(
        fontSize = 10.sp,  // 辅助文字：10sp
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default
    )
    val Overline = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Default,
        letterSpacing = 1.5.sp
    )

    // 按钮字体
    val Button = TextStyle(
        fontSize = 13.sp,  // 按钮：13sp（略小于 HomeApp 的 14sp）
        fontWeight = FontWeight.Medium,
        fontFamily = FontFamily.Default
    )

    // 向后兼容的别名（TextUnit 用于 fontSize，匹配 HomeApp）
    val TitleFontSize = 15.sp        // 标题
    val SubtitleFontSize = 13.sp     // 副标题
    val BodyFontSize = 12.sp         // 正文
    val BodySmallFontSize = 11.sp    // 小号正文 (默认字体)
    val TerminalFontSize = 11.sp     // 终端文字
}

// 间距系统
object AppDimensions {
    // 间距（8dp网格系统）
    val SpaceXXS = 4.dp
    val SpaceXS = 4.dp
    val SpaceS = 8.dp
    val SpaceM = 12.dp
    val SpaceL = 16.dp
    val SpaceXL = 24.dp
    val SpaceXXL = 32.dp

    // 组件特定间距
    val PaddingScreen = 16.dp
    val PaddingCard = 16.dp
    val PaddingButton = 12.dp
    val PaddingInput = 12.dp
    val PaddingInputSmall = 8.dp
    val PaddingM = 12.dp
    val PaddingL = 16.dp
    val PaddingXL = 24.dp

    // 圆角（更现代的圆角设计）
    val CornerXS = 4.dp
    val CornerSmall = 6.dp
    val CornerMedium = 8.dp
    val CornerLarge = 12.dp
    val CornerXL = 16.dp
    val CornerRound = 999.dp  // 完全圆角

    // 阴影（更柔和的层次）
    val ElevationNone = 0.dp
    val ElevationXS = 1.dp
    val ElevationSmall = 2.dp
    val ElevationMedium = 4.dp
    val ElevationLarge = 8.dp
    val ElevationXL = 12.dp
    val ElevationDialog = 16.dp

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



