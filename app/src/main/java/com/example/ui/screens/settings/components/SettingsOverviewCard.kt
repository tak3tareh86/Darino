package com.example.ui.screens.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun SettingsOverviewCard(
    accountCount: Int = 3,
    vehicleCount: Int = 2,
    activeInstallmentCount: Int = 6,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val cardGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF131D31),
                Color(0xFF1E293B),
                Color(0xFF114B44)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A),
                Color(0xFF1E293B),
                Color(0xFF0F766E)
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 10.dp else 8.dp,
                shape = RoundedCornerShape(RadiusLG),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color(0xFF0D9488).copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(RadiusLG),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.25f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(20.dp)
                .testTag("settings_overview_card")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top row with 3D Wallet Icon & Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .shadow(8.dp, RoundedCornerShape(RadiusMD), spotColor = Color(0xFF14B8A6))
                                .clip(RoundedCornerShape(RadiusMD))
                                .background(Color.White.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_3d_wallet),
                                contentDescription = "مدیریت مالی",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(RadiusMD)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column {
                            Text(
                                text = "مدیریت مالی من",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = Color.White
                            )

                            Text(
                                text = "تنظیمات حساب، دارایی‌ها و برنامه",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp
                                ),
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }

                    // Active state badge
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF34D399))
                            )
                            Text(
                                text = "همگام و فعال",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color(0xFF34D399)
                            )
                        }
                    }
                }

                // Stats chips row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OverviewStatChip(
                        title = "حساب‌ها",
                        value = "$accountCount حساب",
                        iconEmoji = "💳",
                        modifier = Modifier.weight(1f)
                    )

                    OverviewStatChip(
                        title = "خودروها",
                        value = "$vehicleCount خودرو",
                        iconEmoji = "🚗",
                        modifier = Modifier.weight(1f)
                    )

                    OverviewStatChip(
                        title = "اقساط",
                        value = "$activeInstallmentCount قسط فعال",
                        iconEmoji = "📅",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewStatChip(
    title: String,
    value: String,
    iconEmoji: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(RadiusSM))
            .background(Color.White.copy(alpha = 0.10f))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$iconEmoji $title",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp
                ),
                color = Color.White.copy(alpha = 0.75f)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                ),
                color = Color.White
            )
        }
    }
}
