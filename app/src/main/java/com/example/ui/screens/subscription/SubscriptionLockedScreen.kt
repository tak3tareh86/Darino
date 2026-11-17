package com.example.ui.screens.subscription

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val PremiumNavy = Color(0xFF031638)
private val PremiumDeepNavy = Color.Black
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        val compact = maxHeight < 760.dp
        val tiny = maxHeight < 680.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (tiny) 16.dp else 20.dp, vertical = if (tiny) 3.dp else 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(Modifier.height(2.dp))

            ImageLogo()

            Spacer(Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.weight(1f).height(2.dp).background(PremiumGold))
                HandshakeMark(Modifier.size(width = 40.dp, height = 36.dp))
                Text(
                    text = "دوست خوبم",
                    color = PremiumGold,
                    fontSize = if (tiny) 22.sp else 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Box(Modifier.weight(1f).height(2.dp).background(PremiumGold))
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = if (requiresMarketReverification) {
                    "برای تأیید دوباره اشتراک، لازم است وضعیت خرید شما از کافه‌بازار بازیابی شود."
                } else {
                    "مدت استفاده رایگان ۷ روزه شما از برنامه به پایان رسیده است."
                },
                modifier = Modifier.fillMaxWidth(),
                color = PremiumText,
                fontSize = if (tiny) 14.sp else 16.sp,
                lineHeight = if (tiny) 19.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(2.dp))

            ExpiredSubscriptionIllustration(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (tiny) 105.dp else if (compact) 125.dp else 145.dp)
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = if (requiresMarketReverification) {
                    "اگر قبلاً اشتراک تهیه کرده‌اید، گزینه بازیابی خرید قبلی را بزنید."
                } else {
                    "برای استفاده مجدد از امکانات برنامه و ادامه مدیریت امور مالی و زندگی روزمره خود، لطفاً اشتراک سالانه تهیه کنید."
                },
                modifier = Modifier.fillMaxWidth(),
                color = PremiumText,
                fontSize = if (tiny) 13.sp else 14.sp,
                lineHeight = if (tiny) 18.sp else 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(3.dp))

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
                        .padding(horizontal = 14.dp, vertical = if (tiny) 5.dp else 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiamondMark(Modifier.width(48.dp).height(38.dp))
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(42.dp)
                            .background(PremiumGold)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "اشتراک سالانه",
                            color = PremiumText,
                            fontSize = if (tiny) 15.sp else 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(1.dp))
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
                                    fontSize = if (tiny) 18.sp else 20.sp,
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
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            Button(
                onClick = onPurchaseClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (tiny) 48.dp else 52.dp),
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
                            fontSize = if (tiny) 15.sp else 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.width(14.dp))
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            OutlinedButton(
                onClick = onRestoreClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (tiny) 44.dp else 48.dp),
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
                            fontSize = if (tiny) 14.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(16.dp))
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            }

            if (!uiState.userMessage.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = uiState.userMessage.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (uiState.isError) Color(0xFFFFA5A5) else Color(0xFF9BE8B1),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(2.dp))

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
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = "اطلاعات شما محفوظ است",
                    color = Color(0xFFC5D0E4),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(Modifier.weight(1f).height(1.dp).background(Color(0xFF62799E)))
            }

            Spacer(Modifier.height(1.dp))
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



@Composable
private fun HandshakeMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawContext.transform.scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero)
        val gold = Color(0xFFFFBE16)
        val ink = Color(0xFF09234D)
        val hand = Path().apply {
            moveTo(7f, 35f); lineTo(25f, 17f); lineTo(39f, 21f); lineTo(50f, 32f)
            lineTo(62f, 19f); lineTo(76f, 23f); lineTo(94f, 41f); lineTo(77f, 59f)
            lineTo(54f, 83f); lineTo(43f, 73f); lineTo(21f, 53f); close()
        }
        drawPath(hand, gold)
        drawLine(ink, Offset(38f, 29f), Offset(27f, 40f), 8f)
        drawLine(ink, Offset(27f, 40f), Offset(39f, 51f), 8f)
        drawLine(ink, Offset(50f, 34f), Offset(68f, 51f), 8f)
        drawLine(ink, Offset(41f, 49f), Offset(56f, 64f), 7f)
        drawLine(ink, Offset(33f, 58f), Offset(47f, 72f), 7f)
        drawLine(ink, Offset(26f, 66f), Offset(38f, 77f), 7f)
    }
}

@Composable
private fun DiamondMark(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawContext.transform.scale(size.width / 100f, size.height / 80f, pivot = Offset.Zero)
        val gem = Path().apply {
            moveTo(20f, 8f); lineTo(80f, 8f); lineTo(98f, 29f); lineTo(50f, 76f); lineTo(2f, 29f); close()
        }
        drawPath(gem, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFE34F), Color(0xFFFFA900)), startY = 0f, endY = 80f))
        drawLine(Color(0xFF9B6900), Offset(20f, 8f), Offset(50f, 76f), 2f)
        drawLine(Color(0xFF9B6900), Offset(80f, 8f), Offset(50f, 76f), 2f)
        drawLine(Color(0xFF9B6900), Offset(2f, 29f), Offset(98f, 29f), 2f)
    }
}

@Composable
private fun ExpiredSubscriptionIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val s = minOf(size.width / 400f, size.height / 270f)
        drawContext.transform.scale(s, s, pivot = Offset.Zero)
        val gold = Color(0xFFFFC21C)
        val blue = Color(0xFF1768E8)
        drawOval(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFF20BFFF).copy(alpha = .7f), Color.Transparent)), Offset(40f, 220f), Size(320f, 38f))
        val leaf = Path().apply { moveTo(70f, 210f); quadraticTo(5f, 135f, 36f, 88f); quadraticTo(93f, 94f, 111f, 177f); close() }
        drawPath(leaf, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF328DFF), Color(0xFF0647B8))))
        val leaf2 = Path().apply { moveTo(320f, 211f); quadraticTo(345f, 137f, 391f, 153f); quadraticTo(391f, 207f, 337f, 231f); close() }
        drawPath(leaf2, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF328DFF), Color(0xFF0647B8))))
        drawRoundRect(Color(0x66000A24), Offset(111f, 39f), Size(220f, 183f), androidx.compose.ui.geometry.CornerRadius(23f))
        drawRoundRect(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color.White, Color(0xFFCAD7F4))), Offset(104f, 28f), Size(220f, 184f), androidx.compose.ui.geometry.CornerRadius(22f))
        drawRoundRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF398AFF), Color(0xFF064CC5))), Offset(104f, 28f), Size(220f, 65f), androidx.compose.ui.geometry.CornerRadius(22f))
        drawRect(Color(0xFF175FD9), Offset(104f, 70f), Size(220f, 23f))
        for (x in listOf(139f, 184f, 229f, 274f)) {
            drawLine(Color(0xFF063C9E), Offset(x, 13f), Offset(x, 48f), 13f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawLine(Color(0xFF83D5FF), Offset(x - 3f, 12f), Offset(x - 3f, 42f), 7f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        for (r in 0..2) for (col in 0..3) {
            val x = 122f + col * 47f
            val y = 104f + r * 34f
            drawRoundRect(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFE9EEFF), Color(0xFFB8C8EE))), Offset(x, y), Size(34f, 27f), androidx.compose.ui.geometry.CornerRadius(6f))
        }
        drawCircle(Color(0xFF073A9C), 77f, Offset(91f, 202f))
        drawCircle(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF8ADFFF), blue)), 72f, Offset(88f, 196f))
        drawCircle(Color(0xFF0C4FC5), 63f, Offset(88f, 196f))
        drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color.White, Color(0xFFE2E9FF), Color(0xFFB4C8F8))), 55f, Offset(88f, 196f))
        for (a in 0 until 12) {
            val rad = Math.toRadians(a * 30.0)
            val p1 = Offset(88f + kotlin.math.sin(rad).toFloat() * 44f, 196f - kotlin.math.cos(rad).toFloat() * 44f)
            val p2 = Offset(88f + kotlin.math.sin(rad).toFloat() * 49f, 196f - kotlin.math.cos(rad).toFloat() * 49f)
            drawLine(Color(0xFF244C9B), p1, p2, if (a % 3 == 0) 4f else 2f)
        }
        drawLine(Color(0xFF103B8E), Offset(88f, 196f), Offset(88f, 159f), 5f)
        drawLine(Color(0xFF103B8E), Offset(88f, 196f), Offset(124f, 196f), 5f)
        drawCircle(Color(0xFF103B8E), 7f, Offset(88f, 196f))
        drawCircle(Color(0xFFFFF7F3), 30f, Offset(253f, 130f))
        drawCircle(Color(0xFFFF604D), 27f, Offset(253f, 130f), style = Stroke(5f))
        drawLine(Color(0xFFE83E32), Offset(239f, 116f), Offset(267f, 144f), 7f)
        drawLine(Color(0xFFE83E32), Offset(267f, 116f), Offset(239f, 144f), 7f)
        val crown = Path().apply {
            moveTo(292f, 181f); lineTo(277f, 132f); lineTo(310f, 158f); lineTo(329f, 111f)
            lineTo(345f, 157f); lineTo(373f, 133f); lineTo(361f, 190f); quadraticTo(328f, 208f, 292f, 181f); close()
        }
        drawPath(crown, androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFFFFE66A), Color(0xFFFFB000), Color(0xFFE98500))))
        drawPath(crown, gold, style = Stroke(3f))
        drawRoundRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFFFD34A), Color(0xFFE98900))), Offset(286f, 179f), Size(80f, 24f), androidx.compose.ui.geometry.CornerRadius(10f))
        for (p in listOf(Offset(277f, 130f), Offset(329f, 108f), Offset(373f, 131f))) drawCircle(gold, 8f, p)
        drawLine(gold, Offset(300f, 73f), Offset(305f, 49f), 5f)
        drawLine(gold, Offset(328f, 88f), Offset(345f, 69f), 5f)
        drawLine(gold, Offset(355f, 105f), Offset(376f, 99f), 5f)
    }
}
