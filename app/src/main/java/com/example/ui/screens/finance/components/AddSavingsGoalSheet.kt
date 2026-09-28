package com.example.ui.screens.finance.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.finance.model.SavingsGoal
import com.example.ui.screens.finance.model.SavingsGoalStatus
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.util.MoneyFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingsGoalSheet(
    initialGoal: SavingsGoal? = null,
    onDismiss: () -> Unit,
    onSubmit: (SavingsGoal) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var title by remember { mutableStateOf(initialGoal?.title ?: "") }
    var targetAmountRaw by remember { mutableStateOf(initialGoal?.targetAmount?.toString() ?: "") }
    var currentAmountRaw by remember { mutableStateOf(initialGoal?.currentAmount?.toString() ?: "0") }
    var targetDate by remember { mutableStateOf(initialGoal?.targetDate ?: "۱۴۰۵/۱۲/۲۹") }
    var description by remember { mutableStateOf(initialGoal?.description ?: "") }
    var selectedEmoji by remember { mutableStateOf(initialGoal?.iconEmoji ?: "🎯") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emojis = listOf("🎯", "💻", "🛡️", "🚗", "🏠", "✈️", "💍", "📚", "🎁")

    val parsedTarget = targetAmountRaw.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val parsedCurrent = currentAmountRaw.filter { it.isDigit() }.toLongOrNull() ?: 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("add_savings_goal_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialGoal != null) "ویرایش هدف پس‌انداز" else "تعریف هدف پس‌انداز جدید",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

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

            // Emoji Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emojis.forEach { emoji ->
                    val isSelected = selectedEmoji == emoji
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.2f) else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF10B981) else Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedEmoji = emoji }
                    ) {
                        Text(
                            text = emoji,
                            modifier = Modifier.padding(10.dp),
                            fontSize = 20.sp
                        )
                    }
                }
            }

            // Goal Title
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "عنوان هدف",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("مثال: خرید لپ‌تاپ، سفر شمال", style = MaterialTheme.typography.bodyMedium) },
                    shape = RoundedCornerShape(RadiusMD)
                )
            }

            // Target Amount
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "مبلغ کل هدف (تومان)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = targetAmountRaw,
                    onValueChange = { targetAmountRaw = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("مثال: ۶۰,۰۰۰,۰۰۰", style = MaterialTheme.typography.bodyMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(RadiusMD)
                )

                if (parsedTarget > 0L) {
                    Text(
                        text = "= ${MoneyFormatter.formatToman(parsedTarget)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Current / Initial Amount
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "مبلغ پس‌اندازشده فعلی (تومان)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = currentAmountRaw,
                    onValueChange = { currentAmountRaw = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(RadiusMD)
                )
            }

            // Target Date
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "تاریخ موعد هدف (شمسی)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD)
                )
            }

            // Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "توضیحات اختیاری",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD)
                )
            }

            errorMessage?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "لطفاً عنوان هدف را وارد کنید."
                        return@Button
                    }
                    if (parsedTarget <= 0L) {
                        errorMessage = "مبلغ کل هدف باید بیشتر از صفر باشد."
                        return@Button
                    }

                    val g = SavingsGoal(
                        id = initialGoal?.id ?: UUID.randomUUID().toString(),
                        title = title,
                        targetAmount = parsedTarget,
                        currentAmount = parsedCurrent,
                        targetDate = targetDate,
                        description = description,
                        iconEmoji = selectedEmoji,
                        status = if (parsedCurrent >= parsedTarget) SavingsGoalStatus.COMPLETED else SavingsGoalStatus.IN_PROGRESS
                    )
                    onSubmit(g)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_savings_goal_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = if (initialGoal != null) "ذخیره تغییرات" else "ثبت هدف پس‌انداز",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositGoalSheet(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onDeposit: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var rawAmount by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val parsedAmount = rawAmount.filter { it.isDigit() }.toLongOrNull() ?: 0L

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("deposit_goal_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "واریز به «${goal.title}»",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

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

            Text(
                text = "مبلغ باقیمانده تا تکمیل: ${goal.formattedRemaining}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "مبلغ واریز (تومان)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it.filter { ch -> ch.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("مثال: ۱,۰۰۰,۰۰۰", style = MaterialTheme.typography.bodyMedium) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(RadiusMD)
                )

                if (parsedAmount > 0L) {
                    Text(
                        text = "= ${MoneyFormatter.formatToman(parsedAmount)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFF10B981)
                    )
                }
            }

            errorMessage?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
            }

            Button(
                onClick = {
                    if (parsedAmount <= 0L) {
                        errorMessage = "مبلغ باید بزرگتر از صفر باشد."
                        return@Button
                    }
                    onDeposit(parsedAmount)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_deposit_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text(
                    text = "افزایش موجودی هدف",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
