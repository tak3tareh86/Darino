package com.example.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DarinoLogoMark
import com.example.ui.theme.EmeraldPrimaryLight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Official Persian Splash Screen for Darino (دارینو).
 * Features unified luxury typography, responsive single-line brand name,
 * ambient emerald backlighting, and smooth entrance transition.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    // Entrance Animation States
    val logoScale = remember { Animatable(0.8f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(16f) }

    // Ambient Breathing Glow
    val infiniteTransition = rememberInfiniteTransition(label = "splash_ambient_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    LaunchedEffect(Unit) {
        // Parallel reveal animations for fluid responsiveness
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }
        launch {
            contentOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        }

        // Standard brand showcase duration, then advance
        delay(1300)
        onSplashFinished()
    }

    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF060B12), // Deep midnight abyss
                    Color(0xFF0A1420), // Rich dark slate
                    Color(0xFF060B12)  // Deep obsidian base
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFF0FDF4),
                    Color(0xFFECFDF5)
                )
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .statusBarsPadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSplashFinished
                )
        ) {
            // Ambient Radial Aura behind center
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .align(Alignment.Center)
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = pulseAlpha * (if (isDark) 0.18f else 0.12f)),
                                    Color(0xFF0284C7).copy(alpha = pulseAlpha * (if (isDark) 0.08f else 0.05f)),
                                    Color.Transparent
                                ),
                                radius = size.width * 0.55f
                            )
                        )
                    }
            )

            // Main Centered Content: Logo, Brand Title, Subtitle, and Progress
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .alpha(contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 3D Emerald App Logo
                Box(
                    modifier = Modifier
                        .scale(logoScale.value)
                        .drawBehind {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF10B981).copy(alpha = pulseAlpha * 0.45f),
                                        Color(0xFF0284C7).copy(alpha = pulseAlpha * 0.15f),
                                        Color.Transparent
                                    ),
                                    radius = 80.dp.toPx()
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    DarinoLogoMark(
                        size = 100.dp,
                        showShadow = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Brand Title - Guaranteed single contiguous Persian word without breaks or split glyphs
                Text(
                    text = "دارینو",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 34.sp,
                        lineHeight = 44.sp,
                        letterSpacing = 0.sp
                    ),
                    color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / Slogan
                Text(
                    text = "مدیریت مالی، اقساط و خودرو",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp
                    ),
                    color = if (isDark) Color(0xFF34D399) else Color(0xFF0D9488),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Sleek, minimal circular loading indicator
                CircularProgressIndicator(
                    color = if (isDark) Color(0xFF34D399) else EmeraldPrimaryLight,
                    trackColor = if (isDark) Color(0x2634D399) else Color(0x2610B981),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Bottom Footer: Subtle Brand Note and Security Badge
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .alpha(contentAlpha.value),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF34D399) else EmeraldPrimaryLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "سامانه مدیریت یکپارچه دارایی‌ها",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.sp
                        ),
                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }

                Text(
                    text = "رمزنگاری محلی • نسخه ۱.۰",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 0.sp
                    ),
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }
        }
    }
}
