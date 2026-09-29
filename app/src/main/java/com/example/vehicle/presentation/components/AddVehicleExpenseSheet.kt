package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.PersianAmountInputField
import com.example.ui.components.PersianDateInputField
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianAmountUtils
import com.example.vehicle.data.VehicleEntity
import com.example.vehicle.data.VehicleExpenseCategory

/**
 * Bottom sheet to log a vehicle expense (سوخت، تعمیرات، سرویس، بیمه، قطعات، شستشو، پارکینگ، سایر)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleExpenseSheet(
    vehicle: VehicleEntity,
    onAddExpense: (
        title: String,
        category: VehicleExpenseCategory,
        amount: Long,
        date: String,
        description: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(VehicleExpenseCategory.FUEL) }
    var title by remember { mutableStateOf("سوخت‌گیری بنزین") }
    var amountText by remember { mutableStateOf("500000") }
    var date by remember { mutableStateOf("1405/06/20") }
    var description by remember { mutableStateOf("") }
    var hasReceiptMock by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("add_vehicle_expense_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = selectedCategory.iconRes,
                        contentDescription = "ثبت هزینه خودرو",
                        size = 36.dp,
                        accentColor = Color(selectedCategory.colorHex)
                    )
                    Column {
                        Text(
                            text = "ثبت هزینه خودرو",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${vehicle.brand} ${vehicle.model}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "بستن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            // Category Selector Chips (8 Categories from Requirements)
            Text(
                text = "دسته‌بندی هزینه",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(VehicleExpenseCategory.values()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = cat
                            if (title.isBlank() || title == "سوخت‌گیری بنزین" || title.startsWith("هزینه")) {
                                title = when (cat) {
                                    VehicleExpenseCategory.FUEL -> "سوخت‌گیری بنزین"
                                    VehicleExpenseCategory.WASH -> "کارواش و نظافت"
                                    VehicleExpenseCategory.PARKING -> "پارکینگ / عوارض آزادراهی"
                                    VehicleExpenseCategory.REPAIRS -> "تعمیرات فنی"
                                    VehicleExpenseCategory.PARTS -> "خرید قطعات یدکی"
                                    VehicleExpenseCategory.INSURANCE -> "تمدید بیمه‌نامه"
                                    VehicleExpenseCategory.SERVICE -> "سرویس دوره‌ای"
                                    VehicleExpenseCategory.OTHER -> "سایر هزینه‌های متفرقه"
                                }
                            }
                        },
                        label = { Text(cat.title, fontSize = 11.sp) }
                    )
                }
            }

            // Title & Amount
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان هزینه") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PersianAmountInputField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "مبلغ هزینه",
                    unitLabel = "تومان",
                    placeholder = "مثال: ۵۰۰,۰۰۰",
                    showWordsPreview = false,
                    modifier = Modifier.weight(1.2f),
                    testTag = "expense_amount_input"
                )

                PersianDateInputField(
                    value = date,
                    onValueChange = { date = it },
                    label = "تاریخ (شمسی)",
                    placeholder = "۱۴۰۵/۰۶/۲۰",
                    dialogTitle = "انتخاب تاریخ هزینه خودرو",
                    modifier = Modifier.weight(1f),
                    testTag = "expense_date_input"
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات تکمیلی") },
                placeholder = { Text("مثلاً: پمپ بنزین ولنجک ۴۰ لیتر") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2
            )

            // Invoice / Receipt Image Attachment Mock
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (hasReceiptMock) "تصویر فاکتور ضمیمه شد" else "پیوست تصویر فاکتور (اختیاری)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    TextButton(onClick = { hasReceiptMock = !hasReceiptMock }) {
                        Text(if (hasReceiptMock) "حذف" else "انتخاب فایل")
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val amount = IranianAmountUtils.parseAmountToLong(amountText)
                        onAddExpense(title, selectedCategory, amount, date, description)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_add_expense_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ثبت در هزینه‌های خودرو",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
