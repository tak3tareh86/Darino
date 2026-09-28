package com.example.ui.screens.settings.subviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.ui.screens.settings.model.FaqItem
import com.example.ui.screens.settings.model.SettingsMockDataSource
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFaqId by remember { mutableStateOf<String?>(null) }
    var isFeedbackSheetOpen by remember { mutableStateOf(false) }
    var feedbackType by remember { mutableStateOf("گزارش مشکل") }
    var showThankYouDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "پشتیبانی و راهنما",
                subtitle = "پاسخ به سوالات و ارتباط با تیم توسعه",
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
            // Quick Actions
            item {
                SettingsSectionCard(title = "خدمات پشتیبانی") {
                    SettingsItem(
                        title = "گزارش مشکل یا باگ",
                        subtitle = "ثبت ایرادات فنی یا ناهماهنگی در عملکرد برنامه",
                        vectorIcon = Icons.Rounded.BugReport,
                        iconAccentColor = Color(0xFFE11D48),
                        onClick = {
                            feedbackType = "گزارش مشکل"
                            isFeedbackSheetOpen = true
                        }
                    )

                    SettingsItem(
                        title = "ارسال پیشنهاد و بازخورد",
                        subtitle = "ارائه نظرات و امکانات پیشنهادی برای نسخه‌های آتی",
                        vectorIcon = Icons.Rounded.Feedback,
                        iconAccentColor = EmeraldPrimaryLight,
                        onClick = {
                            feedbackType = "ارسال پیشنهاد"
                            isFeedbackSheetOpen = true
                        }
                    )
                }
            }

            // Contact Channels
            item {
                SettingsSectionCard(title = "راه‌های ارتباطی مستقیم") {
                    SettingsItem(
                        title = "پشتیبانی از طریق ایمیل",
                        subtitle = "support@financemanager.app",
                        vectorIcon = Icons.Rounded.Email,
                        iconAccentColor = Color(0xFF2563EB),
                        showChevron = false
                    )

                    SettingsItem(
                        title = "کانال و پشتیبانی تلگرام",
                        subtitle = "@FinanceManagerApp_Support",
                        iconEmoji = "✈️",
                        showChevron = false
                    )
                }
            }

            // FAQ Section
            item {
                Text(
                    text = "پرسش‌های متداول (FAQ)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            items(SettingsMockDataSource.faqs, key = { it.id }) { faq ->
                FaqExpandableCard(
                    faq = faq,
                    isExpanded = expandedFaqId == faq.id,
                    onToggle = {
                        expandedFaqId = if (expandedFaqId == faq.id) null else faq.id
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (isFeedbackSheetOpen) {
        FeedbackBottomSheet(
            title = feedbackType,
            onDismiss = { isFeedbackSheetOpen = false },
            onSubmit = {
                isFeedbackSheetOpen = false
                showThankYouDialog = true
            }
        )
    }

    SettingsConfirmationDialog(
        isOpen = showThankYouDialog,
        title = "پیام شما دریافت شد",
        message = "از همراهی و ارسال بازخورد ارزشمند شما سپاسگزاریم. پیام شما بررسی خواهد شد.",
        confirmButtonText = "تأیید",
        cancelButtonText = "بستن",
        isDanger = false,
        onConfirm = { showThankYouDialog = false },
        onDismiss = { showThankYouDialog = false }
    )
}

@Composable
private fun FaqExpandableCard(
    faq: FaqItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 4.dp else 1.5.dp,
                shape = RoundedCornerShape(RadiusMD)
            )
            .clip(RoundedCornerShape(RadiusMD))
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.03f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimaryLight.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = EmeraldPrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = faq.question,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 38.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var content by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("شرح بازخورد یا توضیح مشکل") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 4,
                shape = RoundedCornerShape(RadiusMD)
            )

            OutlinedTextField(
                value = emailOrPhone,
                onValueChange = { emailOrPhone = it },
                label = { Text("ایمیل یا شماره تماس جهت پیگیری (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            Button(
                onClick = onSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_feedback_button"),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "ارسال بازخورد",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = Color.White
                )
            }
        }
    }
}
