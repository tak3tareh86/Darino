package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

/**
 * Premium 3D Card with layered depth, inner highlight, dual soft shadow, and micro-press interaction.
 */
@Composable
fun Layered3DCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(RadiusLG),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 4.dp,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f),
        label = "CardPressScale"
    )

    val currentElevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else elevation.value,
        animationSpec = spring(stiffness = 400f),
        label = "CardPressElevation"
    )

    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val topHighlightBrush = remember(isDark) {
        Brush.verticalGradient(
            colors = listOf(
                if (isDark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.8f),
                Color.Transparent
            )
        )
    }

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                if (testTag != null) Modifier.testTag(testTag) else Modifier
            )
            .shadow(
                elevation = currentElevation.dp,
                shape = shape,
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF0F172A).copy(alpha = 0.08f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.8f) else Color(0xFF0F172A).copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = topHighlightBrush
                ),
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * Premium 3D Hero Icon Container that supports high-resolution rendered 3D assets
 * or fallback styled vectors with ambient glow and embossed claymorphism effect.
 */
@Composable
fun Soft3DIcon(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    @DrawableRes imageRes: Int? = null,
    vectorIcon: ImageVector? = null,
    contentDescription: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    containerShape: Shape = RoundedCornerShape(RadiusMD)
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = containerShape,
                ambientColor = accentColor.copy(alpha = 0.35f),
                spotColor = accentColor.copy(alpha = 0.5f)
            )
            .clip(containerShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isDark) 0.28f else 0.15f),
                        accentColor.copy(alpha = if (isDark) 0.12f else 0.05f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.35f else 0.85f),
                        accentColor.copy(alpha = 0.2f)
                    )
                ),
                shape = containerShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(size * 0.92f)
                    .clip(containerShape),
                contentScale = ContentScale.Crop
            )
        } else if (vectorIcon != null) {
            androidx.compose.material3.Icon(
                imageVector = vectorIcon,
                contentDescription = contentDescription,
                tint = accentColor,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

/**
 * Status Pill Badge with 3D Depth.
 */
@Composable
fun Soft3DBadge(
    text: String,
    modifier: Modifier = Modifier,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 2.dp,
                shape = CircleShape,
                ambientColor = badgeColor.copy(alpha = 0.3f),
                spotColor = badgeColor.copy(alpha = 0.4f)
            ),
        shape = CircleShape,
        color = badgeColor.copy(alpha = 0.15f),
        border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * Shimmer Loading Brush for Skeleton states.
 */
@Composable
fun shimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition(label = "ShimmerTransition")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ShimmerTranslate"
        )
        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnimation - 200f, translateAnimation - 200f),
            end = Offset(translateAnimation, translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}
