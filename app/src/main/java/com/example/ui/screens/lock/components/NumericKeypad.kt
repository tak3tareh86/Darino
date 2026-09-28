package com.example.ui.screens.lock.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight

/**
 * Premium PIN Indicators (Dots) showing filled state, errors, and animations.
 */
@Composable
fun PinDotsDisplay(
    pinLength: Int,
    maxDigits: Int,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxDigits) {
            val isFilled = i < pinLength
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> ExpenseRoseLight
                    isFilled -> EmeraldPrimaryLight
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                label = "PinDotColor"
            )
            val scale by animateFloatAsState(
                targetValue = if (isFilled) 1.25f else 1.0f,
                animationSpec = spring(),
                label = "PinDotScale"
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(dotColor)
                    .border(
                        width = 1.dp,
                        color = if (isFilled || isError) dotColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Clean Soft 3D Numeric Keypad with optional biometric trigger button.
 */
@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBiometricButton: Boolean = false,
    onBiometricClick: () -> Unit = {}
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showBiometricButton) "bio" else "", "0", "delete")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> {
                            Box(modifier = Modifier.size(68.dp))
                        }
                        "bio" -> {
                            KeypadButton(
                                onClick = onBiometricClick,
                                modifier = Modifier.testTag("keypad_bio_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Fingerprint,
                                    contentDescription = "اثر انگشت",
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        "delete" -> {
                            KeypadButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.testTag("keypad_delete_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Backspace,
                                    contentDescription = "حذف رقم",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            KeypadButton(
                                onClick = { onDigitClick(key) },
                                modifier = Modifier.testTag("keypad_digit_$key")
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(),
        label = "KeypadButtonScale"
    )

    Box(
        modifier = modifier
            .size(68.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isPressed) 0.8f else 0.45f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
