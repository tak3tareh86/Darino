package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

enum class LogoStyle {
    IMAGE_ASSET,
    VECTOR_MARK
}

/**
 * Official Darino Brand Logo Mark Composable.
 * Renders the modern, stylized 'D' brand mark with elegant ambient glow.
 */
@Composable
fun DarinoLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showShadow: Boolean = true,
    style: LogoStyle = LogoStyle.IMAGE_ASSET
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val cornerRadius = size * 0.26f

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showShadow) {
                    Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = if (isDark) 0.35f else 0.22f),
                                    Color.Transparent
                                ),
                                radius = size.toPx() * 0.75f
                            )
                        )
                    }
                } else Modifier
            )
            .shadow(
                elevation = if (showShadow) (size.value * 0.14f).dp else 0.dp,
                shape = RoundedCornerShape(cornerRadius),
                spotColor = Color(0xFF10B981).copy(alpha = 0.5f),
                ambientColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = if (isDark) {
                        listOf(Color(0xFF0F1D2B), Color(0xFF09111A))
                    } else {
                        listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
                    }
                )
            )
            .border(
                BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = if (isDark) 0.5f else 0.7f),
                            Color(0xFF0284C7).copy(alpha = if (isDark) 0.25f else 0.4f)
                        )
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_darino_logo),
            contentDescription = "لوگوی دارینو",
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius)),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Official Darino Persian Wordmark Composable.
 * Uses strict single-line configuration and safe typography without breaking Arabic glyph connections.
 */
@Composable
fun DarinoWordmark(
    modifier: Modifier = Modifier,
    showSubtitle: Boolean = true,
    subtitleText: String = "مدیریت مالی، اقساط و خودرو",
    fontSize: Float = 28f,
    isDark: Boolean = MaterialTheme.colorScheme.background.red < 0.2f
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main Brand Title - Single line, extra bold, zero letterSpacing to preserve Persian ligatures
            Text(
                text = "دارینو",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.35f).sp,
                    letterSpacing = 0.sp
                ),
                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )

            if (showSubtitle) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = (fontSize * 0.46f).coerceIn(12f, 15f).sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = ((fontSize * 0.46f).coerceIn(12f, 15f) * 1.4f).sp,
                        letterSpacing = 0.sp
                    ),
                    color = if (isDark) Color(0xFF34D399) else EmeraldPrimaryLight,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Combined Official Branding Header Component: Logo Mark + Wordmark.
 */
@Composable
fun DarinoBrandHeader(
    modifier: Modifier = Modifier,
    logoSize: Dp = 72.dp,
    showSubtitle: Boolean = true,
    subtitleText: String = "مدیریت مالی، اقساط و خودرو",
    isDark: Boolean = MaterialTheme.colorScheme.background.red < 0.2f
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DarinoLogoMark(
            size = logoSize,
            showShadow = true
        )

        DarinoWordmark(
            showSubtitle = showSubtitle,
            subtitleText = subtitleText,
            fontSize = (logoSize.value * 0.30f).coerceIn(20f, 32f),
            isDark = isDark
        )
    }
}
