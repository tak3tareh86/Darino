package com.example.ui.screens.installments.category

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import androidx.compose.runtime.remember
import com.example.ui.screens.installments.components.InstallmentCard
import com.example.ui.screens.installments.components.InstallmentProgressBar
import com.example.ui.screens.installments.model.InstallmentItem
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun HomeLoansScreen(
    loans: List<InstallmentItem>,
    onBackClick: () -> Unit,
    onLoanClick: (InstallmentItem) -> Unit,
    onAddLoanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val totalAmount = loans.sumOf { it.totalAmount }
    val paidAmount = loans.sumOf { it.paidAmount }
    val remainingAmount = loans.sumOf { it.remainingAmount }
    val progress = if (totalAmount > 0) (paidAmount.toFloat() / totalAmount.toFloat()) else 0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 3.dp,
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.05f),
                        spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0xFF0F172A).copy(alpha = 0.08f)
                    ),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("home_loans_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "بازگشت",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Soft3DIcon(
                            imageRes = R.drawable.img_3d_home,
                            contentDescription = "وام‌های خانگی",
                            size = 36.dp,
                            accentColor = WarningAmberLight,
                            containerShape = RoundedCornerShape(10.dp)
                        )

                        Text(
                            text = "وام‌های خانگی و فامیلی",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Button(
                        onClick = onAddLoanClick,
                        shape = RoundedCornerShape(RadiusMD),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmberLight),
                        modifier = Modifier.testTag("add_home_loan_btn")
                    ) {
                        Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("افزودن وام", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp))
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusLG),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    elevation = 2.dp,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموع تعهدات خانوادگی (${loans.size} مورد)",
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "باقیمانده: ${(remainingAmount / 1_000_000).toInt()}M تومان",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                color = WarningAmberLight
                            )
                        }

                        InstallmentProgressBar(
                            progress = progress,
                            activeColor = WarningAmberLight,
                            label = "پیشرفت بازپرداخت خانگی"
                        )
                    }
                }
            }

            items(loans, key = { it.id }) { loan ->
                InstallmentCard(
                    item = loan,
                    onClick = { onLoanClick(loan) }
                )
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}
