package com.example.ui.screens.settings.components

import android.net.Uri
import android.util.Log
import java.io.File
import java.util.UUID
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.security.SessionManager
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
    val context = LocalContext.current
    // Keep each account's image in private app storage under a stable account-specific name.
    val profileKey = currentUser?.id?.takeIf { it.isNotBlank() } ?: "local_profile"
    val profilePhotoFile = remember(profileKey) {
        val stableId = UUID.nameUUIDFromBytes(profileKey.toByteArray(Charsets.UTF_8)).toString()
        File(File(context.filesDir, "profile_photos"), "$stableId.img")
    }

    val initialName = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: ""
    var userNameInput by remember { mutableStateOf(initialName) }
    var selectedImageUri by remember(profileKey) {
        mutableStateOf(
            profilePhotoFile.takeIf { it.isFile && it.length() > 0L }?.let(Uri::fromFile)
        )
    }
    // Android zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                profilePhotoFile.parentFile?.mkdirs()
                val temporaryFile = File(profilePhotoFile.parentFile, "${profilePhotoFile.name}.tmp")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temporaryFile.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("Could not open selected profile image")

                if (!temporaryFile.renameTo(profilePhotoFile)) {
                    temporaryFile.copyTo(profilePhotoFile, overwrite = true)
                    temporaryFile.delete()
                }
                selectedImageUri = Uri.fromFile(profilePhotoFile)
            } catch (error: Exception) {
                Log.e("UserAccountCard", "Could not save profile photo", error)
            }
        }
    }

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

                // Keep the only account action clear and easy to reach.
                OutlinedButton(
                    onClick = onLogoutClick,
                    shape = RoundedCornerShape(RadiusMD),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ExpenseRoseLight
                    ),
                    border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                        contentDescription = "خروج از حساب",
                        modifier = Modifier.size(17.dp),
                        tint = ExpenseRoseLight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "خروج از حساب",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRoseLight
                    )
                }
            }
        }
    }

}
