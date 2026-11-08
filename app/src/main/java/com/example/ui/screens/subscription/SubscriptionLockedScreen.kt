package com.example.ui.screens.subscription

import android.widget.Toast
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.subscription.MarketConfig
import com.example.util.MoneyFormatter

private val ExpiredBackgroundTop = Color(0xFF111A30)
private val ExpiredBackgroundBottom = Color(0xFF080F20)
private val ExpiredAccent = Color(0xFF18C99A)
private val ExpiredPurple = Color(0xFF5147B8)

@Composable
fun SubscriptionLockedScreen(
    viewModel: SubscriptionViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ExpiredBackgroundTop, ExpiredBackgroundBottom)
                )
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            // Calendar and expired-time illustration.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF263D78).copy(alpha = 0.72f), Color.Transparent)
                            )
                        )
                )

                Surface(
                    modifier = Modifier
                        .size(width = 152.dp, height = 142.dp)
                        .align(Alignment.Center)
                        .padding(end = 16.dp, top = 6.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF8DB5FF)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF6EA7F5), Color(0xFFDCE8FF))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF315DAA),
                            modifier = Modifier.size(88.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(112.dp)
                        .align(Alignment.Center)
                        .padding(start = 94.dp, top = 66.dp),
                    shape = CircleShape,
                    color = ExpiredPurple,
                    border = androidx.compose.foundation.BorderStroke(5.dp, Color(0xFF8277E8))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF27345E), Color(0xFF111A35))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFFDCE5FF),
                            modifier = Modifier.size(62.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(58.dp)
                        .align(Alignment.Center)
                        .padding(end = 142.dp, top = 18.dp),
                    shape = CircleShape,
                    color = Color(0xFFEC4E88)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "دوست خوبم 🌟",
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                fontSize = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "مدت زمان استفاده رایگان شما از برنامه به پایان رسیده است.",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE1E7F5),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 29.sp,
                textAlign = TextAlign.Justify
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "برای استفاده مجدد از امکانات برنامه و ادامه مدیریت امور مالی و زندگی روزمره خود، لطفاً اشتراک سالانه تهیه کنید.",
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFBAC6E0),
                fontSize = 16.sp,
                lineHeight = 29.sp,
                textAlign = TextAlign.Justify
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Annual subscription price.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF192443).copy(alpha = 0.92f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, ExpiredPurple.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC94A),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "اشتراک سالانه",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = MoneyFormatter.formatToman(MarketConfig.DISPLAY_PRICE_TOMAN),
                        color = ExpiredAccent,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val activity = context as? androidx.activity.ComponentActivity
                    if (activity != null) {
                        viewModel.startPurchaseFlow(activity.activityResultRegistry) {
                            Toast.makeText(context, "اشتراک با موفقیت فعال شد.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExpiredAccent,
                    contentColor = Color.White
                ),
                enabled = !uiState.isLoading && !uiState.isSyncing
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            modifier = Modifier.size(25.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "خرید اشتراک سالانه",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = { viewModel.manualRestorePurchase() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF64739C)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE1E7F5)
                ),
                enabled = !uiState.isLoading && !uiState.isSyncing
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(21.dp),
                        color = Color(0xFFE1E7F5),
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "بازیابی خرید قبلی",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!uiState.userMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.userMessage.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    color = if (uiState.isError) Color(0xFFFF9AAE) else Color(0xFF9BE8CE),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF8290B3),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "اطلاعات شما محفوظ است",
                    color = Color(0xFF8290B3),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}
