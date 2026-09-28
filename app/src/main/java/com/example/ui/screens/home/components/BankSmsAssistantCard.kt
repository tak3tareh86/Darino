package com.example.ui.screens.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.home.domain.BankSmsSuggestion
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun BankSmsAssistantCard(
    queue: List<BankSmsSuggestion>,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onSimulateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (queue.isEmpty()) return

    val currentSuggestion = queue.first()
    val remainingCount = queue.size - 1
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    // Luxury background border styling for modern visual polish
    val glowBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF14B8A6),
            Color(0xFF6366F1),
            Color(0xFFF43F5E)
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bank_sms_assistant_card"),
        shape = RoundedCornerShape(RadiusLG),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F131E) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(1.5.dp, if (isDark) Color(0xFF232D42) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Icon + Title + Queue Indicator + Simulate Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isDark) Color(0xFF16252C) else Color(0xFFE6F4EA),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sms,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF14B8A6) else EmeraldPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "دستیار هوشمند پیامک بانکی",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (remainingCount > 0) {
                            Text(
                                text = "۱ از ${queue.size} پیامک معلق در صف بررسی",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = if (isDark) Color(0xFF6366F1) else Color(0xFF4F46E5)
                            )
                        } else {
                            Text(
                                text = "تراکنش بانکی موقت نیاز به تأیید شما",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Simulate incoming SMS action button
                TextButton(
                    onClick = onSimulateClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color(0xFFEAB308)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "پیامک آزمایشی 📲",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFEAB308)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sliding animation when top SMS suggestion changes
            AnimatedContent(
                targetState = currentSuggestion,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "sms_queue_transition"
            ) { suggestion ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF161E2E) else Color(0xFFF8FAFC),
                            RoundedCornerShape(RadiusMD)
                        )
                        .padding(12.dp)
                ) {
                    // Bank Name, Amount, Type (Withdrawal vs Deposit)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(RadiusSM),
                                color = if (suggestion.isExpense) {
                                    if (isDark) Color(0xFF3F161C) else Color(0xFFFFECEE)
                                } else {
                                    if (isDark) Color(0xFF0F3A22) else Color(0xFFE6F9EE)
                                },
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (suggestion.isExpense) Icons.Rounded.TrendingDown else Icons.Rounded.TrendingUp,
                                        contentDescription = null,
                                        tint = if (suggestion.isExpense) ExpenseRoseLight else EmeraldPrimaryLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (suggestion.isExpense) "برداشت" else "واریز",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (suggestion.isExpense) ExpenseRoseLight else EmeraldPrimaryLight
                                    )
                                }
                            }

                            Text(
                                text = suggestion.bankName,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = suggestion.formattedAmount,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (suggestion.isExpense) ExpenseRoseLight else EmeraldPrimaryLight
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Raw SMS Content representation
                    Text(
                        text = suggestion.smsText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "دسته‌بندی خودکار پیشنهادی: ${suggestion.category}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        }

                        Text(
                            text = suggestion.dateText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dismiss Button
                OutlinedButton(
                    onClick = { onDismiss(currentSuggestion.id) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(RadiusMD),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF324158) else Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "رد کردن",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Accept Button
                Button(
                    onClick = { onAccept(currentSuggestion.id) },
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF14B8A6) else EmeraldPrimaryLight,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تأیید و ثبت هوشمند",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
