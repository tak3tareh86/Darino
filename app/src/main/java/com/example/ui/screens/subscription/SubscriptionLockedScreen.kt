package com.example.ui.screens.subscription

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val PremiumNavy = Color(0xFF031638)
private val PremiumDeepNavy = Color(0xFF00102B)
private val PremiumGold = Color(0xFFFFC21C)
private val PremiumBlue = Color(0xFF398BFF)
private val PremiumText = Color(0xFFF6F7FF)

@Composable
fun SubscriptionLockedScreen(
    viewModel: SubscriptionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val requiresMarketReverification =
        uiState.info.marketAccountState == "MARKET_ACTIVE" &&
            uiState.info.subscriptionStatus == com.example.domain.subscription.SubscriptionStatus.LOCKED

    SubscriptionLockedScreenContent(
        uiState = uiState,
        requiresMarketReverification = requiresMarketReverification,
        onPurchaseClick = {
            // Poolakey opens the official Cafe Bazaar purchase sheet. Entitlement is
            // granted only after Poolakey returns a verified PURCHASED result.
            val activity = context as? androidx.activity.ComponentActivity
            if (activity != null) {
                viewModel.startPurchaseFlow(activity.activityResultRegistry) {
                    Toast.makeText(context, "اشتراک با موفقیت فعال شد.", Toast.LENGTH_LONG).show()
                }
            }
        },
        // Restore only queries the current Cafe Bazaar account; it never launches payment.
        onRestoreClick = { viewModel.manualRestorePurchase() },
        actionsEnabled = !uiState.isLoading && !uiState.isSyncing
    )
}

@Composable
fun SubscriptionLockedScreenContent(
    uiState: SubscriptionState,
    requiresMarketReverification: Boolean,
    onPurchaseClick: () -> Unit,
    onRestoreClick: () -> Unit,
    actionsEnabled: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF061B43),
                        0.48f to PremiumNavy,
                        1f to PremiumDeepNavy
                    )
                )
            )
    ) {
        // Soft blue corner lighting, matching the supplied premium-screen reference.
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopStart)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF1769D5).copy(alpha = 0.34f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(320.dp)
                .align(Alignment.BottomEnd)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF0876C9).copy(alpha = 0.24f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            ImageLogo()

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f).height(2.dp).background(PremiumGold))
                Text(
                    text = "🤝",
                    fontSize = 48.sp,
                    lineHeight = 54.sp
                )
                Text(
                    text = "دوست خوبم",
                    color = PremiumGold,
                    fontSize = 35.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Box(Modifier.weight(1f).height(2.dp).background(PremiumGold))
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = if (requiresMarketReverification) {
                    "برای تأیید دوباره اشتراک، لازم است وضعیت خرید شما از کافه‌بازار بازیابی شود."
                } else {
                    "مدت استفاده رایگان ۷ روزه شما از برنامه به پایان رسیده است."
                },
                modifier = Modifier.fillMaxWidth(),
                color = PremiumText,
                fontSize = 23.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            ExpiredSubscriptionIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(286.dp)
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (requiresMarketReverification) {
                    "اگر قبلاً اشتراک تهیه کرده‌اید، گزینه بازیابی خرید قبلی را بزنید."
                } else {
                    "برای استفاده مجدد از امکانات برنامه و ادامه مدیریت امور مالی و زندگی روزمره خود، لطفاً اشتراک سالانه تهیه کنید."
                },
                modifier = Modifier.fillMaxWidth(),
                color = PremiumText,
                fontSize = 20.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // The displayed amount is always the Bazaar SKU display price from Poolakey.
            // No price is hard-coded in the UI.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(34.dp),
                color = Color(0xFF061A3A).copy(alpha = 0.82f),
                border = BorderStroke(2.dp, PremiumGold)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💎",
                        fontSize = 54.sp,
                        modifier = Modifier.width(82.dp),
                        textAlign = TextAlign.Center
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(76.dp)
                            .background(PremiumGold)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "اشتراک سالانه",
                            color = PremiumText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        when {
                            uiState.isPriceLoading && uiState.priceDisplay == null -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(25.dp),
                                    color = PremiumGold,
                                    strokeWidth = 2.5.dp
                                )
                            }
                            uiState.priceDisplay != null -> {
                                Text(
                                    text = uiState.priceDisplay,
                                    color = PremiumGold,
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                            else -> {
                                Text(
                                    text = "قیمت از بازار دریافت نشد",
                                    color = Color(0xFFFFD98A),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        if (uiState.priceDisplay != null) {
                            Text(
                                text = if (uiState.isPriceCached) {
                                    "آخرین قیمت ذخیره‌شده کافه‌بازار"
                                } else {
                                    "قیمت به‌روز کافه‌بازار"
                                },
                                color = Color(0xFFB8C8E8),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Button(
                onClick = onPurchaseClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PremiumGold,
                    contentColor = Color(0xFF07152E),
                    disabledContainerColor = PremiumGold.copy(alpha = 0.55f),
                    disabledContentColor = Color(0xFF07152E)
                ),
                border = BorderStroke(2.dp, Color(0xFFFFE47A)),
                enabled = actionsEnabled
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(25.dp),
                        color = Color(0xFF07152E),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "خرید اشتراک سالانه",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(14.dp))
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(31.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            OutlinedButton(
                onClick = onRestoreClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(2.dp, PremiumBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = PremiumText,
                    disabledContentColor = PremiumText.copy(alpha = 0.55f)
                ),
                enabled = actionsEnabled
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(23.dp),
                        color = PremiumText,
                        strokeWidth = 2.2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "بازیابی خرید قبلی",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(29.dp)
                        )
                    }
                }
            }

            if (!uiState.userMessage.isNullOrBlank()) {
                Spacer(Modifier.height(13.dp))
                Text(
                    text = uiState.userMessage.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (uiState.isError) Color(0xFFFFA5A5) else Color(0xFF9BE8B1),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(26.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF62799E)))
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF5BD36A),
                    modifier = Modifier.size(30.dp)
                )
                Text(
                    text = "اطلاعات شما محفوظ است",
                    color = Color(0xFFC5D0E4),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF62799E)))
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ImageLogo() {
    androidx.compose.foundation.Image(
        painter = painterResource(id = com.example.R.drawable.img_darino_logo),
        contentDescription = "لوگوی دارینو",
        modifier = Modifier
            .width(190.dp)
            .height(88.dp),
        contentScale = ContentScale.Fit
    )
}

/** A layered Compose illustration echoing the calendar, clock and crown in the reference. */
@Composable
private fun ExpiredSubscriptionIllustration(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.74f)
                .height(26.dp)
                .align(Alignment.BottomCenter)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF27B9FF).copy(alpha = 0.75f), Color.Transparent)
                    )
                )
        )

        Surface(
            modifier = Modifier
                .width(236.dp)
                .height(202.dp)
                .align(Alignment.Center)
                .padding(start = 20.dp, top = 5.dp)
                .rotate(-5f),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFE8EEFF),
            shadowElevation = 14.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(49.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF2679FF), Color(0xFF0750C9))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("●   ●   ●   ●", color = Color(0xFFB9D8FF), fontSize = 21.sp)
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(31.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color(0xFFD2DCF4))
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(142.dp)
                .align(Alignment.BottomStart)
                .padding(start = 10.dp, bottom = 3.dp),
            shape = CircleShape,
            color = Color(0xFF1766E8),
            border = BorderStroke(5.dp, Color(0xFF73C8FF)),
            shadowElevation = 18.dp
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White, Color(0xFFDCE7FF), Color(0xFF9DBBFA))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◷",
                    color = Color(0xFF123B88),
                    fontSize = 91.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(66.dp)
                .align(Alignment.Center)
                .padding(start = 98.dp, bottom = 45.dp),
            shape = CircleShape,
            color = Color(0xFFFFF2E9),
            border = BorderStroke(4.dp, Color(0xFFFF5A45)),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("×", color = Color(0xFFE83F32), fontSize = 54.sp, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            text = "👑",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 6.dp, bottom = 8.dp)
                .rotate(9f),
            fontSize = 94.sp
        )
    }
}
