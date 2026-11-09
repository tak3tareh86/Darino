package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    fieldHeight: androidx.compose.ui.unit.Dp? = null,
    singleLineLabel: Boolean = false,
    fixedLabel: Boolean = false,
    contentStartPadding: androidx.compose.ui.unit.Dp = 10.dp
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

        if (fixedLabel) {
            var labelWidthPx by remember(label) { mutableStateOf(0f) }
            val layoutDirection = LocalLayoutDirection.current
            val borderColor = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.outline
            }
            val labelColor = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight ?: 56.dp)
                    .drawBehind {
                        val strokeWidth = 1.dp.toPx()
                        val cornerRadius = RadiusMD.toPx().coerceAtMost(size.height / 2f)
                        val inset = strokeWidth / 2f
                        val left = inset
                        val right = size.width - inset
                        val top = inset
                        val bottom = size.height - inset
                        val labelGap = if (label.isNullOrBlank()) 0f else labelWidthPx + 8.dp.toPx()
                        val gapStart: Float
                        val gapEnd: Float

                        if (layoutDirection == LayoutDirection.Rtl) {
                            gapEnd = right - cornerRadius
                            gapStart = (gapEnd - labelGap).coerceAtLeast(left + cornerRadius)
                        } else {
                            gapStart = left + cornerRadius
                            gapEnd = (gapStart + labelGap).coerceAtMost(right - cornerRadius)
                        }

                        val outline = Path().apply {
                            moveTo(left + cornerRadius, top)
                            if (gapStart > left + cornerRadius) {
                                lineTo(gapStart, top)
                            }
                            if (gapEnd < right - cornerRadius) {
                                moveTo(gapEnd, top)
                                lineTo(right - cornerRadius, top)
                            } else {
                                moveTo(right - cornerRadius, top)
                            }
                            quadraticTo(right, top, right, top + cornerRadius)
                            lineTo(right, bottom - cornerRadius)
                            quadraticTo(right, bottom, right - cornerRadius, bottom)
                            lineTo(left + cornerRadius, bottom)
                            quadraticTo(left, bottom, left, bottom - cornerRadius)
                            lineTo(left, top + cornerRadius)
                            quadraticTo(left, top, left + cornerRadius, top)
                        }
                        drawPath(                            path = outline,
                            color = borderColor,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    .testTag(testTag)
            ) {
                if (!label.isNullOrBlank()) {
                    Text(
                        text = label,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = labelColor,
                        onTextLayout = { labelWidthPx = it.size.width.toFloat() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = if (layoutDirection == LayoutDirection.Rtl) (-RadiusMD - 8.dp) else RadiusMD,
                                y = (-8).dp
                            )
                            .padding(horizontal = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = contentStartPadding, end = 6.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = formattedDisplay,
                        onValueChange = { input ->
                            val formatted = IranianDateUtils.formatWithSlashes(input, inPersian = true)
                            onValueChange(formatted)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = enabled,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (formattedDisplay.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    IconButton(
                        onClick = { showCalendarDialog = true },
                        enabled = enabled,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "انتخاب تاریخ از تقویم",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = formattedDisplay,
                onValueChange = { input ->
                    val formatted = IranianDateUtils.formatWithSlashes(input, inPersian = true)
                    onValueChange(formatted)
                },
                label = if (!useOuterHeader && !label.isNullOrBlank()) {
                    {
                        if (singleLineLabel) {
                            Text(
                                text = label,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                            )
                        } else {
                            Text(text = label)
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fieldHeight != null) Modifier.height(fieldHeight) else Modifier)
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
        }

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