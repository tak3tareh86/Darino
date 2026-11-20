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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("واریز", "برداشت", "انتقال وجه")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "مثال‌ها",
                subtitle = "آموزش جداگانه نشانه‌های هر نوع تراکنش",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Surface(
                            onClick = { selectedTab = index },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shadowElevation = if (selectedTab == index) 3.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selectedTab == index) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                when (selectedTab) {
                                    0 -> "آموزش تشخیص پیامک واریز"
                                    1 -> "آموزش تشخیص پیامک برداشت"
                                    else -> "آموزش تشخیص پیامک انتقال وجه"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "در همین بخش، پیامک نمونه و نشانه‌های ثابت مخصوص این تراکنش را بررسی کن.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            when (selectedTab) {
                0 -> {
                                    item {

                                        ExampleLessonCard(
                                        number = "۱",
                                        title = "پیامک واریز وجه بانک قرض‌الحسنه رسالت",
                                        sender = "بانک قرض‌الحسنه رسالت",
                                        message = buildAnnotatedString {
                                            append("10.9096543.1\n")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF087F5B))) { append("+") }
                                            append("30,000,000 \\n08/25_21:43\\nمانده: 30,000,000")
                                        },
                                        fixedTerms = listOf("965", "+"),
                                        variableTerms = "مبلغ، تاریخ، ساعت و مانده حساب",
                                        marker = "965/+",
                                        explanation = "نشانه می‌تواند عددی باشد؛ مثلاً چند رقم از شماره حساب. ولی تاریخ، ساعت، مبلغ و واحد پول نباید به‌عنوان نشانه ثبت شوند."
                                    )
                                    }
                    item {
                        TipCard(
                            title = "نکات تشخیص واریز",
                            tips = listOf(
                                "عبارت‌های ثابت مانند «واریز» و «نشست» را انتخاب کن؛ مبلغ، موجودی، تاریخ، ساعت و واحد پول متغیرند.",
                                "حداقل دو نشانه متفاوت وارد کن و آن‌ها را فقط با / جدا کن؛ همه نشانه‌ها باید در یک پیامک وجود داشته باشند.",
                                "نشانه‌ها را با چند پیامک واقعی واریز از همان بانک بررسی کن."
                            )
                        )
                    }
                }
                1 -> {
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
                        TipCard(
                            title = "نکات تشخیص برداشت",
                            tips = listOf(
                                "نشانه‌هایی مانند «خرید» و «برداشت» را از متن ثابت پیامک انتخاب کن.",
                                "نشانه می‌تواند عددی باشد؛ مثل چند رقم پایانی شماره حساب «0987».",
                                "مبلغ، موجودی، تاریخ، ساعت و واحد پول را نشانه ثبت نکن؛ این موارد در هر پیامک تغییر می‌کنند.",
                                "حداقل دو نشانه متفاوت را با / جدا کن؛ همه باید هم‌زمان در پیامک وجود داشته باشند."
                            )
                        )
                    }
                }
                else -> {
                                    item {

                                        ExampleLessonCard(
                                        number = "۳",
                                        title = "پیامک انتقال وجه کارت به کارت",
                                        sender = "بانک شهر",
                                        message = buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF2563EB))) { append("انتقال") }
                                            append(" ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF2563EB))) { append("وجه") }
                                            append(" کارتي\nبرداشت ")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF2563EB))) { append("از") }
                                            append(":1234567000")
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF087F5B))) { append("5670") }
                                            append("\nمبلغ:20,014,500ريال\nموجودي:77,060,364 ريال\n1405/08/29 15:57:42")
                                        },
                                        fixedTerms = listOf("انتقال", "از", "وجه", "5670"),
                                        variableTerms = "مبلغ، موجودی، تاریخ، ساعت و واحد پول",
                                        marker = "انتقال/از/وجه/5670",
                                        explanation = "نشانه می‌تواند عددی باشد؛ مثلاً چند رقم از شماره حساب. ولی تاریخ، ساعت، مبلغ و واحد پول نباید به‌عنوان نشانه ثبت شوند."
                                    )
                                    }
                    item {
                        TipCard(
                            title = "نکات تشخیص انتقال وجه",
                            tips = listOf(
                                "نشانه‌های ثابت این نمونه «انتقال»، «از»، «وجه» و «5670» هستند؛ نشانه عددی می‌تواند از شماره حساب انتخاب شود.",
                                "مبلغ، موجودی، تاریخ، ساعت و واحد پول متغیرند و نباید به‌عنوان نشانه ثبت شوند.",
                                "همه نشانه‌هایی که با / جدا می‌کنی باید هم‌زمان در همان پیامک وجود داشته باشند."
                            )
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun TipCard(title: String, tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleSmall)
            }
            tips.forEach { TipLine(it) }
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
