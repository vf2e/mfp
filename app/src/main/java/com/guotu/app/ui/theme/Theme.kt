package com.guotu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Notion / 健康类轻量高级感色板。 */
@Immutable
data class GuotuColorTokens(
    val canvas: Color,
    val ink: Color,
    val inkMute: Color,
    val panel: Color,
    val rule: Color,
    val slate: Color,
    val signal: Color,
    val signalSoft: Color,
    val danger: Color,
    val onSignal: Color,
)

val LightGuotuColors = GuotuColorTokens(
    canvas = Color(0xFFF7F8FA),
    ink = Color(0xFF111827),
    inkMute = Color(0xFF6B7280),
    panel = Color(0xFFFFFFFF),
    rule = Color(0xFFE5E7EB),
    slate = Color(0xFF111827),
    signal = Color(0xFF3B82F6),
    signalSoft = Color(0xFFDBEAFE),
    danger = Color(0xFFEF4444),
    onSignal = Color.White,
)

val DarkGuotuColors = GuotuColorTokens(
    canvas = Color(0xFF0B0F14),
    ink = Color(0xFFF3F4F6),
    inkMute = Color(0xFF9CA3AF),
    panel = Color(0xFF161B22),
    rule = Color(0xFF2A313C),
    slate = Color(0xFFF9FAFB),
    signal = Color(0xFF60A5FA),
    signalSoft = Color(0xFF1E3A5F),
    danger = Color(0xFFF87171),
    onSignal = Color(0xFF0B0F14),
)

val LocalGuotuColors = staticCompositionLocalOf { LightGuotuColors }

/** Compose 内取色；跟随系统亮/暗。 */
object GuotuColors {
    val Canvas: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.canvas
    val Ink: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.ink
    val InkMute: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.inkMute
    val Panel: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.panel
    val Rule: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.rule
    val Slate: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.slate
    val Signal: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.signal
    val SignalSoft: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.signalSoft
    val Danger: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.danger
    val OnSignal: Color
        @Composable @ReadOnlyComposable get() = LocalGuotuColors.current.onSignal
}

private fun schemeOf(tokens: GuotuColorTokens, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = tokens.signal,
        onPrimary = tokens.onSignal,
        primaryContainer = tokens.signalSoft,
        onPrimaryContainer = tokens.ink,
        secondary = tokens.inkMute,
        onSecondary = tokens.panel,
        secondaryContainer = tokens.rule,
        onSecondaryContainer = tokens.ink,
        tertiary = tokens.signal,
        onTertiary = tokens.onSignal,
        background = tokens.canvas,
        onBackground = tokens.ink,
        surface = tokens.panel,
        onSurface = tokens.ink,
        surfaceVariant = tokens.rule,
        onSurfaceVariant = tokens.inkMute,
        outline = tokens.rule,
        outlineVariant = tokens.rule,
        error = tokens.danger,
        onError = Color.White,
    )
} else {
    lightColorScheme(
        primary = tokens.signal,
        onPrimary = tokens.onSignal,
        primaryContainer = tokens.signalSoft,
        onPrimaryContainer = tokens.slate,
        secondary = tokens.inkMute,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEEF2F7),
        onSecondaryContainer = tokens.slate,
        tertiary = tokens.signal,
        onTertiary = tokens.onSignal,
        background = tokens.canvas,
        onBackground = tokens.ink,
        surface = tokens.panel,
        onSurface = tokens.ink,
        surfaceVariant = Color(0xFFEEF2F7),
        onSurfaceVariant = tokens.inkMute,
        outline = tokens.rule,
        outlineVariant = Color(0xFFF0F2F5),
        error = tokens.danger,
        onError = Color.White,
    )
}

@Composable
fun GuotuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) DarkGuotuColors else LightGuotuColors
    CompositionLocalProvider(LocalGuotuColors provides tokens) {
        MaterialTheme(
            colorScheme = schemeOf(tokens, darkTheme),
            typography = GuotuTypography,
            content = content,
        )
    }
}
