package com.example.ui.screens.reports.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.reports.model.FilterState
import com.example.ui.screens.reports.model.ReportPeriod
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFilterSheet(
    sheetState: SheetState,
    currentFilter: FilterState,
    onDismiss: () -> Unit,
    onApplyFilter: (FilterState) -> Unit,
    onResetFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var selectedAccount by remember { mutableStateOf(currentFilter.selectedAccount) }
    var selectedCategory by remember { mutableStateOf(currentFilter.selectedCategory) }
    var selectedVehicle by remember { mutableStateOf(currentFilter.selectedVehicle) }
    var selectedTxType by remember { mutableStateOf(currentFilter.transactionType) }

    val accounts = listOf("همه حساب‌ها", "کارت ملی", "حساب پاسارگاد", "کارت سامان", "کیف پول نقد")
    val categories = listOf("همه دسته‌ها", "مسکن و اجاره", "خودرو و ترابری", "خوراک", "خرید و پوشاک", "قبوض", "اقساط")
    val vehicles = listOf("همه خودروها", "پژو ۲۰۶ تیپ ۵", "دنا پلاس توربو")
    val txTypes = listOf("همه تراکنش‌ها", "فقط هزینه‌ها", "فقط درآمدها")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "فیلتر پیشرفته گزارشات",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Transaction Type
            FilterSectionTitle(title = "نوع تراکنش:")
            FilterChipsRow(
                options = txTypes,
                selectedOption = selectedTxType,
                onSelect = { selectedTxType = it }
            )

            // 2. Account Filter
            FilterSectionTitle(title = "حساب یا کارت بانکی:")
            FilterChipsRow(
                options = accounts,
                selectedOption = selectedAccount,
                onSelect = { selectedAccount = it }
            )

            // 3. Category Filter
            FilterSectionTitle(title = "دسته‌بندی مخارج:")
            FilterChipsRow(
                options = categories,
                selectedOption = selectedCategory,
                onSelect = { selectedCategory = it }
            )

            // 4. Vehicle Filter
            FilterSectionTitle(title = "خودروهای ثبت‌شده:")
            FilterChipsRow(
                options = vehicles,
                selectedOption = selectedVehicle,
                onSelect = { selectedVehicle = it }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (Apply & Clear)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Reset Button
                OutlinedButton(
                    onClick = {
                        selectedAccount = "همه حساب‌ها"
                        selectedCategory = "همه دسته‌ها"
                        selectedVehicle = "همه خودروها"
                        selectedTxType = "همه تراکنش‌ها"
                        onResetFilter()
                    },
                    modifier = Modifier
                        .weight(0.4f)
                        .height(48.dp)
                        .testTag("filter_reset_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRoseLight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "پاک کردن",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp)
                        )
                    }
                }

                // Apply Button
                Button(
                    onClick = {
                        onApplyFilter(
                            FilterState(
                                selectedPeriod = currentFilter.selectedPeriod,
                                selectedAccount = selectedAccount,
                                selectedCategory = selectedCategory,
                                selectedVehicle = selectedVehicle,
                                transactionType = selectedTxType
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(0.6f)
                        .height(48.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(RadiusMD),
                            ambientColor = EmeraldPrimaryLight.copy(alpha = 0.3f),
                            spotColor = EmeraldPrimaryLight.copy(alpha = 0.5f)
                        )
                        .testTag("filter_apply_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimaryLight)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "اعمال فیلترها",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun FilterChipsRow(
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            val isSelected = option == selectedOption

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusMD))
                    .clickable { onSelect(option) }
                    .testTag("filter_chip_$option"),
                shape = RoundedCornerShape(RadiusMD),
                color = if (isSelected) EmeraldPrimaryLight.copy(alpha = if (isDark) 0.35f else 0.15f)
                else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 0.8.dp,
                    color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) EmeraldPrimaryLight else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}
