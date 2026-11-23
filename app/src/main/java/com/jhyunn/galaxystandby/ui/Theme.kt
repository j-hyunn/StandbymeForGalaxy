package com.jhyunn.galaxystandby.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object StandbyColors {
    val Background = Color(0xFF101114)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF92949B)
    val Divider = Color(0xFF34363D)
    /** 포인트 컬러. 권한 안내 등 최소한의 강조에만 사용 */
    val Accent = Color(0xFF8AB4FF)
}

@Composable
fun StandbyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = StandbyColors.Background,
            surface = StandbyColors.Background,
            onBackground = StandbyColors.TextPrimary,
            onSurface = StandbyColors.TextPrimary,
            primary = StandbyColors.TextPrimary,
            onPrimary = StandbyColors.Background,
            secondary = StandbyColors.TextSecondary,
            outline = StandbyColors.Divider,
        ),
        content = content,
    )
}
