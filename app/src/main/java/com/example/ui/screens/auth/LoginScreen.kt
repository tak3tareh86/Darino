package com.example.ui.screens.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.DarinoLogoMark
import com.example.ui.components.Layered3DCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD

/**
 * Dedicated Premium Persian RTL Login & Registration Screen.
 * Implements mixed-case sensitive Username + Password authentication and Gmail recovery flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }

    val backgroundBrush = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF090D16),
                    Color(0xFF0F172A),
                    Color(0xFF090D16)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFF1F5F9),
                    Color(0xFFE2E8F0)
                )
            )
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .testTag("login_screen_container"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Header with Darino Branding
                AuthHeroHeader()

                // 2. Auth Card with Login / Register Tabs
                Layered3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 4.dp,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tabs: Login vs Register
                        TabRow(
                            selectedTabIndex = uiState.selectedTab.ordinal,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Tab(
                                selected = uiState.selectedTab == AuthTab.LOGIN,
                                onClick = { viewModel.selectTab(AuthTab.LOGIN) },
                                text = {
                                    Text(
                                        text = "ورود",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                modifier = Modifier.testTag("login_tab")
                            )
                            Tab(
                                selected = uiState.selectedTab == AuthTab.REGISTER,
                                onClick = { viewModel.selectTab(AuthTab.REGISTER) },
                                text = {
                                    Text(
                                        text = "ثبت‌نام",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                },
                                modifier = Modifier.testTag("register_tab")
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (uiState.selectedTab == AuthTab.LOGIN) {
                            LoginTabForm(
                                uiState = uiState,
                                viewModel = viewModel,
                                isDark = isDark,
                                onLogin = {
                                    focusManager.clearFocus()
                                    viewModel.login()
                                },
                                onForgotPasswordClick = {
                                    viewModel.showForgotPasswordSheet(true)
                                }
                            )
                        } else {
                            RegisterTabForm(
                                uiState = uiState,
                                viewModel = viewModel,
                                isDark = isDark,
                                onRegister = {
                                    focusManager.clearFocus()
                                    viewModel.register()
                                }
                            )
                        }

                        // General Error Banner
                        AnimatedVisibility(
                            visible = uiState.generalErrorMessage != null,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            uiState.generalErrorMessage?.let { msg ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(RadiusMD),
                                    color = ExpenseRoseLight.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = ExpenseRoseLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = ExpenseRoseLight,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Success Banner
                        AnimatedVisibility(
                            visible = uiState.successMessage != null && !uiState.isAuthenticated,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            uiState.successMessage?.let { msg ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(RadiusMD),
                                    color = EmeraldPrimaryLight.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, EmeraldPrimaryLight.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldPrimaryLight,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = msg,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = EmeraldPrimaryLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Security Note Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMD),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "رمز عبور شما با استفاده از الگوریتم‌های پیشرفته رمزنگاری ذخیره می‌شود.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Real Working Forgot Password Bottom Sheet
            if (uiState.showForgotPasswordSheet) {
                ForgotPasswordBottomSheet(
                    uiState = uiState,
                    viewModel = viewModel,
                    isDark = isDark,
                    onDismiss = { viewModel.showForgotPasswordSheet(false) }
                )
            }
        }
    }
}

@Composable
private fun AuthHeroHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DarinoLogoMark(
            size = 64.dp,
            showShadow = true
        )

        Text(
            text = "ورود به دارینو",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "مدیریت هوشمند دارایی‌ها و امور مالی شخصی",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoginTabForm(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    isDark: Boolean,
    onLogin: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Username Input (Supports both uppercase and lowercase)
        Text(
            text = "نام کاربری",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = uiState.loginUsername,
            onValueChange = viewModel::onLoginUsernameChanged,
            placeholder = { Text("حروف بزرگ و کوچک، عدد یا _") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            isError = uiState.loginError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            shape = RoundedCornerShape(RadiusMD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                errorBorderColor = ExpenseRoseLight,
                focusedContainerColor = if (isDark) Color(0xFF131B2E) else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_username_input")
        )

        // Password Input
        Text(
            text = "رمز عبور",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = uiState.loginPassword,
            onValueChange = viewModel::onLoginPasswordChanged,
            placeholder = { Text("••••••••") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
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
            isError = uiState.loginError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onLogin() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(RadiusMD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                errorBorderColor = ExpenseRoseLight,
                focusedContainerColor = if (isDark) Color(0xFF131B2E) else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_password_input")
        )

        if (uiState.loginError != null) {
            Text(
                text = uiState.loginError,
                style = MaterialTheme.typography.labelSmall,
                color = ExpenseRoseLight,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Forgot Password Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onForgotPasswordClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "رمز عبور را فراموش کرده‌اید؟",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Login CTA Button
        Button(
            onClick = onLogin,
            enabled = uiState.canSubmitLogin,
            shape = RoundedCornerShape(RadiusMD),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .shadow(
                    elevation = if (uiState.canSubmitLogin) 4.dp else 0.dp,
                    shape = RoundedCornerShape(RadiusMD)
                )
                .testTag("login_submit_button")
        ) {
            if (uiState.isLoginLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Text(
                        text = "در حال بررسی...",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = "ورود به برنامه",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RegisterTabForm(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    isDark: Boolean,
    onRegister: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Mandatory Gmail / Email Input placed ABOVE Username
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "جیمیل / ایمیل (الزامی)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "جیمیل یا ایمیل برای بازیابی رمز عبور فراموش شده خود استفاده می‌شود.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedTextField(
            value = uiState.registerEmail,
            onValueChange = viewModel::onRegisterEmailChanged,
            placeholder = { Text("مثال: yourname@gmail.com") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            isError = uiState.registerError != null && (uiState.registerEmail.isBlank() || !uiState.registerEmail.contains("@")),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            shape = RoundedCornerShape(RadiusMD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                errorBorderColor = ExpenseRoseLight,
                focusedContainerColor = if (isDark) Color(0xFF131B2E) else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_email_input")
        )

        // 2. Username Input (Supports both uppercase and lowercase)
        Text(
            text = "نام کاربری جدید",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = uiState.registerUsername,
            onValueChange = viewModel::onRegisterUsernameChanged,
            placeholder = { Text("حروف بزرگ و کوچک، عدد یا _") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            isError = uiState.registerError != null && uiState.registerUsername.length < 3,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            shape = RoundedCornerShape(RadiusMD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                errorBorderColor = ExpenseRoseLight,
                focusedContainerColor = if (isDark) Color(0xFF131B2E) else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_username_input")
        )

        // 3. Password Input
        Text(
            text = "رمز عبور",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = uiState.registerPassword,
            onValueChange = viewModel::onRegisterPasswordChanged,
            placeholder = { Text("حداقل ۸ کاراکتر") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
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
            isError = uiState.registerError != null && uiState.registerPassword.length < 8,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            shape = RoundedCornerShape(RadiusMD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                errorBorderColor = ExpenseRoseLight,
                focusedContainerColor = if (isDark) Color(0xFF131B2E) else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_password_input")
        )

        // 4. Confirm Password Input
        Text(
            text = "تکرار رمز عبور",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        OutlinedTextField(
            value = uiState.registerConfirmPassword,
            onValueChange = viewModel::onRegisterConfirmPasswordChanged,
            placeholder = { Text("تکرار رمز عبور") },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (confirmPasswordVisible) "مخفی کردن رمز عبور" else "نمایش رمز عبور",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = uiState.registerError != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onRegister() }
            ),
            singleLine = true,
            shape = RoundedCornerShape(RadiusMD),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                errorBorderColor = ExpenseRoseLight,
                focusedContainerColor = if (isDark) Color(0xFF131B2E) else MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("register_confirm_password_input")
        )

        if (uiState.registerError != null) {
            Text(
                text = uiState.registerError,
                style = MaterialTheme.typography.labelSmall,
                color = ExpenseRoseLight,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Register CTA Button
        Button(
            onClick = onRegister,
            enabled = uiState.canSubmitRegister,
            shape = RoundedCornerShape(RadiusMD),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .shadow(
                    elevation = if (uiState.canSubmitRegister) 4.dp else 0.dp,
                    shape = RoundedCornerShape(RadiusMD)
                )
                .testTag("register_submit_button")
        ) {
            if (uiState.isRegisterLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Text(
                        text = "در حال ساخت حساب...",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = "ساخت حساب و ورود",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Dedicated, Real-Working Forgot Password Bottom Sheet.
 * Validates registered Gmail, regenerates secure credentials in the local database,
 * launches the native Gmail/Email client, and provides instant copy/login affordances.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordBottomSheet(
    uiState: AuthUiState,
    viewModel: AuthViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Column {
                    Text(
                        text = "بازیابی اطلاعات ورود و رمز عبور",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ارسال نام کاربری و رمز جدید به جیمیل ثبت‌شده",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.recoveredCredentials == null) {
                // Input Stage
                Text(
                    text = "لطفاً همان جیمیل یا ایمیلی که هنگام ثبت‌نام وارد کرده‌اید را در کادر زیر بنویسید:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = uiState.forgotPasswordEmail,
                    onValueChange = viewModel::onForgotPasswordEmailChanged,
                    placeholder = { Text("مثال: yourname@gmail.com") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    isError = uiState.forgotPasswordError != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        errorBorderColor = ExpenseRoseLight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("forgot_password_email_input")
                )

                if (uiState.forgotPasswordError != null) {
                    Text(
                        text = uiState.forgotPasswordError,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = ExpenseRoseLight,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        viewModel.recoverPassword { recovery ->
                            Toast.makeText(context, "اطلاعات ورود به جیمیل آماده ارسال شد.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = uiState.forgotPasswordEmail.isNotBlank() && !uiState.isForgotPasswordLoading,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    if (uiState.isForgotPasswordLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "بازیابی و ارسال به جیمیل",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Success Stage with credentials & mail action
                val recovery = uiState.recoveredCredentials

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldPrimaryLight.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, EmeraldPrimaryLight.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldPrimaryLight,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "اطلاعات حساب با موفقیت بازیابی شد",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldPrimaryLight
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(EmeraldPrimaryLight.copy(alpha = 0.3f))
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نام کاربری شما:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = recovery.username,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "رمز عبور جدید:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = recovery.newTemporaryPassword,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Action 1: Launch Native Email/Gmail Client
                Button(
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${recovery.email}")
                            putExtra(Intent.EXTRA_SUBJECT, "اطلاعات ورود و بازیابی دارینو")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "کاربر گرامی دارینو،\nاطلاعات حساب شما بازیابی شد:\n\nنام کاربری: ${recovery.username}\nرمز عبور جدید: ${recovery.newTemporaryPassword}\n\nلطفاً پس از ورود، از بخش تنظیمات رمز خود را تغییر دهید."
                            )
                        }
                        try {
                            context.startActivity(Intent.createChooser(emailIntent, "ارسال به جیمیل / ایمیل"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "برنامه ایمیل در دستگاه یافت نشد.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ارسال مستقیم به برنامه جیمیل", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }

                // Action 2: Copy and Auto-fill into Login
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(recovery.newTemporaryPassword))
                        Toast.makeText(context, "رمز عبور جدید کپی شد و در فرم ورود قرار گرفت.", Toast.LENGTH_LONG).show()
                        viewModel.selectTab(AuthTab.LOGIN)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(RadiusMD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("کپی رمز و رفتن به صفحه ورود", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}
