package com.example.ui.screens.reports.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.reports.model.ExpenseCategoryReportItem
import com.example.ui.screens.reports.model.ReportsMockDataSource
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

enum class FinancialPeriodFilter(val title: String) {
    DAILY("روزانه"),
    WEEKLY("هفتگی"),
    MONTHLY("ماهانه"),
    YEARLY("سالانه")
}

@Composable
fun FinancialReportDashboard(
    modifier: Modifier = Modifier,
    onCategoryClick: (ExpenseCategoryReportItem) -> Unit = {}
) {
    var selectedPeriod by remember { mutableStateOf(FinancialPeriodFilter.MONTHLY) }
    var searchQuery by remember { mutableStateOf("") }

    // Mock calculations based on selected period
    val totalExpenseFormatted = when (selectedPeriod) {
        FinancialPeriodFilter.DAILY -> "۹۵,۰۰۰ تومان"
        FinancialPeriodFilter.WEEKLY -> "۲,۱۵۰,۰۰۰ تومان"
        FinancialPeriodFilter.MONTHLY -> "۹,۵۰۰,۰۰۰ تومان"
        FinancialPeriodFilter.YEARLY -> "۱۱۸,۵۰۰,۰۰۰ تومان"
    }

    val highestExpenseTitle = when (selectedPeriod) {
        FinancialPeriodFilter.DAILY -> "خرید روغن موتور و فیلتر"
        FinancialPeriodFilter.WEEKLY -> "تعویض دیسک و صفحه کلاچ"
        FinancialPeriodFilter.MONTHLY -> "تعمیر کامل موتور و جلوبندی"
        FinancialPeriodFilter.YEARLY -> "خرید خودرو و پیش‌پرداخت مسکن"
    }

    val highestExpenseAmount = when (selectedPeriod) {
        FinancialPeriodFilter.DAILY -> "۹۵,۰۰۰ تومان"
        FinancialPeriodFilter.WEEKLY -> "۱,۲۵۰,۰۰۰ تومان"
        FinancialPeriodFilter.MONTHLY -> "۴,۳۰۰,۰۰۰ تومان"
        FinancialPeriodFilter.YEARLY -> "۴۵,۰۰۰,۰۰۰ تومان"
    }

    val highestExpenseDate = when (selectedPeriod) {
        FinancialPeriodFilter.DAILY -> "امروز - ساعت ۱۶:۳۰"
        FinancialPeriodFilter.WEEKLY -> "۲۸ مهر ۱۴۰۴"
        FinancialPeriodFilter.MONTHLY -> "مهر ۱۴۰۴"
        FinancialPeriodFilter.YEARLY -> "شهریور ۱۴۰۴"
    }

    val highestExpenseIcon = when (selectedPeriod) {
        FinancialPeriodFilter.DAILY -> R.drawable.img_3d_oil
        FinancialPeriodFilter.WEEKLY -> R.drawable.img_3d_oil
        FinancialPeriodFilter.MONTHLY -> R.drawable.img_3d_car
        FinancialPeriodFilter.YEARLY -> R.drawable.img_3d_home
    }

    // Filter categories based on search query
    val allCategories = remember { ReportsMockDataSource.expenseCategories }
    val filteredCategories = allCategories.filter {
        it.title.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Time Selection Segments (روزانه، هفتگی، ماهانه، سالانه)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(RadiusMD))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FinancialPeriodFilter.values().forEach { filter ->
                val isSelected = selectedPeriod == filter
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(RadiusMD))
                        .clickable { selectedPeriod = filter }
                        .testTag("finance_period_filter_${filter.name.lowercase()}"),
                    shape = RoundedCornerShape(RadiusMD),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.Transparent
                    )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 2. Centralized Total Expenses Card
        Layered3DCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLG),
            backgroundColor = MaterialTheme.colorScheme.surface,
            elevation = 3.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "مجموع هزینه‌های ${selectedPeriod.title}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = totalExpenseFormatted,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        ),
                        color = ExpenseRoseLight
                    )
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ExpenseRoseLight.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = ExpenseRoseLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 3. Highest Expense of Selected Period (بیشترین هزینه دوره)
        Layered3DCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLG),
            backgroundColor = MaterialTheme.colorScheme.surface,
            elevation = 3.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "بیشترین هزینه دوره (${selectedPeriod.title})",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "رتبه ۱ مخارج",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFFF59E0B)
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = highestExpenseIcon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = highestExpenseTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "تاریخ ثبت: $highestExpenseDate",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = highestExpenseAmount,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }

        // 4. Interactive Search Bar (گزینه جستجو)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reports_finance_search_input"),
            placeholder = {
                Text(
                    text = "جستجوی دسته‌بندی و هزینه‌ها...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "جستجو",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(RadiusMD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 5. Categorized Expense Type Breakdown (نوع هزینه‌ها به تفکیک)
        Layered3DCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(RadiusLG),
            backgroundColor = MaterialTheme.colorScheme.surface,
            elevation = 3.dp,
            contentPadding = PaddingValues(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Category,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تفکیک نوع هزینه‌های انجام شده",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "دسته‌بندی‌ها",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (filteredCategories.isEmpty()) {
                    Text(
                        text = "موردی یافت نشد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        filteredCategories.forEach { category ->
                            CategorizedBreakdownRow(
                                category = category,
                                onClick = { onCategoryClick(category) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorizedBreakdownRow(
    category: ExpenseCategoryReportItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMD))
            .clickable { onClick() },
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, category.accentColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = category.iconRes),
                        contentDescription = category.title,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${category.transactionCount} تراکنش ثبت شده",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = category.amountFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 13.sp),
                        color = category.accentColor
                    )
                    Text(
                        text = category.percentageFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Clean custom linear progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(category.percentage)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(category.accentColor)
                )
            }
        }
    }
}
