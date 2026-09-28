package com.example.ui.screens.settings.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.security.SessionManager
import com.example.data.security.AuthSessionManager
import com.example.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

/**
 * Top User Account Card displayed prominently at the head of the Settings Screen.
 * Allows user to manually enter name, pick profile photo from gallery, and shows login phone in LTR digits.
 */
@Composable
fun UserAccountCard(
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val currentUser = SessionManager.currentUser

    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionManager = remember { AuthSessionManager(context) }
    val activeSession = sessionManager.getActiveSession()
    val rawUsername = activeSession?.username ?: "username"

    val initialName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: ""
    var userNameInput by remember { mutableStateOf(initialName) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val scope = rememberCoroutineScope()
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var currentPasswordInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var passwordErrorMessage by remember { mutableStateOf<String?>(null) }
    var passwordSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val rawPhone = currentUser?.phoneNumber ?: "09121234567"

    val cardBgGradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF131D31),
                Color(0xFF1E293B),
                Color(0xFF0F3E38)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFF0FDF4),
                Color(0xFFECFDF5),
                Color(0xFFE6FFFA)
            )
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 8.dp else 4.dp,
                shape = RoundedCornerShape(RadiusLG),
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = EmeraldPrimaryLight.copy(alpha = 0.3f)
            )
            .testTag("user_account_card"),
        shape = RoundedCornerShape(RadiusLG),
        color = Color.Transparent,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) EmeraldPrimaryLight.copy(alpha = 0.35f) else EmeraldPrimaryLight.copy(alpha = 0.45f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBgGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row: Avatar Picker on one side, Name TextField & LTR Phone on the other
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo Avatar with Photo Picker overlay
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(EmeraldPrimaryLight, EmeraldPrimaryDark)
                                )
                            )
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("profile_photo_picker"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "عکس انتخابی کاربر",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_3d_shield_security),
                                contentDescription = "آواتار پیش‌فرض",
                                modifier = Modifier.size(46.dp),
                                contentScale = ContentScale.Fit
                            )
                        }

                        // Small camera badge at bottom
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CameraAlt,
                                contentDescription = "انتخاب عکس از گالری",
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // User Inputs: Manual Name Entry & Phone (LTR)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Manual Name Input Field (بالای کادر کاربر اسمش رو دستی وارد کنه)
                        OutlinedTextField(
                            value = userNameInput,
                            onValueChange = {
                                userNameInput = it
                                SessionManager.updateFullName(it)
                            },
                            label = { Text("نام و نام خانوادگی", fontSize = 11.sp) },
                            placeholder = { Text("نام خود را وارد کنید", fontSize = 12.sp) },
                            singleLine = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "ویرایش نام",
                                    modifier = Modifier.size(16.dp),
                                    tint = EmeraldPrimaryLight
                                )
                            },
                            textStyle = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(RadiusMD),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimaryLight,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                focusedContainerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.5f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .testTag("user_name_input")
                        )

                        // 2. Username details (پایین اسمش نام کاربری LTR از چپ به راست)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = EmeraldPrimaryLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "نام کاربری:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }

                            // Force Left-To-Right (LTR) for Username text
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Surface(
                                    shape = RoundedCornerShape(RadiusSM),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = rawUsername,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = EmeraldPrimaryLight
                                    )
                                }
                            }
                        }
                    }
                }



                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                        )
                )

                // Actions row: Change Password & Logout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showChangePasswordDialog = true },
                        shape = RoundedCornerShape(RadiusMD),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EmeraldPrimaryLight
                        ),
                        border = BorderStroke(1.dp, EmeraldPrimaryLight.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("change_password_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "تغییر رمز",
                            modifier = Modifier.size(15.dp),
                            tint = EmeraldPrimaryLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تغییر رمز کاربری",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }

                    OutlinedButton(
                        onClick = onLogoutClick,
                        shape = RoundedCornerShape(RadiusMD),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ExpenseRoseLight
                        ),
                        border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                            contentDescription = "خروج",
                            modifier = Modifier.size(15.dp),
                            tint = ExpenseRoseLight
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "خروج از حساب",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRoseLight
                        )
                    }
                }
            }
        }
    }

    // Change Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = {
                Text(text = "تغییر رمز عبور کاربری", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (passwordErrorMessage != null) {
                        Text(
                            text = passwordErrorMessage!!,
                            color = ExpenseRoseLight,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (passwordSuccessMessage != null) {
                        Text(
                            text = passwordSuccessMessage!!,
                            color = EmeraldPrimaryLight,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("رمز عبور فعلی") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("رمز عبور جدید (حداقل ۸ کاراکتر)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmPasswordInput,
                        onValueChange = { confirmPasswordInput = it },
                        label = { Text("تکرار رمز عبور جدید") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPasswordInput.length < 8) {
                            passwordErrorMessage = "رمز عبور جدید باید حداقل ۸ کاراکتر باشد."
                            return@TextButton
                        }
                        if (newPasswordInput != confirmPasswordInput) {
                            passwordErrorMessage = "رمز عبور جدید و تکرار آن مطابقت ندارند."
                            return@TextButton
                        }

                        scope.launch(Dispatchers.IO) {
                            try {
                                val db = AppDatabase.getDatabase(context)
                                val userDao = db.userDao()
                                val user = userDao.getFirstUser()
                                if (user != null) {
                                    if (!user.passwordHash.isNullOrBlank() && !user.salt.isNullOrBlank()) {
                                        val isValid = PasswordHasher.verifyPassword(currentPasswordInput, user.salt, user.passwordHash)
                                        if (!isValid) {
                                            passwordErrorMessage = "رمز عبور فعلی اشتباه است."
                                            return@launch
                                        }
                                    }
                                    val newSalt = PasswordHasher.generateSalt()
                                    val newHash = PasswordHasher.hashPassword(newPasswordInput, newSalt)
                                    userDao.updatePassword(user.id, newHash, newSalt)
                                    passwordSuccessMessage = "رمز عبور با موفقیت تغییر یافت."
                                    kotlinx.coroutines.delay(1200)
                                    showChangePasswordDialog = false
                                    currentPasswordInput = ""
                                    newPasswordInput = ""
                                    confirmPasswordInput = ""
                                    passwordErrorMessage = null
                                    passwordSuccessMessage = null
                                } else {
                                    passwordErrorMessage = "کاربری یافت نشد."
                                }
                            } catch (e: Exception) {
                                passwordErrorMessage = "خطا در تغییر رمز: ${e.localizedMessage}"
                            }
                        }
                    }
                ) {
                    Text("تایید و تغییر رمز", fontWeight = FontWeight.Bold, color = EmeraldPrimaryLight)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangePasswordDialog = false
                        passwordErrorMessage = null
                        passwordSuccessMessage = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}
