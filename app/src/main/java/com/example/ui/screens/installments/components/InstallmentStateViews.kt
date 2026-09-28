package com.example.ui.screens.installments.components

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.components.shimmerBrush
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun InstallmentsEmptyState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(24.dp),
        testTag = "installments_empty_state"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Soft3DIcon(
                imageRes = R.drawable.img_3d_installment,
                contentDescription = "بدون قسط",
                size = 72.dp,
                accentColor = Color(0xFF0EA5E9),
                containerShape = RoundedCornerShape(20.dp)
            )

            Text(
                text = "هنوز قسطی ثبت نشده",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "وام‌ها، بیمه‌ها و سایر تعهدات خود را اینجا مدیریت کنید.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(RadiusMD),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("empty_state_add_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "افزودن اولین قسط",
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
fun InstallmentsLoadingSkeleton(modifier: Modifier = Modifier) {
    val shimmer = shimmerBrush()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(RadiusLG))
                .background(shimmer)
        )

        // Category 2x2 Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(RadiusMD))
                    .background(shimmer)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(RadiusMD))
                    .background(shimmer)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(RadiusMD))
                    .background(shimmer)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(RadiusMD))
                    .background(shimmer)
            )
        }

        // List item skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(RadiusMD))
                .background(shimmer)
        )
    }
}

@Composable
fun InstallmentsErrorState(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(20.dp),
        testTag = "installments_error_state"
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ExpenseRoseLight.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = ExpenseRoseLight,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "دریافت اطلاعات اقساط با مشکل مواجه شد",
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedButton(
                onClick = onRetryClick,
                shape = RoundedCornerShape(RadiusMD),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.testTag("installments_retry_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "تلاش مجدد",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
