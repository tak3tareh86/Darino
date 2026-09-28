package com.example.ui.screens.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD
import com.example.util.IranianPhoneUtils

/**
 * Premium 6-Cell OTP Input Component for Persian RTL experience.
 * Features soft 3D cell depth, active glow, paste handling, and single-input keyboard routing.
 */
@Composable
fun OtpInputField(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    cellCount: Int = 6,
    isEnabled: Boolean = true,
    isError: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onComplete: (() -> Unit)? = null
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val focusManager = LocalFocusManager.current

    // Normalize input to 6 characters
    val cleanOtp = IranianPhoneUtils.convertDigitsToEnglish(otpValue.take(cellCount))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("otp_container"),
        contentAlignment = Alignment.Center
    ) {
        // Hidden BasicTextField capturing keyboard events
        BasicTextField(
            value = cleanOtp,
            onValueChange = { input ->
                val digitsOnly = IranianPhoneUtils.convertDigitsToEnglish(
                    input.filter { it.isDigit() || it in "۰۱۲۳۴۵۶۷۸۹" }
                ).take(cellCount)
                onOtpChange(digitsOnly)
                if (digitsOnly.length == cellCount) {
                    onComplete?.invoke()
                }
            },
            enabled = isEnabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = if (cleanOtp.length == cellCount) ImeAction.Done else ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onComplete?.invoke()
                }
            ),
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester)
                .testTag("otp_hidden_input")
        )

        // Visual 6-Cell Row (Enforce LTR sequence 1 -> 2 -> 3 -> 4 -> 5 -> 6 even in RTL app)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isEnabled) {
                            focusRequester.requestFocus()
                        }
                    },
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (index in 0 until cellCount) {
                    val char = cleanOtp.getOrNull(index)?.toString() ?: ""
                    val isFocused = isEnabled && (index == cleanOtp.length || (index == cellCount - 1 && cleanOtp.length == cellCount))
                    val isFilled = char.isNotEmpty()

                    OtpCell(
                        char = char,
                        isFocused = isFocused,
                        isFilled = isFilled,
                        isEnabled = isEnabled,
                        isError = isError,
                        isDark = isDark,
                        testTag = "otp_cell_$index"
                    )
                }
            }
        }
    }
}

@Composable
private fun OtpCell(
    char: String,
    isFocused: Boolean,
    isFilled: Boolean,
    isEnabled: Boolean,
    isError: Boolean,
    isDark: Boolean,
    testTag: String
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> ExpenseRoseLight
            isFocused -> MaterialTheme.colorScheme.primary
            isFilled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        },
        label = "OtpBorderColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "OtpCellScale"
    )

    val cellBackground = when {
        !isEnabled -> if (isDark) Color(0xFF131B2E).copy(alpha = 0.4f) else Color(0xFFF1F5F9).copy(alpha = 0.5f)
        isFocused -> if (isDark) Color(0xFF1A263D) else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        isFilled -> if (isDark) Color(0xFF152033) else MaterialTheme.colorScheme.surface
        else -> if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    }

    val persianChar = remember(char) {
        if (char.isNotEmpty()) IranianPhoneUtils.convertDigitsToPersian(char) else ""
    }

    Surface(
        modifier = Modifier
            .size(width = 38.dp, height = 44.dp)
            .scale(scale)
            .shadow(
                elevation = if (isFocused) 4.dp else if (isFilled) 2.dp else 1.dp,
                shape = RoundedCornerShape(RadiusMD),
                ambientColor = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
            )
            .clip(RoundedCornerShape(RadiusMD))
            .border(
                BorderStroke(
                    width = if (isFocused) 1.5.dp else 1.dp,
                    color = borderColor
                ),
                shape = RoundedCornerShape(RadiusMD)
            )
            .testTag(testTag),
        color = cellBackground,
        shape = RoundedCornerShape(RadiusMD)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (persianChar.isNotEmpty()) {
                Text(
                    text = persianChar,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = if (isError) ExpenseRoseLight else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            } else if (isFocused && isEnabled) {
                // Active cursor placeholder indicator
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
            } else {
                // Inactive dot placeholder
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isEnabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                            }
                        )
                )
            }
        }
    }
}
