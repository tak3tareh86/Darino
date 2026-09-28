package com.example.ui.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Master Spacing Scale - Strictly consistent across all screens.
 * Replaces arbitrary padding with standard 4-8-12-16-20-24-32-40-48 scale.
 */
object AppSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val huge: Dp = 32.dp
    val massive: Dp = 40.dp
    val jumbo: Dp = 48.dp
}

/**
 * Standardized Corner Radius Scale.
 */
object AppRadius {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp
    val pill: Dp = 100.dp
}

/**
 * Standardized Elevation Levels for soft multi-layered depth.
 */
object AppElevation {
    val Level0: Dp = 0.dp
    val Level1: Dp = 2.dp
    val Level2: Dp = 4.dp
    val Level3: Dp = 8.dp
    val Level4: Dp = 16.dp
}

/**
 * Standardized Motion & Animation Specifications.
 * Premium, fast, natural micro-interactions.
 */
object AppMotion {
    const val DurationFast = 150
    const val DurationNormal = 250
    const val DurationSlow = 400

    val SpecFast = tween<Float>(durationMillis = DurationFast)
    val SpecNormal = tween<Float>(durationMillis = DurationNormal)
    val SpecSlow = tween<Float>(durationMillis = DurationSlow)

    val SpringPress = spring<Float>(
        stiffness = 500f,
        dampingRatio = Spring.DampingRatioLowBouncy
    )

    val SpringSelection = spring<Float>(
        stiffness = Spring.StiffnessMediumLow,
        dampingRatio = Spring.DampingRatioMediumBouncy
    )
}

/**
 * Semantic Palette for Light and Dark themes.
 */
object AppColors {
    // Primary Emerald Brand
    val EmeraldLight = Color(0xFF10B981)
    val EmeraldDark = Color(0xFF34D399)
    val EmeraldDeep = Color(0xFF059669)
    val EmeraldSurfaceLight = Color(0xFFECFDF5)
    val EmeraldSurfaceDark = Color(0xFF064E3B)

    // Expense / Rose
    val ExpenseLight = Color(0xFFF43F5E)
    val ExpenseDark = Color(0xFFFB7185)
    val ExpenseSurfaceLight = Color(0xFFFFF1F2)
    val ExpenseSurfaceDark = Color(0xFF4C0519)

    // Warning / Amber
    val WarningLight = Color(0xFFF59E0B)
    val WarningDark = Color(0xFFFBBF24)
    val WarningSurfaceLight = Color(0xFFFFFBEB)
    val WarningSurfaceDark = Color(0xFF451A03)

    // Info / Blue
    val InfoLight = Color(0xFF3B82F6)
    val InfoDark = Color(0xFF60A5FA)
    val InfoSurfaceLight = Color(0xFFEFF6FF)
    val InfoSurfaceDark = Color(0xFF1E3A8A)

    // Purple / Accent
    val PurpleLight = Color(0xFF8B5CF6)
    val PurpleDark = Color(0xFFA78BFA)
    val PurpleSurfaceLight = Color(0xFFF5F3FF)
    val PurpleSurfaceDark = Color(0xFF2E1065)

    // Background & Surfaces
    val BgLight = Color(0xFFF8FAFC)
    val BgDark = Color(0xFF090D16)

    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF131B2E)

    val SurfaceElevatedLight = Color(0xFFFFFFFF)
    val SurfaceElevatedDark = Color(0xFF1A243D)

    val CardBorderLight = Color(0xFFE2E8F0)
    val CardBorderDark = Color(0xFF1E293B)

    val TextPrimaryLight = Color(0xFF0F172A)
    val TextPrimaryDark = Color(0xFFF1F5F9)

    val TextSecondaryLight = Color(0xFF64748B)
    val TextSecondaryDark = Color(0xFF94A3B8)

    val TextMutedLight = Color(0xFF94A3B8)
    val TextMutedDark = Color(0xFF64748B)

    // Semantic gradients
    val EmeraldGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF10B981), Color(0xFF059669))
    )

    val EmeraldGradientDark = Brush.linearGradient(
        colors = listOf(Color(0xFF059669), Color(0xFF047857))
    )

    val CardGlowGradientLight = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.9f), Color.White.copy(alpha = 0.2f))
    )

    val CardGlowGradientDark = Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)
    )

    @Composable
    fun cardGlowGradient(): Brush {
        return if (isSystemInDarkTheme()) CardGlowGradientDark else CardGlowGradientLight
    }

    @Composable
    fun shadowAmbient(): Color {
        return if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.06f)
    }

    @Composable
    fun shadowSpot(): Color {
        return if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.7f) else Color(0xFF0F172A).copy(alpha = 0.10f)
    }
}

/**
 * Persian Number & Currency Formatting Utilities.
 * Standardizes all financial and count displays across the application.
 */
object AppFormatters {
    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toPersianDigits(input: String): String {
        val sb = java.lang.StringBuilder()
        for (char in input) {
            if (char in '0'..'9') {
                sb.append(persianDigits[char - '0'])
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    fun toPersianDigits(number: Number): String {
        return toPersianDigits(number.toString())
    }

    /**
     * Formats amounts with 3-digit comma separators and Persian digits.
     * e.g. 18500000 -> "۱۸,۵۰۰,۰۰۰"
     */
    fun formatAmount(amount: Long, showSign: Boolean = false): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formatted = formatter.format(kotlin.math.abs(amount))
        val persian = toPersianDigits(formatted)
        return when {
            showSign && amount > 0 -> "+$persian"
            showSign && amount < 0 -> "-$persian"
            else -> persian
        }
    }

    /**
     * Formats amounts with currency suffix.
     * e.g. "۱۸,۵۰۰,۰۰۰ تومان" or "۱۸۵,۰۰۰,۰۰۰ ریال"
     */
    fun formatCurrency(amount: Long, currency: String? = null, showSign: Boolean = false): String {
        return if (currency != null) {
            "${formatAmount(amount, showSign)} $currency"
        } else {
            com.example.util.MoneyFormatter.format(amount, showSign)
        }
    }

    /**
     * Formats large numbers compactly.
     * e.g. 18500000 -> "۱۸.۵ میلیون تومان"
     */
    fun formatCompact(amount: Long, currency: String = "تومان"): String {
        val abs = kotlin.math.abs(amount)
        return when {
            abs >= 1_000_000_000 -> {
                val value = abs / 1_000_000_000.0
                val formatted = String.format(Locale.US, "%.1f", value)
                "${toPersianDigits(formatted)} میلیارد $currency"
            }
            abs >= 1_000_000 -> {
                val value = abs / 1_000_000.0
                val formatted = String.format(Locale.US, "%.1f", value)
                "${toPersianDigits(formatted)} میلیون $currency"
            }
            abs >= 1_000 -> {
                val value = abs / 1_000.0
                val formatted = String.format(Locale.US, "%.1f", value)
                "${toPersianDigits(formatted)} هزار $currency"
            }
            else -> "${toPersianDigits(abs)} $currency"
        }
    }

    /**
     * Formats kilometers.
     * e.g. 125800 -> "۱۲۵,۸۰۰ کیلومتر"
     */
    fun formatKm(km: Long): String {
        return "${formatAmount(km)} کیلومتر"
    }
}
