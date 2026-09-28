package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun DataBackupScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var showFeatureNoticeDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "پشتیبان‌گیری و بازیابی",
                subtitle = "حفاظت از اطلاعات مالی و سوابق خودرو",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 3D Hero Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 8.dp else 4.dp,
                            shape = RoundedCornerShape(RadiusLG)
                        ),
                    shape = RoundedCornerShape(RadiusLG),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                EmeraldPrimaryLight.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(6.dp, RoundedCornerShape(RadiusMD), spotColor = EmeraldPrimaryLight)
                                .clip(RoundedCornerShape(RadiusMD))
                                .background(EmeraldPrimaryLight.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_3d_cloud_backup),
                                contentDescription = "پشتیبان‌گیری",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(RadiusMD)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "حفاظت از داده‌های شما",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "ایجاد نسخه پشتیبان به شما امکان انتقال اطلاعات به گوشی جدید و جلوگیری از حذف تصادفی را می‌دهد.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Create Backup Section
            item {
                SettingsSectionCard(title = "پشتیبان‌گیری (تهیه نسخه خروجی)") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "آخرین پشتیبان ذخیره شده:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "۲ روز پیش (۲۶ شهریور ۱۴۰۳)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EmeraldPrimaryLight.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "حجم: ۴۸۰ کیلوبایت",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = EmeraldPrimaryLight
                                )
                            }
                        }

                        Button(
                            onClick = {
                                dialogTitle = "تهیه نسخه پشتیبان"
                                dialogMessage = "این قابلیت در نسخه نهایی فعال خواهد شد و فایل پشتیبان در حافظه یا فضای ابری ذخیره می‌گردد."
                                showFeatureNoticeDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("create_backup_button"),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "ایجاد نسخه پشتیبان جدید",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Restore Section
            item {
                SettingsSectionCard(title = "بازیابی اطلاعات (Restore)") {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "اگر قبلاً نسخه پشتیبانی با فرمت اختصاصی برنامه ذخیره کرده‌اید، می‌توانید آن را انتخاب و اطلاعات را بازیابی نمایید:",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                dialogTitle = "بازیابی اطلاعات"
                                dialogMessage = "این قابلیت در نسخه نهایی فعال خواهد شد و فایل پشتیبان مورد نظر خوانده و جایگزین می‌شود."
                                showFeatureNoticeDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("restore_backup_button"),
                            shape = ButtonShape
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "انتخاب فایل پشتیبان از حافظه",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Security Notice
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = EmeraldPrimaryLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "فایل‌های پشتیبان به صورت رمزگذاری‌شده و ایمن ذخیره می‌شوند.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Informational Notice Dialog
    SettingsConfirmationDialog(
        isOpen = showFeatureNoticeDialog,
        title = dialogTitle,
        message = dialogMessage,
        confirmButtonText = "متوجه شدم",
        cancelButtonText = "بستن",
        isDanger = false,
        onConfirm = { showFeatureNoticeDialog = false },
        onDismiss = { showFeatureNoticeDialog = false }
    )
}
