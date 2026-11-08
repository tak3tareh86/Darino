package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RadiusMD
import com.example.util.IranianAmountUtils
import com.example.util.IranianPhoneUtils
import com.example.util.MoneyFormatter
import java.math.BigDecimal

/**
 * Universal Persian & English Amount Input Field with automatic 3-digit comma separation.
 * Supports:
 * - Direct typing in Persian digits (۰۱۲۳۴۵۶۷۸۹) and English digits (0123456789)
 * - Automatic 3-digit comma formatting (e.g. ۱۲,۳۴۵,۶۷۸ or 12,345,678)
 * - Live Persian words preview (به حروف: دوازده میلیون و سیصد و چهل و پنج هزار و ششصد و هفتاد و هشت تومان / ریال)
 * - Custom suffix unit ("تومان", "ریال", etc.)
 */
@Composable
fun PersianAmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRawAmountChange: ((rawDigits: String, amountLong: Long) -> Unit)? = null,
    allowDecimalInput: Boolean = false,
    @Suppress("UNUSED_PARAMETER") lenientGroupingWhileEditing: Boolean = false,
    onRawDecimalAmountChange: ((rawAmount: String, amount: BigDecimal?) -> Unit)? = null,
    label: String? = null,
    placeholder: String = "مثال: ۴۵۰,۰۰۰",
    unitLabel: String = MoneyFormatter.getUnitLabel(),
    showWordsPreview: Boolean = true,
    useOuterHeader: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(RadiusMD),
    testTag: String = "amount_input_field",
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
    fieldHeight: androidx.compose.ui.unit.Dp? = null,
    singleLineLabel: Boolean = false
) {
    val effectiveLabel = label ?: if (unitLabel.isNotBlank()) "مبلغ ($unitLabel)" else "مبلغ"
    val cleanDigits = remember(value, allowDecimalInput) {
        if (allowDecimalInput) IranianAmountUtils.cleanDecimalAmountInput(value)
        else IranianAmountUtils.cleanAmountDigits(value)
    }
    val displayFormatted = remember(value, allowDecimalInput) {
        if (allowDecimalInput) IranianAmountUtils.formatDecimalWithCommas(value)
        else IranianAmountUtils.formatWithCommas(value, inPersian = true)
    }
    val parsedAmount = remember(cleanDigits, allowDecimalInput) {
        if (allowDecimalInput) {
            IranianAmountUtils.parseAmountToBigDecimal(cleanDigits)
                ?.let { runCatching { it.longValueExact() }.getOrNull() } ?: 0L
        } else cleanDigits.toLongOrNull() ?: 0L
    }

    // Keep the editing selection while the displayed value is reformatted with grouping.
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(displayFormatted, TextRange(displayFormatted.length)))
    }

    LaunchedEffect(value, allowDecimalInput) {
        if (textFieldValue.text != displayFormatted) {
            textFieldValue = TextFieldValue(
                text = displayFormatted,
                selection = TextRange(displayFormatted.length)
            )
        }
    }

    fun logicalOffset(text: String, offset: Int): Int =
        text.take(offset.coerceIn(0, text.length)).count {
            it.isDigit() || (allowDecimalInput && (it == '.' || it == '٫'))
        }

    fun formattedOffset(text: String, logicalPosition: Int): Int {
        if (logicalPosition <= 0) return 0
        var seen = 0
        text.forEachIndexed { index, char ->
            if (char.isDigit() || (allowDecimalInput && (char == '.' || char == '٫'))) {
                seen++
                if (seen >= logicalPosition) return index + 1
            }
        }
        return text.length
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (useOuterHeader && effectiveLabel.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = effectiveLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (unitLabel.isNotBlank()) {
                    Text(
                        text = unitLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { input ->
                // Grouping separators are presentation-only during editing. A temporarily
                // incomplete group (for example "1,23" after backspace) must retain its digits.
                val newClean = if (allowDecimalInput) {
                    IranianAmountUtils.cleanDecimalAmountInputWhileEditing(input.text)
                } else {
                    IranianAmountUtils.cleanAmountDigits(input.text)
                }
                val newFormatted = if (allowDecimalInput) {
                    IranianAmountUtils.formatDecimalWithCommas(newClean)
                } else {
                    IranianAmountUtils.formatWithCommas(newClean, inPersian = true)
                }

                val start = formattedOffset(newFormatted, logicalOffset(input.text, input.selection.start))
                val end = formattedOffset(newFormatted, logicalOffset(input.text, input.selection.end))
                textFieldValue = TextFieldValue(
                    text = newFormatted,
                    selection = TextRange(start, end)
                )
                onValueChange(newFormatted)
                if (allowDecimalInput) {
                    onRawDecimalAmountChange?.invoke(
                        newClean,
                        IranianAmountUtils.parseAmountToBigDecimal(newClean)
                    )
                } else {
                    onRawAmountChange?.invoke(newClean, newClean.toLongOrNull() ?: 0L)
                }
            },
            label = if (!useOuterHeader && effectiveLabel.isNotBlank()) {
                { if (singleLineLabel) Text(text = effectiveLabel, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)) else Text(text = effectiveLabel) }
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
            leadingIcon = leadingIcon,
            trailingIcon = if (unitLabel.isNotBlank()) {
                {
                    Text(
                        text = unitLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = if (allowDecimalInput) KeyboardType.Decimal else KeyboardType.Number),
            singleLine = singleLine,
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
        } else if (showWordsPreview && !allowDecimalInput && parsedAmount > 0L) {
            val words = remember(parsedAmount, unitLabel) {
                IranianAmountUtils.amountToPersianWords(parsedAmount, unitLabel)
            }
            Text(
                text = "به حروف: $words",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 1.dp)
            )
        }
    }
}
