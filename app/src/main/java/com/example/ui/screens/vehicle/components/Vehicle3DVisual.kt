package com.example.ui.screens.vehicle.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Vehicle3DVisual(
    @DrawableRes imageRes: Int?,
    modifier: Modifier = Modifier,
    carSize: Dp = 130.dp,
    accentGlowColor: Color = Color(0xFF3B82F6)
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Box(
        modifier = modifier.size(carSize),
        contentAlignment = Alignment.Center
    ) {
        // Ambient soft ground reflection/shadow
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 4.dp)
                .width(carSize * 0.85f)
                .height(20.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentGlowColor.copy(alpha = if (isDark) 0.45f else 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        // The 3D Car Render Asset or Fallback
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "خودرو",
                modifier = Modifier
                    .size(carSize)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.2f),
                        spotColor = accentGlowColor.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            // Flexible 3D placeholder
            Box(
                modifier = Modifier
                    .size(carSize)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsCar,
                    contentDescription = "خودرو",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(54.dp)
                )
            }
        }
    }
}
