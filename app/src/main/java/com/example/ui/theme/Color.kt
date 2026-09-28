package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Light Theme Tokens
val EmeraldPrimaryLight = Color(0xFF0D9488)
val EmeraldOnPrimaryLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFCCFBF1)
val EmeraldOnContainerLight = Color(0xFF115E59)

val SlateSecondaryLight = Color(0xFF1E293B)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)

val BackgroundLight = Color(0xFFF6F8FB)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceElevatedLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)
val TextMutedLight = Color(0xFF94A3B8)

val SuccessGreenLight = Color(0xFF10B981)
val SuccessContainerLight = Color(0xFFD1FAE5)
val ExpenseRoseLight = Color(0xFFF43F5E)
val ExpenseContainerLight = Color(0xFFFFE4E6)
val WarningAmberLight = Color(0xFFF59E0B)
val WarningContainerLight = Color(0xFFFEF3C7)
val InfoIndigoLight = Color(0xFF4F46E5)
val InfoContainerLight = Color(0xFFE0E7FF)

// Dark Theme Tokens
val EmeraldPrimaryDark = Color(0xFF14B8A6)
val EmeraldOnPrimaryDark = Color(0xFF042F2E)
val EmeraldContainerDark = Color(0xFF134E4A)
val EmeraldOnContainerDark = Color(0xFF5EEAD4)

val SlateSecondaryDark = Color(0xFF1E293B)
val SlateOnSecondaryDark = Color(0xFFF8FAFC)

val BackgroundDark = Color(0xFF000000)
val SurfaceDark = Color(0xFF0A0D14)
val SurfaceElevatedDark = Color(0xFF121824)
val SurfaceVariantDark = Color(0xFF161E2E)

val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val TextMutedDark = Color(0xFF64748B)

val SuccessGreenDark = Color(0xFF34D399)
val SuccessContainerDark = Color(0xFF064E3B)
val ExpenseRoseDark = Color(0xFFFB7185)
val ExpenseContainerDark = Color(0xFF4C0519)
val WarningAmberDark = Color(0xFFFBBF24)
val WarningContainerDark = Color(0xFF451A03)
val InfoIndigoDark = Color(0xFF818CF8)
val InfoContainerDark = Color(0xFF1E1B4B)

// 3D Gradients & Glass Brushes
val HeaderGradientLight = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F172A),
        Color(0xFF134E4A),
        Color(0xFF0D9488)
    )
)

val HeaderGradientDark = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0B132B),
        Color(0xFF112240),
        Color(0xFF0A2B28)
    )
)

val SummaryCardGradientLight = Brush.linearGradient(
    colors = listOf(
        Color(0xFF0F172A),
        Color(0xFF1E293B),
        Color(0xFF0F766E)
    )
)

val SummaryCardGradientDark = Brush.linearGradient(
    colors = listOf(
        Color(0xFF131D31),
        Color(0xFF1E293B),
        Color(0xFF114B44)
    )
)

val GoldAccentGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFDE047),
        Color(0xFFF59E0B),
        Color(0xFFD97706)
    )
)
