package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadiusMD
import com.example.util.IranianDateUtils
import com.example.util.IranianPhoneUtils

/**
 * Universal Persian & English Date Input Field with:
 * - Automatic slash insertion as user types (e.g. 14030715 -> ۱۴۰۳/۰۷/۱۵)
 * - Support for both Persian and English number inputs
 * - Built-in calendar picker button to choose date from Persian Solar Calendar dialog
 */
@Composable
fun PersianDateInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = "تاریخ",
    placeholder: String = "۱۴۰۴/۰۷/۱۵",
    helperText: String? = null,
    useOuterHeader: Boolean = false,
    showSubLabel: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(RadiusMD),
    dialogTitle: String = "انتخاب تاریخ (شمسی)",
    testTag: String = "date_input_field",
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    var showCalendarDialog by remember { mutableStateOf(false) }

    val formattedDisplay = remember(value) {
        IranianDateUtils.formatWithSlashes(value, inPersian = true)
    }

    val (currentY, currentM, currentD) = remember(value) {
        IranianDateUtils.parsePersianDate(value)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (useOuterHeader && !label.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showSubLabel) {
                    Text(
                        text = "شمسی (روز/ماه/سال)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        OutlinedTextField(
            value = formattedDisplay,
            onValueChange = { input ->
                val formatted = IranianDateUtils.formatWithSlashes(input, inPersian = true)
                onValueChange(formatted)
            },
            label = if (!useOuterHeader && !label.isNullOrBlank()) {
                { Text(text = label) }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { showCalendarDialog = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = "انتخاب تاریخ از تقویم",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = enabled,
            isError = isError,
            shape = shape,
            colors = colors
        )

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp)
            )
        } else if (!helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }

    if (showCalendarDialog) {
        SharedPersianDatePickerDialog(
            initialYear = currentY,
            initialMonth = currentM,
            initialDay = currentD,
            title = dialogTitle,
            onDismiss = { showCalendarDialog = false },
            onConfirm = { y, m, d ->
                val newDate = IranianDateUtils.createFormattedDate(y, m, d, inPersian = true)
                onValueChange(newDate)
            }
        )
    }
}
