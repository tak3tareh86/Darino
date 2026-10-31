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
import com.example.util.MoneyFormatter

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

    val remainingDays = if (isActive) state.subscriptionDaysRemaining else state.trialDaysRemaining
    val isWarningActive = isActive && state.subscriptionDaysRemaining in 1..7

    // Formatted expiration date
    val endDateTime = if (info.subscriptionEndAt != null) {
        com.example.util.PersianCalendarHelper.fromEpochMillis(info.subscriptionEndAt).toFormattedDate()
    } else {
        state.subscriptionEndJalaliDate
    }

    if (isActive) {
        // Active Annual Subscriber Card
        val activeGradient = when {
            isDark && isWarningActive -> Brush.horizontalGradient(
                colors = listOf(Color(0xFF881337), Color(0xFFBE123C), Color(0xFFE11D48))
            )
            isDark -> Brush.horizontalGradient(
                colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF0F766E))
            )
            isWarningActive -> Brush.horizontalGradient(
                colors = listOf(Color(0xFFFFE4E6), Color(0xFFFFF1F2), Color(0xFFFFE4E6))
            )
            else -> Brush.horizontalGradient(
                colors = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5), Color(0xFFCCFBF1))
            )
        }
        val cardTextColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
        val cardMutedTextColor = if (isDark) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurfaceVariant

        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 210.dp)
                .testTag("active_subscription_card")
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = if (isWarningActive) Color(0xFFE11D48).copy(alpha = 0.2f) else Color(0xFF047857).copy(alpha = 0.2f),
                    spotColor = if (isWarningActive) Color(0xFF881337).copy(alpha = 0.3f) else Color(0xFF064E3B).copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(16.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(activeGradient)
                    .padding(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
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
                                color = if (isWarningActive) Color.White.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, if (isWarningActive) Color.White else Color(0xFFF59E0B)),
                                modifier = Modifier.size(34.dp).then(if (onDebugLongPress != null) Modifier.pointerInput(onDebugLongPress) { detectTapGestures(onLongPress = { onDebugLongPress() }) } else Modifier)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isWarningActive) Icons.Default.Warning else Icons.Default.WorkspacePremium,
                                        contentDescription = null,
                                        tint = if (isWarningActive) Color(0xFFFEF08A) else Color(0xFFFDE047),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isWarningActive) "اشتراک شما به‌زودی به پایان می‌رسد" else "اشتراک سالیانه فعال است",
                                    color = cardTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (isWarningActive) "هشدار انقضا" else "طلایی",
                                        color = if (isDark) Color(0xFFFEF08A) else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "دوست خوبم سلام",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "به جمع ما خوش اومدی 🌟",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "از اینکه اشتراک سالانه رو انتخاب کردی ممنونیم. حالا می‌تونی با خیال راحت از تمام امکانات استفاده کنی.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Justify,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = cardMutedTextColor
                            )
                        )
                        Text(
                            text = "امیدواریم همراه خوبی برای مدیریت بهتر امور مالی و زندگی روزمره‌ات باشیم.",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = cardMutedTextColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Expiration & Action Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val daysText = IranianPhoneUtils.convertDigitsToPersian(remainingDays.toString())
                        Column {
                            Text(
                                text = "وضعیت اعتبار",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.5.sp,
                                    color = if (isDark) Color(0xFFA7F3D0) else MaterialTheme.colorScheme.primary
                                )
                            )
                            Text(
                                text = if (endDateTime.isNotBlank()) {
                                    "$daysText روز تا پایان اشتراک باقی مانده است (انقضا: $endDateTime)"
                                } else {
                                    "$daysText روز تا پایان اشتراک باقی مانده است"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = cardTextColor
                                )
                            )
                        }

                        if (isWarningActive) Button(
                            onClick = onPurchaseClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                                contentColor = if (isDark) { if (isWarningActive) Color(0xFF881337) else Color(0xFF064E3B) } else MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "تمدید اشتراک",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                            Spacer(modifier = Modifier.width(3.dp))
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
                                        text = if (isTrial) "${IranianPhoneUtils.convertDigitsToPersian(state.trialDaysRemaining.toString())} روز مانده" else "ویژه ۳۶۵ روز",
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
                                    text = "از تمام امکانات برنامه به مدت ۱۴ روز رایگان و بدون محدودیت استفاده کن. اگر از",
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
                                text = MoneyFormatter.formatToman(state.priceDisplayToman),
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

