package com.example.ui.screens.finance.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusMD

@Composable
fun TransferSheet(
    accounts: List<com.example.ui.screens.finance.model.Account> = emptyList(),
    onDismiss: () -> Unit,
    onSubmitTransfer: (from: String, to: String, amount: String, desc: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAccounts = remember(accounts) { accounts.filter { it.isActive } }
    var selectedSource by remember(activeAccounts) {
        mutableStateOf<com.example.ui.screens.finance.model.Account?>(
            activeAccounts.find { it.name == "کارت بانکی" || it.name.startsWith("کارت بانکی") } ?: activeAccounts.firstOrNull()
        )
    }
    var selectedDestination by remember(activeAccounts) {
        mutableStateOf<com.example.ui.screens.finance.model.Account?>(
            activeAccounts.find { it.name == "کیف پول" || it.name.startsWith("کیف پول") } ?: activeAccounts.getOrNull(1) ?: activeAccounts.firstOrNull()
        )
    }

    LaunchedEffect(activeAccounts) {
        if (selectedSource == null && activeAccounts.isNotEmpty()) {
            selectedSource = activeAccounts.find { it.name == "کارت بانکی" || it.name.startsWith("کارت بانکی") } ?: activeAccounts.firstOrNull()
        }
        if (selectedDestination == null && activeAccounts.size >= 2) {
            selectedDestination = activeAccounts.find { it.name == "کیف پول" || it.name.startsWith("کیف پول") } ?: activeAccounts.getOrNull(1) ?: activeAccounts.firstOrNull()
        }
    }
    var amountText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 24.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = InfoIndigoLight.copy(alpha = 0.25f),
                    spotColor = InfoIndigoLight.copy(alpha = 0.4f)
                ),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, InfoIndigoLight.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(InfoIndigoLight.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SwapHoriz,
                                contentDescription = null,
                                tint = InfoIndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "انتقال وجه",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_transfer_dialog")) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Account Selectors
                val isDark = MaterialTheme.colorScheme.background.red < 0.2f
                if (activeAccounts.size < 2) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(RadiusMD),
                        color = Color(0xFFEF4444).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "برای انتقال وجه بین حساب‌ها حداقل به دو حساب فعال نیاز دارید.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFEF4444)),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("حساب مبدأ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeAccounts.forEach { acc ->
                                val isSel = selectedSource?.id == acc.id
                                Surface(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedSource = acc },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                ) {
                                    Text(acc.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Text("حساب مقصد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeAccounts.forEach { acc ->
                                val isSel = selectedDestination?.id == acc.id
                                Surface(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { selectedDestination = acc },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                ) {
                                    Text(acc.name, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("مبلغ انتقال (تومان)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_amount_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InfoIndigoLight,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                // Description Field
                OutlinedTextField(
                    value = descText,
                    onValueChange = { descText = it },
                    label = { Text("توضیحات (اختیاری)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_desc_input"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InfoIndigoLight,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                errorMessage?.let { msg ->
                    Text(text = msg, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFEF4444)))
                }

                // Submit Button
                Button(
                    onClick = {
                        val parsed = com.example.util.IranianAmountUtils.parseAmountToLong(amountText) ?: amountText.toLongOrNull() ?: 0L
                        if (parsed <= 0L) {
                            errorMessage = "لطفاً مبلغ معتبری وارد کنید."
                            return@Button
                        }
                        val src = selectedSource
                        val dest = selectedDestination
                        if (src == null || dest == null) {
                            errorMessage = "لطفاً حساب مبدأ و مقصد را انتخاب کنید."
                            return@Button
                        }
                        if (src.id == dest.id) {
                            errorMessage = "حساب مبدأ و مقصد نمی‌توانند یکسان باشند."
                            return@Button
                        }
                        errorMessage = null
                        onSubmitTransfer(src.id, dest.id, amountText, descText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_transfer_btn"),
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = InfoIndigoLight),
                    enabled = amountText.isNotBlank()
                ) {
                    Text(
                        text = "ثبت انتقال",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}
