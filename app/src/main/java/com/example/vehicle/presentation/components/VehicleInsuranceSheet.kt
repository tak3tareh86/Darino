package com.example.vehicle.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shield
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
import com.example.ui.components.Soft3DIcon
import com.example.vehicle.data.VehicleEntity

/**
 * Bottom sheet to manage vehicle insurance policy and technical inspection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleInsuranceSheet(
    vehicle: VehicleEntity,
    onSaveInsurance: (
        company: String,
        type: String,
        startDate: String,
        endDate: String,
        amount: Long,
        policyNumber: String
    ) -> Unit,
    onSaveInspection: (
        lastInspectionDate: String,
        expiryDate: String,
        cost: Long,
        status: String,
        centerName: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) } // 0: Insurance, 1: Inspection

    // Insurance fields
    var insuranceCompany by remember { mutableStateOf("بیمه ایران") }
    var insuranceType by remember { mutableStateOf("شخص ثالث") }
    var startDate by remember { mutableStateOf("1405/06/01") }
    var endDate by remember { mutableStateOf("1406/06/01") }
    var insuranceAmountText by remember { mutableStateOf("3000000") }
    var policyNumber by remember { mutableStateOf("IR-9820-4491-01") }

    // Inspection fields
    var lastInspectionDate by remember { mutableStateOf("1404/08/10") }
    var inspectionExpiryDate by remember { mutableStateOf("1406/08/10") }
    var inspectionCostText by remember { mutableStateOf("92000") }
    var inspectionStatus by remember { mutableStateOf("معتبر (گواهی معاینه فنی)") }
    var centerName by remember { mutableStateOf("مرکز مکانیزه نیایش") }

    val companies = listOf("بیمه ایران", "بیمه آسیا", "بیمه دانا", "بیمه البرز", "بیمه پاسارگاد", "بیمه سامان")
    val insuranceTypes = listOf("شخص ثالث", "بیمه بدنه")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("vehicle_insurance_sheet")
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
                        imageRes = R.drawable.img_3d_insurance,
                        contentDescription = "مدیریت بیمه و مدارک خودرو",
                        size = 36.dp,
                        accentColor = Color(0xFFF59E0B)
                    )
                    Column {
                        Text(
                            text = "مدیریت بیمه و مدارک خودرو",
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

            // Tab Switcher between Insurance & Inspection
            PrimaryTabRow(
                selectedTabIndex = selectedSection,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    text = { Text("بیمه‌نامه خودرو") }
                )
                Tab(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    text = { Text("معاینه فنی") }
                )
            }

            if (selectedSection == 0) {
                // Insurance Form
                Text(
                    text = "نوع بیمه",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    insuranceTypes.forEach { type ->
                        FilterChip(
                            selected = insuranceType == type,
                            onClick = { insuranceType = type },
                            label = { Text(type) }
                        )
                    }
                }

                Text(
                    text = "شرکت بیمه‌گر",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    companies.take(3).forEach { comp ->
                        FilterChip(
                            selected = insuranceCompany == comp,
                            onClick = { insuranceCompany = comp },
                            label = { Text(comp, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("تاریخ شروع") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("تاریخ انقضا") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = insuranceAmountText,
                        onValueChange = { insuranceAmountText = it },
                        label = { Text("حق بیمه پرداختی (تومان)") },
                        modifier = Modifier.weight(1.2f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = policyNumber,
                        onValueChange = { policyNumber = it },
                        label = { Text("شماره بیمه‌نامه") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "🔔 یادآور خودکار ۳۰ روز، ۱۵ روز و ۷ روز قبل از پایان بیمه برای شما فعال خواهد بود.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Button(
                    onClick = {
                        val amount = insuranceAmountText.toLongOrNull() ?: 0L
                        onSaveInsurance(insuranceCompany, insuranceType, startDate, endDate, amount, policyNumber)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_insurance_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت و فعال‌سازی بیمه‌نامه", fontWeight = FontWeight.Bold)
                }
            } else {
                // Technical Inspection Form
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = lastInspectionDate,
                        onValueChange = { lastInspectionDate = it },
                        label = { Text("تاریخ آخرین معاینه") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = inspectionExpiryDate,
                        onValueChange = { inspectionExpiryDate = it },
                        label = { Text("تاریخ انقضا") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = inspectionCostText,
                        onValueChange = { inspectionCostText = it },
                        label = { Text("هزینه (تومان)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = centerName,
                        onValueChange = { centerName = it },
                        label = { Text("مرکز معاینه فنی") },
                        modifier = Modifier.weight(1.3f),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = inspectionStatus,
                    onValueChange = { inspectionStatus = it },
                    label = { Text("وضعیت گواهی معاینه فنی") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val cost = inspectionCostText.toLongOrNull() ?: 0L
                        onSaveInspection(lastInspectionDate, inspectionExpiryDate, cost, inspectionStatus, centerName)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_inspection_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ذخیره اطلاعات معاینه فنی", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
