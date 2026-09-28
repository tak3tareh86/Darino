package com.example.ui.screens.home.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.home.model.UpcomingPaymentItemData
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight

@Composable
fun UpcomingItemsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("همه") }
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val allItems = remember {
        listOf(
            UpcomingPaymentItemData(
                id = "up_1",
                title = "قسط وام مسکن ملی",
                categoryName = "وام بانکی",
                dueDatePersian = "۲۵ شهریور",
                amountFormatted = "۴,۵۰۰,۰۰۰ تومان",
                daysRemainingText = "۲ روز مانده",
                isUrgent = true,
                iconRes = R.drawable.img_3d_bank,
                accentColor = ExpenseRoseLight
            ),
            UpcomingPaymentItemData(
                id = "up_2",
                title = "بیمه شخص ثالث پژو ۲۰۷",
                categoryName = "بیمه و خودرو",
                dueDatePersian = "۳۰ شهریور",
                amountFormatted = "۸,۰۰۰,۰۰۰ تومان",
                daysRemainingText = "۷ روز مانده",
                isUrgent = false,
                iconRes = R.drawable.img_3d_car,
                accentColor = InfoIndigoLight
            ),
            UpcomingPaymentItemData(
                id = "up_3",
                title = "قسط صندوق خانوادگی مهر",
                categoryName = "وام خانگی",
                dueDatePersian = "۲ مهر",
                amountFormatted = "۲,۰۰۰,۰۰۰ تومان",
                daysRemainingText = "۹ روز مانده",
                isUrgent = false,
                iconRes = R.drawable.img_3d_home,
                accentColor = WarningAmberLight
            ),
            UpcomingPaymentItemData(
                id = "up_4",
                title = "سرویس تعویض روغن و فیلتر",
                categoryName = "بیمه و خودرو",
                dueDatePersian = "۵۰۰ کیلومتر دیگر",
                amountFormatted = "۱,۲۵۰,۰۰۰ تومان",
                daysRemainingText = "نزدیک به حد مجاز",
                isUrgent = true,
                iconRes = R.drawable.img_3d_oil,
                accentColor = WarningAmberLight
            ),
            UpcomingPaymentItemData(
                id = "up_5",
                title = "قسط خرید لپ‌تاپ دیجی‌پی",
                categoryName = "اقساط متفرقه",
                dueDatePersian = "۱۰ مهر",
                amountFormatted = "۲,۸۰۰,۰۰۰ تومان",
                daysRemainingText = "۱۷ روز مانده",
                isUrgent = false,
                iconRes = R.drawable.img_3d_installment,
                accentColor = EmeraldPrimaryLight
            ),
            UpcomingPaymentItemData(
                id = "up_6",
                title = "معاینه فنی خودرو پژو ۲۰۶",
                categoryName = "بیمه و خودرو",
                dueDatePersian = "۱۵ آبان",
                amountFormatted = "۱۵۰,۰۰۰ تومان",
                daysRemainingText = "۵۲ روز مانده",
                isUrgent = false,
                iconRes = R.drawable.img_3d_car,
                accentColor = InfoIndigoLight
            )
        )
    }

    val filteredItems = remember(selectedFilter) {
        when (selectedFilter) {
            "همه" -> allItems
            "وام بانکی" -> allItems.filter { it.categoryName == "وام بانکی" }
            "وام خانگی" -> allItems.filter { it.categoryName == "وام خانگی" }
            "بیمه و خودرو" -> allItems.filter { it.categoryName == "بیمه و خودرو" }
            else -> allItems
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp),
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
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("upcoming_items_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "موارد مهم پیش رو",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filteredItems.size} تعهد و سررسید فعال",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(modifier = Modifier.size(40.dp))
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("همه", "وام بانکی", "وام خانگی", "بیمه و خودرو").forEach { filter ->
                val selected = selectedFilter == filter
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(filteredItems, key = { it.id }) { item ->
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMD),
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    elevation = 2.dp,
                    contentPadding = PaddingValues(14.dp),
                    testTag = "upcoming_detail_${item.id}"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Soft3DIcon(
                                imageRes = item.iconRes,
                                contentDescription = item.title,
                                size = 42.dp,
                                accentColor = item.accentColor
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.dueDatePersian,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = item.daysRemainingText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.isUrgent) ExpenseRoseLight else item.accentColor
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = item.amountFormatted,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = CircleShape,
                                color = item.accentColor.copy(alpha = if (isDark) 0.2f else 0.1f),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = item.categoryName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = item.accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
