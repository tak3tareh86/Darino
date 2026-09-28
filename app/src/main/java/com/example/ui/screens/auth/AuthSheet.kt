package com.example.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusXL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthBottomSheet(
    authViewModel: AuthViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onDismissRequest()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            shape = RoundedCornerShape(topStart = RadiusXL, topEnd = RadiusXL),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            },
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Soft3DIcon(
                    imageRes = R.drawable.img_3d_shield_security,
                    contentDescription = "احراز هویت",
                    accentColor = EmeraldPrimaryLight,
                    size = 46.dp
                )

                Text(
                    text = "ورود به حساب کاربری",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "جهت همگام‌سازی حساب و استفاده از تمامی امکانات دارینو وارد شوید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Username
                OutlinedTextField(
                    value = uiState.loginUsername,
                    onValueChange = authViewModel::onLoginUsernameChanged,
                    label = { Text("نام کاربری") },
                    placeholder = { Text("username") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sheet_username_input")
                )

                // Password
                OutlinedTextField(
                    value = uiState.loginPassword,
                    onValueChange = authViewModel::onLoginPasswordChanged,
                    label = { Text("رمز عبور") },
                    placeholder = { Text("••••••••") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (passwordVisible) "مخفی کردن رمز عبور" else "نمایش رمز عبور",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sheet_password_input")
                )

                if (uiState.loginError != null) {
                    Text(
                        text = uiState.loginError ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRoseLight
                    )
                }

                Button(
                    onClick = authViewModel::login,
                    enabled = uiState.canSubmitLogin,
                    shape = RoundedCornerShape(RadiusMD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (uiState.isLoginLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("ورود به حساب")
                    }
                }
            }
        }
    }
}
