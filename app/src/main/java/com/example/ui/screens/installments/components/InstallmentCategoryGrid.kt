package com.example.ui.screens.installments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AppNumberFormatter
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.CategorySummaryStat
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.theme.RadiusMD

@Composable
fun InstallmentCategoryGrid(
    categories: List<CategorySummaryStat>,
    onCategoryClick: (InstallmentCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "دسته‌بندی تعهدات و اقساط",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "۴ دسته اصلی",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 2x2 Grid using Rows
        val firstRow = categories.take(2)
        val secondRow = categories.drop(2).take(2)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            firstRow.forEach { item ->
                CategoryGridCard(
                    item = item,
                    onClick = { onCategoryClick(item.category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            secondRow.forEach { item ->
                CategoryGridCard(
                    item = item,
                    onClick = { onCategoryClick(item.category) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CategoryGridCard(
    item: CategorySummaryStat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberMode by AppNumberFormatter.modeFlow.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val cardShape = RoundedCornerShape(RadiusMD)
    val gradientColors = when (item.category) {
        InstallmentCategory.BANK_LOANS -> if (isDark) {
            listOf(Color(0xFF10264A), Color(0xFF174A78), Color(0xFF173B63))
        } else {
            listOf(Color(0xFFEAF3FF), Color(0xFFD7E8FF), Color(0xFFE4F0FF))
        }
        InstallmentCategory.HOME_LOANS -> if (isDark) {
            listOf(Color(0xFF103B37), Color(0xFF176052), Color(0xFF174B48))
        } else {
            listOf(Color(0xFFE5F7F0), Color(0xFFD0EFE4), Color(0xFFE3F5EF))
        }
        InstallmentCategory.CAR_INSURANCE -> if (isDark) {
            listOf(Color(0xFF28234F), Color(0xFF443B78), Color(0xFF302F68))
        } else {
            listOf(Color(0xFFF0EDFF), Color(0xFFE1DCFF), Color(0xFFECEAFF))
        }
        InstallmentCategory.MISC -> if (isDark) {
            listOf(Color(0xFF482638), Color(0xFF71364E), Color(0xFF512C4A))
        } else {
            listOf(Color(0xFFFFEEF2), Color(0xFFFFDDE7), Color(0xFFFBEAF4))
        }
    }
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF172033)
    val secondaryColor = if (isDark) Color(0xFFD0D9E8) else Color(0xFF475569)

    Layered3DCard(
        modifier = modifier
            .clip(cardShape)
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors
                ),
                shape = cardShape
            ),
        shape = cardShape,
        backgroundColor = Color.Transparent,
        elevation = 2.dp,
        contentPadding = PaddingValues(12.dp),
        testTag = "category_card_${item.category.id}",
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Soft3DIcon(
                imageRes = item.category.iconRes,
                contentDescription = item.category.title,
                size = 38.dp,
                accentColor = item.category.accentColor,
                containerShape = RoundedCornerShape(10.dp)
            )

            Text(
                text = item.category.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                ),
                color = titleColor,
                maxLines = 1
            )

            Text(
                text = item.countText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.5.sp
                ),
                color = secondaryColor
            )

            Text(
                text = AppNumberFormatter.formatDigits(item.remainingFormatted, numberMode),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = if (isDark) Color(0xFFFFFFFF) else item.category.accentColor,
                maxLines = 1
            )
        }
    }
}
