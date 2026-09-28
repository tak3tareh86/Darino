package com.example.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.subscription.MarketConfig
import com.example.domain.subscription.SubscriptionStatus
import com.example.util.MoneyFormatter

@Composable
fun SubscriptionStatusCard(
    state: SubscriptionState,
    onOpenSubscription: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = state.info
    val isExpired = info.subscriptionStatus == SubscriptionStatus.EXPIRED || info.subscriptionStatus == SubscriptionStatus.LOCKED
    val isActive = info.subscriptionStatus == SubscriptionStatus.ACTIVE
    val isTrial = info.subscriptionStatus == SubscriptionStatus.TRIAL

    val remainingDays = if (isActive) state.subscriptionDaysRemaining else state.trialDaysRemaining
    val isWarningRemaining = (isTrial && state.trialDaysRemaining in 1..7) || (isActive && state.subscriptionDaysRemaining in 1..7)

    // Purchase date and time from startAt
    val startAt = info.subscriptionStartAt ?: info.trialStartAt
    val startDateTime = com.example.util.PersianCalendarHelper.fromEpochMillis(startAt)
    val purchaseDate = startDateTime.toFormattedDate()
    val purchaseTime = startDateTime.toFormattedTime()

    val cardBgGradient = when {
        isExpired || isWarningRemaining -> Brush.horizontalGradient(
            listOf(Color(0xFF8C1D1D), Color(0xFFD32F2F)) // Red warning / expired
        )
        isActive -> Brush.horizontalGradient(
            listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)) // Elegant active green
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF0D47A1), Color(0xFF1976D2)) // Premium blue
        )
    }

    if (isActive) {
        // Compact & low-height Active Subscription Card
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenSubscription() },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBgGradient)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isWarningRemaining) Icons.Default.Lock else Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = if (isWarningRemaining) "اشتراک دارینو (رو به اتمام)" else "اشتراک دارینو فعال است",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "خرید: $purchaseDate ساعت $purchaseTime | باقی‌مانده: ${com.example.util.IranianPhoneUtils.convertDigitsToPersian(remainingDays.toString())} روز",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Left action button (very compact)
                    Surface(
                        onClick = onOpenSubscription,
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White,
                        contentColor = if (isWarningRemaining) Color(0xFFD32F2F) else Color(0xFF1B5E20),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "جزئیات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Standard full card for Trial / Unsubscribed / Expired states
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenSubscription() },
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBgGradient)
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Top Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when {
                                            isExpired || isWarningRemaining -> Icons.Default.Lock
                                            else -> Icons.Default.AutoAwesome
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when {
                                        isExpired -> "دوره رایگان شما به پایان رسیده است"
                                        isWarningRemaining -> "دوره رایگان (رو به اتمام)"
                                        else -> "دوره رایگان دارینو (۳۰ روزه)"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = when {
                                        isExpired -> "برای ادامه استفاده از دارینو، اشتراک سالیانه را فعال کنید."
                                        else -> "${com.example.util.IranianPhoneUtils.convertDigitsToPersian(remainingDays.toString())} روز تا پایان مهلت استفاده رایگان"
                                    },
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isWarningRemaining) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                contentColor = Color(0xFFD32F2F)
                            ) {
                                Text(
                                    text = "هشدار مهلت",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Price and Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "اشتراک ۳۶۵ روزه",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = MoneyFormatter.formatToman(MarketConfig.DISPLAY_PRICE_TOMAN),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = onOpenSubscription,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = if (isExpired || isWarningRemaining) Color(0xFFD32F2F) else Color(0xFF0D47A1)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "فعالسازی اشتراک",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
