package com.example.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Timer
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
    val isWarning = state.isWarningTrial
    val isExpired = info.subscriptionStatus == SubscriptionStatus.EXPIRED || info.subscriptionStatus == SubscriptionStatus.LOCKED
    val isActive = info.subscriptionStatus == SubscriptionStatus.ACTIVE

    val cardBgGradient = when {
        isExpired -> Brush.horizontalGradient(
            listOf(Color(0xFF8C1D1D), Color(0xFFD32F2F))
        )
        isWarning -> Brush.horizontalGradient(
            listOf(Color(0xFFE65100), Color(0xFFF57C00))
        )
        isActive -> Brush.horizontalGradient(
            listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
        )
        else -> Brush.horizontalGradient(
            listOf(Color(0xFF0D47A1), Color(0xFF1976D2))
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenSubscription() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgGradient)
                .padding(18.dp)
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        isExpired -> Icons.Default.Lock
                                        isActive -> Icons.Default.Star
                                        else -> Icons.Default.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    isExpired -> "دوره رایگان شما به پایان رسیده است"
                                    isActive -> "اشتراک دارینو فعال است"
                                    else -> "دوره رایگان دارینو (۳۰ روزه)"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = when {
                                    isExpired -> "برای ادامه استفاده از دارینو، اشتراک سالیانه را فعال کنید."
                                    isActive -> "${state.subscriptionDaysRemaining} روز باقی مانده تا ${state.subscriptionEndJalaliDate}"
                                    else -> "${state.trialDaysRemaining} روز تا پایان مهلت استفاده رایگان"
                                },
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    if (isWarning) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            contentColor = Color(0xFFD32F2F)
                        ) {
                            Text(
                                text = "هشدار مهلت",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                            fontSize = 11.sp
                        )
                        Text(
                            text = MoneyFormatter.formatToman(MarketConfig.DISPLAY_PRICE_TOMAN),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Button(
                        onClick = onOpenSubscription,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = when {
                                isExpired -> Color(0xFFD32F2F)
                                isWarning -> Color(0xFFE65100)
                                isActive -> Color(0xFF1B5E20)
                                else -> Color(0xFF0D47A1)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isActive) "مشاهده اشتراک" else "فعالسازی اشتراک",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
