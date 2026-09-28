package com.example.ui.screens.settings.subviews

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD

@Composable
fun DataManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var activeDialogAction by remember { mutableStateOf<String?>(null) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "مدیریت داده‌ها و بازنشانی",
                subtitle = "حذف هدفمند داده‌ها و بازنشانی تنظیمات",
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
            // Warning Banner
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = if (isDark) 4.dp else 2.dp,
                            shape = RoundedCornerShape(RadiusLG)
                        ),
                    shape = RoundedCornerShape(RadiusLG),
                    color = ExpenseRoseLight.copy(alpha = if (isDark) 0.12f else 0.08f),
                    border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(RadiusMD))
                                .background(ExpenseRoseLight.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = ExpenseRoseLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "منطقه حساس و بدون بازگشت",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = ExpenseRoseLight
                            )
                            Text(
                                text = "عملیات‌های این بخش داده‌های انتخابی را به صورت غیرقابل بازگشت پاکسازی می‌کنند.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Selective Data Clear
            item {
                SettingsSectionCard(title = "پاکسازی دسته‌ای اطلاعات") {
                    SettingsItem(
                        title = "پاک کردن تاریخچه تراکنش‌ها",
                        subtitle = "حذف تمامی دریافتی‌ها و پرداختی‌های ثبت‌شده",
                        vectorIcon = Icons.Rounded.DeleteOutline,
                        isDanger = true,
                        onClick = {
                            activeDialogAction = "clear_transactions"
                            dialogTitle = "حذف تمامی تراکنش‌ها"
                            dialogMessage = "آیا مطمئن هستید؟ تمامی سوابق هزینه‌ها و درآمدهای ثبت‌شده حذف خواهند شد و قابل بازیابی نخواهند بود."
                        }
                    )

                    SettingsItem(
                        title = "پاک کردن خودروها و سوابق سرویس",
                        subtitle = "حذف اطلاعات خودروها، تعویض روغن‌ها و بیمه‌نامه‌ها",
                        vectorIcon = Icons.Rounded.DeleteOutline,
                        isDanger = true,
                        onClick = {
                            activeDialogAction = "clear_vehicles"
                            dialogTitle = "حذف خودروها و سوابق سرویس"
                            dialogMessage = "آیا مطمئن هستید؟ اطلاعات گاراژ خودرو و سوابق تعمیراتی به صورت کامل پاک خواهند شد."
                        }
                    )

                    SettingsItem(
                        title = "پاک کردن اقساط و بدهی‌ها",
                        subtitle = "حذف لیست اقساط فعال و تسویه‌شده",
                        vectorIcon = Icons.Rounded.DeleteOutline,
                        isDanger = true,
                        onClick = {
                            activeDialogAction = "clear_installments"
                            dialogTitle = "حذف تمامی اقساط"
                            dialogMessage = "آیا مطمئن هستید؟ تمامی اطلاعات سررسید اقساط حذف خواهند شد."
                        }
                    )
                }
            }

            // Full Factory Reset
            item {
                SettingsSectionCard(title = "بازنشانی کلی به حالت اولیه (Factory Reset)") {
                    SettingsItem(
                        title = "بازنشانی کامل تمام اطلاعات و تنظیمات",
                        subtitle = "حذف کامل کلیه داده‌های حساب‌ها، دسته‌بندی‌ها و تنظیمات به حالت روز اول",
                        vectorIcon = Icons.Rounded.RestartAlt,
                        isDanger = true,
                        onClick = {
                            activeDialogAction = "reset_all"
                            dialogTitle = "بازنشانی کامل برنامه"
                            dialogMessage = "آیا از بازنشانی کامل مطمئن هستید؟ تمامی اطلاعات شما حذف و برنامه به حالت پیش‌فرض اولیه برمی‌گردد."
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Confirmation Dialog
    SettingsConfirmationDialog(
        isOpen = activeDialogAction != null,
        title = dialogTitle,
        message = dialogMessage,
        confirmButtonText = "تأیید و پاکسازی",
        onConfirm = {
            activeDialogAction = null
        },
        onDismiss = { activeDialogAction = null }
    )
}
