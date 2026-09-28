package com.example.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun HomeEmptyState(
    onAddExpense: () -> Unit = {},
    onAddInstallment: () -> Unit = {},
    onAddVehicle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Layered3DCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_empty_state"),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Soft3DIcon(
                imageRes = R.drawable.img_app_icon,
                contentDescription = "خوش آمدید",
                size = 64.dp,
                accentColor = EmeraldPrimaryLight
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "به دارینو خوش آمدید!",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "برای شروع تحلیل هوشمند و کنترل هزینه‌ها، اولین داده خود را ثبت کنید.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddExpense,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(RadiusMD),
                    modifier = Modifier.fillMaxWidth().testTag("empty_state_add_expense")
                ) {
                    Text(
                        text = "+ ثبت اولین هزینه یا درآمد",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddInstallment,
                        shape = RoundedCornerShape(RadiusMD),
                        modifier = Modifier.weight(1f).testTag("empty_state_add_installment")
                    ) {
                        Text(
                            text = "+ افزودن قسط",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onAddVehicle,
                        shape = RoundedCornerShape(RadiusMD),
                        modifier = Modifier.weight(1f).testTag("empty_state_add_vehicle")
                    ) {
                        Text(
                            text = "+ افزودن خودرو",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
