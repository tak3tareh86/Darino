package com.example.vehicle.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.IranianPhoneUtils

/**
 * Quick dialog to update vehicle odometer / current mileage
 */
@Composable
fun VehicleMileageDialog(
    currentMileage: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var mileageText by remember { mutableStateOf(currentMileage.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("mileage_dialog"),
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "به‌روزرسانی کیلومتر خودرو",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "کیلومتر فعلی خودرو را وارد کنید تا محاسبات سلامت و موعد سرویس‌ها دقیق شود:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = mileageText,
                    onValueChange = { mileageText = it },
                    label = { Text("کیلومتر (کیلومترشمار)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val km = mileageText.toIntOrNull() ?: currentMileage
                    onConfirm(km)
                },
                modifier = Modifier.testTag("confirm_mileage_btn")
            ) {
                Text("ذخیره کیلومتر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
