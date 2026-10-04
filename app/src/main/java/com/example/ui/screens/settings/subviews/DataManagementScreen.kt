package com.example.ui.screens.settings.subviews

import android.widget.Toast
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
import androidx.compose.material.icons.rounded.SettingsBackupRestore
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.LocalBackupRepository
import com.example.data.database.AppDatabase
import com.example.ui.screens.finance.data.LocalFinanceRepository
import com.example.ui.screens.installments.model.InstallmentMockDataSource
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.settings.components.SettingsItem
import com.example.ui.screens.settings.components.SettingsSectionCard
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM
import com.example.vehicle.data.VehicleRepository
import kotlinx.coroutines.launch

@Composable
fun DataManagementScreen(
    onBackClick: () -> Unit,
    prefsRepo: com.example.data.preferences.AppPreferencesRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var activeDialogAction by remember { mutableStateOf<String?>(null) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogMessage by remember { mutableStateOf("") }

    val financeRepo = LocalFinanceRepository.instance
    val vehicleRepo = VehicleRepository.instance
    val db = AppDatabase.getDatabase(context)
    val backupRepo = LocalBackupRepository(context)

    Scaffold(
        topBar = {
            SettingsHeader(
                title = "مدیریت داده‌ها و پاکسازی",
                showBack = true,
                onBackClick = onBackClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning Banner
            item {
                Surface(
                    shape = RoundedCornerShape(RadiusMD),
                    color = ExpenseRoseLight.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(RadiusSM))
                                .background(ExpenseRoseLight.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "هشدار",
                                tint = ExpenseRoseLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "هشدار پاکسازی اطلاعات",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp
                                ),
                                color = ExpenseRoseLight
                            )
                            Text(
                                text = "عملیات‌های این بخش داده‌های انتخابی را پاکسازی می‌کنند تا بتوانید اطلاعات واقعی خود را وارد نمایید.",
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

            // Quick Clean Slate (خام‌سازی کل برنامه و حذف داده‌های ماک تستی)
            item {
                SettingsSectionCard(title = "خام‌سازی برنامه و حذف داده‌های تستی") {
                    SettingsItem(
                        title = "پاکسازی کلیه داده‌های تستی (حالت خام و صفر)",
                        subtitle = "حذف تمامی تراکنش‌ها، خودروها و اقساط تستی جهت ورود اطلاعات واقعی",
                        vectorIcon = Icons.Rounded.DeleteForever,
                        iconAccentColor = ExpenseRoseLight,
                        isDanger = true,
                        onClick = {
                            activeDialogAction = "clean_slate"
                            dialogTitle = "خام‌سازی برنامه و حذف داده‌های تستی"
                            dialogMessage = "تمامی داده‌های نمونه و فرضی (تراکنش‌ها، خودروها، اقساط، بودجه‌ها و اهداف) پاک خواهند شد و برنامه آماده ثبت اطلاعات واقعی شما می‌شود. آیا ادامه می‌دهید؟"
                        }
                    )

                    SettingsItem(
                        title = "بازیابی داده‌های تستی و نمونه (جهت بررسی و دمو)",
                        subtitle = "بازگرداندن مجدد تراکنش‌ها، خودروها و اقساط نمونه به برنامه",
                        vectorIcon = Icons.Rounded.SettingsBackupRestore,
                        iconAccentColor = EmeraldPrimaryLight,
                        onClick = {
                            activeDialogAction = "restore_samples"
                            dialogTitle = "بازیابی داده‌های نمونه و تستی"
                            dialogMessage = "آیا مایلید داده‌های تستی و پیش‌فرض برنامه جهت بررسی و دمو مجدداً بارگذاری شوند؟"
                        }
                    )
                }
            }

            // Selective Data Clear
            item {
                SettingsSectionCard(title = "پاکسازی دسته‌ای اطلاعات") {
                    SettingsItem(
                        title = "پاک کردن تاریخچه تراکنش‌ها و بودجه‌ها",
                        subtitle = "حذف تمامی دریافتی‌ها، پرداختی‌ها، بودجه‌ها و اهداف پس‌انداز",
                        vectorIcon = Icons.Rounded.DeleteOutline,
                        isDanger = true,
                        onClick = {
                            activeDialogAction = "clear_transactions"
                            dialogTitle = "حذف تمامی تراکنش‌ها"
                            dialogMessage = "آیا مطمئن هستید؟ تمامی سوابق هزینه‌ها و درآمدهای ثبت‌شده حذف خواهند شد."
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
                SettingsSectionCard(title = "بازنشانی کلی به حالت کارخانه (Factory Reset)") {
                    SettingsItem(
                        title = "بازنشانی کامل تمام اطلاعات و دیتابیس",
                        subtitle = "حذف کامل کلیه داده‌های دیتابیس، حساب‌ها و تنظیمات به حالت اولیه",
                        vectorIcon = Icons.Rounded.RestartAlt,
                        isDanger = true,
                        onClick = {
                            activeDialogAction = "reset_all"
                            dialogTitle = "بازنشانی کامل برنامه"
                            dialogMessage = "آیا از بازنشانی کامل مطمئن هستید؟ تمامی اطلاعات دیتابیس و حافظه پاک شده و برنامه صفر خواهد شد."
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
        confirmButtonText = if (activeDialogAction == "restore_samples") "تأیید و بازیابی" else "تأیید و پاکسازی",
        onConfirm = {
            val action = activeDialogAction
            activeDialogAction = null
            scope.launch {
                when (action) {
                    "clean_slate" -> {
                        db.clearAllTables()
                        prefsRepo.setCleanSlatePerformed(true)
                        Toast.makeText(context, "برنامه با موفقیت خام‌سازی شد و تمامی داده‌ها پاک شدند.", Toast.LENGTH_LONG).show()
                    }
                    "restore_samples" -> {
                        prefsRepo.setCleanSlatePerformed(false)
                        financeRepo.restoreSampleTransactions()
                        vehicleRepo.restoreSampleVehicles()
                        InstallmentMockDataSource.restoreSampleInstallments(context)
                        Toast.makeText(context, "داده‌های تستی و نمونه با موفقیت بازیابی شدند.", Toast.LENGTH_SHORT).show()
                    }
                    "clear_transactions" -> {
                        financeRepo.clearAllTransactionsData()
                        val userId = com.example.data.security.SessionManager.userId ?: ""
                        db.transactionDao().clearAllTransactions(userId)
                        Toast.makeText(context, "تراکنش‌ها و بودجه‌ها با موفقیت پاکسازی شدند.", Toast.LENGTH_SHORT).show()
                    }
                    "clear_vehicles" -> {
                        vehicleRepo.clearAllVehiclesData()
                        val userId = com.example.data.security.SessionManager.userId ?: ""
                        db.vehicleDao().clearAllVehicles(userId)
                        db.vehicleDao().clearAllServices(userId)
                        Toast.makeText(context, "خودروها و سوابق سرویس با موفقیت پاک شدند.", Toast.LENGTH_SHORT).show()
                    }
                    "clear_installments" -> {
                        InstallmentMockDataSource.clearAllInstallments(context)
                        val userId = com.example.data.security.SessionManager.userId ?: ""
                        db.installmentDao().clearAllInstallments(userId)
                        Toast.makeText(context, "اقساط و بدهی‌ها با موفقیت پاک شدند.", Toast.LENGTH_SHORT).show()
                    }
                    "reset_all" -> {
                        financeRepo.clearAllTransactionsData()
                        vehicleRepo.clearAllVehiclesData()
                        InstallmentMockDataSource.clearAllInstallments(context)
                        backupRepo.deleteAllData()
                        Toast.makeText(context, "برنامه به حالت صفر کارخانه بازنشانی گردید.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        },
        onDismiss = { activeDialogAction = null }
    )
}
