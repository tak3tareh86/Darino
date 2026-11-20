package com.example.ui.screens.settings.subviews

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.ui.screens.settings.components.SettingsHeader

@Composable
fun SmsPatternExamplesScreen(onBackClick: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "مثال‌ها",
                subtitle = "آموزش استخراج نشانه از پیامک بانکی",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "نشانه را از کجای پیامک انتخاب کنیم؟",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "عبارت‌های ثابت و تکرارشونده را پیدا کن؛ اطلاعاتی را که در هر پیام تغییر می‌کنند، کنار بگذار.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            item {
                ExampleLessonCard(
                    number = "۱",
                    title = "پیامک واریز وجه",
                    sender = "بانک نمونه",
                    message = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF087F5B))) { append("واریز") }
                        append(" وجه: ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFB45309))) { append("۵,۰۰۰,۰۰۰ ریال") }
                        append(" به حساب ۱۲۳۴ ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF087F5B))) { append("نشست") }
                        append(".")
                    },
                    fixedTerms = listOf("واریز", "نشست"),
                    variableTerms = "مبلغ و شماره حساب",
                    marker = "واریز/نشست",
                    explanation = "در این نمونه، دو عبارت «واریز» و «نشست» هر دو در همان پیامک وجود دارند و برای تشخیص واریز به کار می‌آیند."
                )
            }
            item {
                ExampleLessonCard(
                    number = "۲",
                    title = "پیامک برداشت با کارت",
                    sender = "بانک شهر",
                    message = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF087F5B))) { append("خريد") }
                        append(" با کارت\n")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFFB45309))) { append("برداشت") }
                        append(" از:1234567000")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF2563EB))) { append("0987") }
                        append("\nمبلغ:400,000ريال\nموجودي:97,124,864 ريال\n1405/08/28 06:54:12")
                    },
                    fixedTerms = listOf("خرید", "برداشت", "0987"),
                    variableTerms = "مبلغ، موجودی، تاریخ، ساعت و واحد پول",
                    marker = "خرید/برداشت/0987",
                    explanation = "نشانه می‌تواند عددی هم باشد؛ مثلاً چند رقم پایانی شماره حساب مانند «0987». تاریخ، ساعت، مبلغ و واحد پول را هرگز به‌عنوان نشانه ثبت نکنید، چون در پیامک‌های مختلف تغییر می‌کنند."
                )
            }
            item {
                ExampleLessonCard(
                    number = "۳",
                    title = "پیامک انتقال وجه",
                    sender = "بانک نمونه",
                    message = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF2563EB))) { append("انتقال") }
                        append(" وجه کارت به کارت به مبلغ ۱,۲۰۰,۰۰۰ ریال ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF2563EB))) { append("انجام") }
                        append(" شد.")
                    },
                    fixedTerms = listOf("انتقال", "انجام"),
                    variableTerms = "مبلغ و شماره کارت‌ها",
                    marker = "انتقال/انجام",
                    explanation = "در این نمونه، دو واژه مستقل «انتقال» و «انجام» هر دو در پیامک وجود دارند. برای عبارت‌های مرکب، زیرخط و خط تیره در منطق تطبیق معادل‌اند؛ اما فاصله معمولی به زیرخط تبدیل نمی‌شود."
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("چند نکته مهم قبل از ثبت", style = MaterialTheme.typography.titleSmall)
                        }
                        TipLine("نشانه‌ها باید ثابت و مرتبط با نوع تراکنش باشند؛ مبلغ، موجودی، تاریخ، ساعت و واحد پول را به‌عنوان نشانه انتخاب نکن.")
                        TipLine("در منطق فعلی دارینو، تمام نشانه‌هایی که با / جدا می‌کنی باید هم‌زمان در همان پیامک وجود داشته باشند؛ / به معنی «یا» نیست.")
                        TipLine("حداقل دو نشانه متفاوت برای هر دسته لازم است. نشانه‌ها را با / از هم جدا کن.")
                        TipLine("سرشناسه فرستنده را جداگانه در کادر سرشماره متنی یا عددی ثبت کن؛ سرشناسه با نشانه تراکنش فرق دارد.")
                    }
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "اگر امکان دارد، نشانه‌ها را با چند پیامک واقعی از همان بانک بررسی کن تا پیامک‌های نامرتبط اشتباه تشخیص داده نشوند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ExampleLessonCard(
    number: String,
    title: String,
    sender: String,
    message: androidx.compose.ui.text.AnnotatedString,
    fixedTerms: List<String>,
    variableTerms: String,
    marker: String,
    explanation: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        Text(number, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sender, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Text("پیامک نمونه آموزشی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("عبارت‌های ثابت", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                fixedTerms.forEach { term ->
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            term,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Text("قسمت‌های متغیر: $variableTerms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("نشانه قابل ثبت", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(marker, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Text(explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TipLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text("•", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
