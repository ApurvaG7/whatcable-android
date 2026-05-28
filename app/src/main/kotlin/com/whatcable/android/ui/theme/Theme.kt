package com.whatcable.android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WhatCableDark = darkColorScheme(
    primary = Blue60,
    onPrimary = Blue10,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue80,
    secondary = Blue70,
    onSecondary = Blue20,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Blue80,
    tertiary = Green60,
    onTertiary = Green20,
    tertiaryContainer = Green20,
    onTertiaryContainer = Green80,
    error = Red60,
    onError = Red20,
    errorContainer = Red20,
    onErrorContainer = Red80,
    background = DarkSurface,
    onBackground = Color(0xFFE6EDF3),
    surface = DarkSurface,
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = Color(0xFF9DA5AE),
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurfaceContainer,
    surfaceContainer = DarkSurfaceHigh,
    surfaceContainerHigh = DarkSurfaceVariant,
    surfaceContainerHighest = Color(0xFF2D333B),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val WhatCableLight = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue10,
    secondary = Blue50,
    onSecondary = Color.White,
    secondaryContainer = Blue90,
    onSecondaryContainer = Blue20,
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Green20,
    error = Color(0xFFDC2626),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun WhatCableTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) WhatCableDark else WhatCableLight

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WhatCableTypography,
        content = content
    )
}
