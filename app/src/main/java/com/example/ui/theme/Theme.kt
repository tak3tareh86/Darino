package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val AppDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ExpenseRoseDark,
    errorContainer = ExpenseContainerDark,
    onError = TextPrimaryDark
)

private val AppLightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    onPrimary = EmeraldOnPrimaryLight,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ExpenseRoseLight,
    errorContainer = ExpenseContainerLight,
    onError = EmeraldOnPrimaryLight
)

@Composable
fun FinanceManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AppDarkColorScheme else AppLightColorScheme

    // Enforce Persian RTL Layout Direction for the entire application
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PersianTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
