package com.example.ui.screens.reports.components

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.shimmerBrush
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun ReportEmptyState(
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 3D Analytics Asset
        Image(
            painter = painterResource(id = R.drawable.img_3d_analytics),
            contentDescription = "گزارش خالی",
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = EmeraldPrimaryLight.copy(alpha = 0.25f),
                    spotColor = EmeraldPrimaryLight.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(24.dp)),
            contentScale = ContentScale.Crop
        )

        Text(
            text = "هنوز داده‌ای برای نمایش گزارش وجود ندارد",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "با ثبت اولین درآمد یا هزینه، گزارش‌ها و نمودارهای هوشمند مالی شما به صورت خودکار در این بخش ترسیم خواهند شد.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Button(
            onClick = onAddTransactionClick,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(RadiusMD),
                    ambientColor = EmeraldPrimaryLight.copy(alpha = 0.3f),
                    spotColor = EmeraldPrimaryLight.copy(alpha = 0.5f)
                )
                .testTag("report_empty_state_add_btn"),
            shape = RoundedCornerShape(RadiusMD),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = "ثبت اولین تراکنش",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ReportLoadingState(
    modifier: Modifier = Modifier
) {
    val shimmer = shimmerBrush()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Shimmer Period Selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(RadiusLG))
                .background(shimmer)
        )

        // Shimmer Summary Card
        Layered3DCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLG),
            backgroundColor = MaterialTheme.colorScheme.surface,
            elevation = 2.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(shimmer)
                )
            }
        }

        // Shimmer Chart Card
        Layered3DCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLG),
            backgroundColor = MaterialTheme.colorScheme.surface,
            elevation = 2.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmer)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(RadiusMD))
                        .background(shimmer)
                )
            }
        }

        // Shimmer Categories
        Layered3DCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLG),
            backgroundColor = MaterialTheme.colorScheme.surface,
            elevation = 2.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(RadiusMD))
                            .background(shimmer)
                    )
                }
            }
        }
    }
}

@Composable
fun ReportErrorState(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ExpenseRoseLight.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = ExpenseRoseLight,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "نمایش گزارش با مشکل مواجه شد",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "خطایی در پردازش داده‌های مالی این بازه رخ داده است. لطفاً مجدداً تلاش نمایید.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        OutlinedButton(
            onClick = onRetryClick,
            modifier = Modifier
                .height(44.dp)
                .testTag("report_error_retry_btn"),
            shape = RoundedCornerShape(RadiusMD)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text("تلاش مجدد")
            }
        }
    }
}
