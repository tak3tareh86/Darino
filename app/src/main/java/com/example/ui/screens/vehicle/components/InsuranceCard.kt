package com.example.ui.screens.vehicle.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.vehicle.model.VehicleInsuranceData
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun InsuranceCard(
    insurance: VehicleInsuranceData,
    modifier: Modifier = Modifier,
    onDetailsClick: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val cardBgGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF1E1B4B),
                Color(0xFF0F172A),
                Color(0xFF172554)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEFF6FF),
                Color(0xFFDBEAFE),
                Color(0xFFEDE9FE)
            )
        )
    }

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = Color.Transparent,
        elevation = 6.dp,
        contentPadding = PaddingValues(0.dp),
        testTag = "insurance_card",
        onClick = onDetailsClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgGradient)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top: 3D Shield Icon + Title + Status Countdown Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .shadow(6.dp, CircleShape, spotColor = Color(0xFFF59E0B))
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_3d_insurance),
                                contentDescription = "بیمه",
                                modifier = Modifier.size(38.dp),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = insurance.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                            Text(
                                text = "${insurance.provider} • شماره: ${insurance.policyNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    // Remaining Days Pill
                    Surface(
                        shape = CircleShape,
                        color = if (insurance.isExpiringSoon) WarningAmberLight.copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(0.8.dp, if (insurance.isExpiringSoon) WarningAmberLight.copy(alpha = 0.5f) else Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${insurance.daysRemaining} روز باقی‌مانده",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (insurance.isExpiringSoon) WarningAmberLight else Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Expiry Date & View Details Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.8f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تاریخ انقضا: ${insurance.expiryDatePersian}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "مشاهده جزئیات",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2563EB)
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
