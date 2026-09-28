package com.example.ui.screens.vehicle.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OilBarrel
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

data class ServiceIndicator(
    val title: String,
    val value: String,
    val statusColor: Color,
    val iconRes: Int
)

@Composable
fun ServiceStatusOverview(
    oilRemainingText: String = "۵۰۰ کیلومتر",
    insuranceRemainingText: String = "۱۲ روز مانده",
    inspectionStatusText: String = "معتبر (تا آبان)",
    onIndicatorClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val indicators = listOf(
        ServiceIndicator(
            title = "تعویض روغن",
            value = oilRemainingText,
            statusColor = WarningAmberLight,
            iconRes = R.drawable.img_3d_oil
        ),
        ServiceIndicator(
            title = "بیمه ثالث",
            value = insuranceRemainingText,
            statusColor = InfoIndigoLight,
            iconRes = R.drawable.img_3d_car
        ),
        ServiceIndicator(
            title = "معاینه فنی",
            value = inspectionStatusText,
            statusColor = EmeraldPrimaryLight,
            iconRes = R.drawable.img_3d_wallet
        )
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        indicators.forEach { item ->
            Layered3DCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onIndicatorClick(item.title) },
                shape = RoundedCornerShape(RadiusMD),
                backgroundColor = MaterialTheme.colorScheme.surface,
                elevation = 2.dp,
                contentPadding = PaddingValues(10.dp),
                testTag = "service_indicator_${item.title}"
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Soft3DIcon(
                        imageRes = item.iconRes,
                        contentDescription = item.title,
                        size = 34.dp,
                        accentColor = item.statusColor
                    )

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )

                    Text(
                        text = item.value,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 11.sp),
                        color = item.statusColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
