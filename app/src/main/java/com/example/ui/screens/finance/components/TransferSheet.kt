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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onDismiss: () -> Unit,
    onSubmitTransfer: (from: String, to: String, amount: String, desc: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var fromAccount by remember { mutableStateOf("حساب اصلی") }
    var toAccount by remember { mutableStateOf("حساب پس‌انداز") }
    var amountText by remember { mutableStateOf("") }
    var descText by remember { mutableStateOf("") }

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

                // Submit Button
                Button(
                    onClick = {
                        if (amountText.isNotBlank()) {
                            onSubmitTransfer(fromAccount, toAccount, amountText, descText)
                        }
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
