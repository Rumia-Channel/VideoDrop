package uk.rumia_ch.videodrop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Clipbox-inspired palette: シンプル・余白・ファイル管理
// Research: official Clipbox is whitespace-heavy, folder/card white, text #1F2937, primary blue
object ClipboxColors {
    val Background = Color(0xFFF7F8FA)      // 背景: research suggestion #F7F8FA
    val Surface = Color(0xFFFFFFFF)         // カード
    val Primary = Color(0xFF3B82F6)         // メイン青
    val PrimaryContainer = Color(0xFFDBEAFE)
    val TextPrimary = Color(0xFF1F2937)
    val TextSecondary = Color(0xFF6B7280)
    val Divider = Color(0xFFE5E7EB)
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
}

private val ClipboxLightScheme = lightColorScheme(
    primary = ClipboxColors.Primary,
    onPrimary = Color.White,
    primaryContainer = ClipboxColors.PrimaryContainer,
    onPrimaryContainer = ClipboxColors.TextPrimary,
    background = ClipboxColors.Background,
    onBackground = ClipboxColors.TextPrimary,
    surface = ClipboxColors.Surface,
    onSurface = ClipboxColors.TextPrimary,
    surfaceVariant = Color(0xFFF3F4F6),
    onSurfaceVariant = ClipboxColors.TextSecondary,
    error = ClipboxColors.Error,
    onError = Color.White
)

@Composable
fun ClipboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClipboxLightScheme,
        content = content
    )
}
