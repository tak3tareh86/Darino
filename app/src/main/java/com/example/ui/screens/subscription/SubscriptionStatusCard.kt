package com.example.ui.screens.subscription

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.subscription.MarketConfig
import com.example.domain.subscription.SubscriptionStatus
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter

@OptIn(ExperimentalLayoutApi::class)
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
    val isExpiringSoon = isActive && state.subscriptionDaysRemaining in 1..30
    val isWarningTrial = isTrial && state.trialDaysRemaining in 1..7

    // Formatted expiration and start dates
    val endDateTime = if (info.subscriptionEndAt != null) {
        com.example.util.PersianCalendarHelper.fromEpochMillis(info.subscriptionEndAt).toFormattedDate()
    } else {
        state.subscriptionEndJalaliDate
    }

    if (isActive) {
        // Active Annual Subscriber Card
        val activeGradient = Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF064E3B), // Emerald dark
                Color(0xFF047857), // Emerald medium
                Color(0xFF0F766E)  // Teal
            )
        )

        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("active_subscription_card")
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF047857).copy(alpha = 0.2f),
                    spotColor = Color(0xFF064E3B).copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenSubscription() },
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeGradient)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF59E0B).copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE047),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "اشتراک سالیانه فعال است",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "طلایی",
                                        color = Color(0xFFFEF08A),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            val daysText = IranianPhoneUtils.convertDigitsToPersian(remainingDays.toString())
                            Text(
                                text = if (endDateTime.isNotBlank()) {
                                    "انقضا: $endDateTime ($daysText روز مانده)"
                                } else {
                                    "$daysText روز از اشتراک شما باقی مانده است"
                                },
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    // Action button
                    Button(
                        onClick = onOpenSubscription,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF064E3B)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isExpiringSoon) "تمدید اشتراک" else "جزئیات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    } else {
        // Free / Inactive / Trial User Annual Subscription Offer Card
        val offerGradient = if (isExpired) {
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF881337), // Crimson dark
                    Color(0xFFBE123C),
                    Color(0xFFE11D48)
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF0F172A), // Slate navy
                    Color(0xFF1E293B),
                    Color(0xFF093733)  // Deep teal hint
                )
            )
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .testTag("annual_subscription_offer_card")
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF0D9488).copy(alpha = 0.2f),
                    spotColor = Color(0xFF0F172A).copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { onOpenSubscription() },
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(offerGradient)
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Row: Title + Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.8f)),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "اشتراک سالیانه دارینو",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = when {
                                        isExpired -> "دوره آزمایشی به اتمام رسید؛ برای فعالسازی خدمات کلیک کنید"
                                        isTrial -> "دوره آزمایشی: ${IranianPhoneUtils.convertDigitsToPersian(state.trialDaysRemaining.toString())} روز باقی‌مانده"
                                        else -> "دسترسی نامحدود به تمام امکانات مدیریت مالی"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFE2E8F0).copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }

                        // Gold Yearly Tag
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B),
                            contentColor = Color(0xFF451A03)
                        ) {
                            Text(
                                text = "ویژه ۳۶۵ روز",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Benefits row / chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SubscriptionBenefitChip(text = "ثبت خودکار پیامک‌ها")
                        SubscriptionBenefitChip(text = "پشتیبان‌گیری ابری")
                        SubscriptionBenefitChip(text = "یادآورهای هوشمند")
                    }

                    // Price & Primary CTA Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "هزینه اشتراک یکساله",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            )
                            Text(
                                text = "${MoneyFormatter.formatToman(state.priceDisplayToman)} / سالانه",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFFFEF08A)
                                )
                            )
                        }

                        Button(
                            onClick = onOpenSubscription,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D9488),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("btn_buy_annual_subscription")
                        ) {
                            Text(
                                text = "خرید اشتراک سالیانه",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
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

@Composable
private fun SubscriptionBenefitChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFF34D399),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = text,
                color = Color.White,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
