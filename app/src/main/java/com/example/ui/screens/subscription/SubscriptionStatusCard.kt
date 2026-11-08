package com.example.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.subscription.SubscriptionStatus
import com.example.util.IranianPhoneUtils

@Composable
fun SubscriptionStatusCard(
    state: SubscriptionState,
    onPurchaseClick: () -> Unit = {},
    onRestoreClick: () -> Unit = {},
    isPurchaseLoading: Boolean = false,
    isRestoreLoading: Boolean = false,
    onDebugLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val info = state.info
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val isExpired = info.subscriptionStatus == SubscriptionStatus.EXPIRED || info.subscriptionStatus == SubscriptionStatus.LOCKED
    val isActive = info.subscriptionStatus == SubscriptionStatus.ACTIVE
    val isTrial = info.subscriptionStatus == SubscriptionStatus.TRIAL

    if (isActive) {
        // Compact active-subscription card. Never invent an expiry date that Market does not provide.
        val activeGradient = if (isDark) {
            Brush.horizontalGradient(
                colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF0F766E))
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFFCCFBF1))
            )
        }
        val cardTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        val cardMutedTextColor = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurfaceVariant
        val marketConfirmedActive = info.marketAccountState == "MARKET_ACTIVE"

        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .testTag("active_subscription_card")
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF0D9488).copy(alpha = 0.2f),
                    spotColor = Color(0xFF0F172A).copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(activeGradient)
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right side: active status and Jalali expiration date.
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF059669).copy(alpha = if (isDark) 0.35f else 0.16f),
                        contentColor = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "فعال",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "تاریخ انقضا",
                            color = cardMutedTextColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = if (marketConfirmedActive && info.subscriptionEndAt != null) state.subscriptionEndJalaliDate else "نامشخص",
                        color = cardTextColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .width(1.dp)
                        .height(70.dp)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.28f)
                            else Color(0xFF047857).copy(alpha = 0.25f)
                        )
                )

                // Left side: Darino logo, centered active-subscription title and remaining time.
                Row(
                    modifier = Modifier.weight(1.25f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "اشتراک فعال",
                                color = cardTextColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                                contentDescription = "اشتراک فعال",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (marketConfirmedActive) "تأییدشده توسط بازار" else "وضعیت محلی اشتراک",
                            color = if (marketConfirmedActive) cardMutedTextColor else if (isDark) Color(0xFFA7F3D0) else Color(0xFF047857),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        if (marketConfirmedActive && info.subscriptionEndAt != null) {
                            Text(
                                text = "${IranianPhoneUtils.convertDigitsToPersian(state.subscriptionDaysRemaining.toString())} روز باقی‌مانده",
                                color = cardMutedTextColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }

                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_darino_logo),
                        contentDescription = "لوگوی دارینو",
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }
            }
        }
    } else {
        // Free / Inactive / Trial User Annual Subscription Offer Card
        val offerGradient = when {
            isDark && isExpired -> Brush.horizontalGradient(
                colors = listOf(Color(0xFF4C0519), Color(0xFF881337), Color(0xFF9F1239))
            )
            isDark -> Brush.horizontalGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF093733))
            )
            isExpired -> Brush.horizontalGradient(
                colors = listOf(Color(0xFFFFE4E6), Color(0xFFFFF1F2), Color(0xFFFFE4E6))
            )
            else -> Brush.horizontalGradient(
                colors = listOf(Color(0xFFEFF6FF), Color(0xFFE0F2FE), Color(0xFFCCFBF1))
            )
        }
        val offerTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        val offerMutedTextColor = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurfaceVariant

        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .testTag("annual_subscription_offer_card")
                .shadow(
                    elevation = 5.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF0D9488).copy(alpha = 0.2f),
                    spotColor = Color(0xFF0F172A).copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(16.dp))
                ,
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(offerGradient)
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header: Star + greeting on one side, remaining-days badge on the other.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.8f)),
                                    modifier = Modifier.size(34.dp).then(if (onDebugLongPress != null) Modifier.pointerInput(onDebugLongPress) { detectTapGestures(onLongPress = { onDebugLongPress() }) } else Modifier)
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

                                // Gold Yearly Tag
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF59E0B),
                                    contentColor = Color(0xFF451A03)
                                ) {
                                    Text(
                                        text = if (isTrial) "${IranianPhoneUtils.convertDigitsToPersian(state.trialDaysRemaining.toString())} روز مانده" else "اشتراک سالیانه",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (isExpired) "پایان دوره آزمایشی" else "دوست خوبم سلام",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = offerTextColor
                                )
                            )
                        }
                        // Description starts immediately below the aligned header row.
                        Spacer(modifier = Modifier.height(2.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isExpired) {
                                Text(
                                    text = "دوره آزمایشی تمام شده؛ برای ادامه اشتراک سالیانه تهیه کنید.",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Justify,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        lineHeight = 15.sp,
                                        color = offerMutedTextColor
                                    )
                                )
                            } else {
                                Text(
                                    text = "از تمام امکانات برنامه به مدت ۷ روز رایگان و بدون محدودیت استفاده کن. اگر از",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Justify,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        lineHeight = 15.sp,
                                        color = offerMutedTextColor
                                    )
                                )
                                Text(
                                    text = "تجربه کار با برنامه رضایت داشتی پس از پایان دوره آزمایشی اشتراک سالیانه را تهیه",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Justify,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        lineHeight = 15.sp,
                                        color = offerMutedTextColor
                                    )
                                )
                                Text(
                                    text = "کن و با خیال راحت به استفاده از امکانات برنامه ادامه بده.",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.5.sp,
                                        lineHeight = 15.sp,
                                        color = offerTextColor
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

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
                                    color = offerMutedTextColor
                                )
                            )
                            Text(
                                text = state.priceDisplay ?: "قیمت در بازار دریافت نشد",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFEF08A)
                                )
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // In RTL layout the purchase action appears to the right of restore.
                            Button(
                                onClick = onPurchaseClick,
                                enabled = !isPurchaseLoading && !isRestoreLoading,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0D9488),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_buy_annual_subscription")
                            ) {
                                if (isPurchaseLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(15.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = "خرید اشتراک سالیانه",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onRestoreClick,
                                enabled = !isPurchaseLoading && !isRestoreLoading,
                                shape = RoundedCornerShape(9.dp),
                                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outline),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_restore_subscription")
                            ) {
                                if (isRestoreLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(13.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "بازیابی خرید",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 9.5.sp,
                                        color = offerTextColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

