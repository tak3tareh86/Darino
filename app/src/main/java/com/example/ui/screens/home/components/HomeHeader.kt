package com.example.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusXL
import com.example.util.IranianPhoneUtils

@Composable
fun HomeHeader(
    userName: String,
    todayDateText: String,
    unreadNotificationsCount: Int,
    onNotificationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val headerGradient = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF131D31),
                Color(0xFF0F172A),
                Color(0xFF0B2926)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF134E4A),
                Color(0xFF0D9488)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL),
                ambientColor = Color(0xFF0D9488).copy(alpha = 0.2f),
                spotColor = Color(0xFF0F172A).copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL))
            .background(headerGradient)
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side (RTL): App 3D Avatar/Logo + Greeting + Persian Date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 3D App Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(
                            elevation = 6.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = Color(0xFF14B8A6).copy(alpha = 0.3f),
                            spotColor = Color(0xFF14B8A6).copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_darino_logo),
                        contentDescription = "لوگوی دارینو",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Greeting Texts
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "سلام $userName 👋",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = todayDateText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = Color(0xFFE2E8F0).copy(alpha = 0.85f)
                    )
                }
            }

            // Left Side (RTL): Notification Button with Badge and Settings Button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationButton(
                    badgeCount = unreadNotificationsCount,
                    onClick = onNotificationClick
                )
                SettingsButton(
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun SettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("settings_button")
            .size(44.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color(0xFF14B8A6).copy(alpha = 0.4f)
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "تنظیمات برنامه",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun NotificationButton(
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("notification_button")
            .size(44.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color(0xFF14B8A6).copy(alpha = 0.4f)
            )
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = "مرکز اعلان‌ها",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )

        // Notification Badge Indicator
        if (badgeCount > 0) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + scaleIn(tween(300)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ExpenseRoseLight,
                    border = BorderStroke(1.5.dp, Color.White),
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = IranianPhoneUtils.convertDigitsToPersian(badgeCount.toString()),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
