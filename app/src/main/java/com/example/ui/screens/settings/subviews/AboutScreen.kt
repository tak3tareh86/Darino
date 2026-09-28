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
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Copyright
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var showLicensesDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "درباره برنامه",
                subtitle = "اطلاعات نسخه، تکنولوژی‌ها و مجوزها",
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
            // App Branding Hero Card
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
                                EmeraldPrimaryLight.copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        com.example.ui.components.DarinoBrandHeader(
                            logoSize = 80.dp,
                            showSubtitle = true,
                            subtitleText = "سامانه مدیریت جامع مالی، اقساط و خودرو",
                            isDark = isDark
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldPrimaryLight.copy(alpha = 0.12f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "نسخه ۱.۰.۰ (ساخت رسمی)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = EmeraldPrimaryLight
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "سامانه مدرن مدیریت هزینه‌ها، پس‌اندازها، اقساط، چک‌ها و سوابق سرویس‌های دوره‌ای خودرو با رابط کاربری نسل جدید و پشتیبانی کامل از زبان فارسی و تقویم شمسی.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                lineHeight = 19.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Tech Specs
            item {
                SettingsSectionCard(title = "مشخصات فنی و پلتفرم") {
                    SettingsItem(
                        title = "معماری نرم‌افزار",
                        subtitle = "Clean Architecture + MVVM Pattern",
                        vectorIcon = Icons.Rounded.Code,
                        iconAccentColor = Color(0xFF2563EB),
                        showChevron = false
                    )

                    SettingsItem(
                        title = "رابط کاربری",
                        subtitle = "Android Jetpack Compose & Material 3",
                        iconEmoji = "🎨",
                        showChevron = false
                    )

                    SettingsItem(
                        title = "امنیت و پایگاه داده",
                        subtitle = "مبتنی بر رمزنگاری آفلاین در حافظه امن دستگاه",
                        vectorIcon = Icons.Rounded.Security,
                        iconAccentColor = EmeraldPrimaryLight,
                        showChevron = false
                    )
                }
            }

            // Legal & Open Source
            item {
                SettingsSectionCard(title = "قوانین و مجوزها") {
                    SettingsItem(
                        title = "مجوزهای متن‌باز (Open Source Licenses)",
                        subtitle = "کتابخانه‌ها و ابزارهای مورد استفاده در پروژه",
                        vectorIcon = Icons.Rounded.Description,
                        iconAccentColor = Color(0xFF6366F1),
                        onClick = { showLicensesDialog = true }
                    )

                    SettingsItem(
                        title = "شرایط و قوانین استفاده",
                        subtitle = "توافقنامه حقوق کاربری و سلب مسئولیت",
                        vectorIcon = Icons.Rounded.Copyright,
                        iconAccentColor = Color(0xFF64748B),
                        onClick = { showTermsDialog = true }
                    )
                }
            }

            // Heart Footer
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "طراحی و توسعه با ",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " برای کاربران ایرانی",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    SettingsConfirmationDialog(
        isOpen = showLicensesDialog,
        title = "مجوزهای نرم‌افزاری",
        message = "این برنامه از کتابخانه‌های متن‌باز شامل AndroidX Jetpack, Compose Material3, Coroutines, Vico Charts و سایر پکیج‌های استاندارد تحت مجوز Apache 2.0 و MIT استفاده می‌نماید.",
        confirmButtonText = "بستن",
        cancelButtonText = "",
        isDanger = false,
        onConfirm = { showLicensesDialog = false },
        onDismiss = { showLicensesDialog = false }
    )

    SettingsConfirmationDialog(
        isOpen = showTermsDialog,
        title = "شرایط استفاده",
        message = "استفاده از این برنامه رایگان بوده و به عنوان دستیار کمکی برای امور مالی و نگهداری خودرو طراحی شده است. لطفاً در ثبت ارقام دقیق دقت کافی مبذول فرمایید.",
        confirmButtonText = "تأیید",
        cancelButtonText = "",
        isDanger = false,
        onConfirm = { showTermsDialog = false },
        onDismiss = { showTermsDialog = false }
    )
}
