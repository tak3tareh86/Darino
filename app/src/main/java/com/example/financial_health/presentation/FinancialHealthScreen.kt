package com.example.financial_health.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.financial_health.domain.FinancialHealthStatus
import com.example.financial_health.presentation.components.EditIncomeBottomSheet
import com.example.financial_health.presentation.components.FinancialInsightCard
import com.example.financial_health.presentation.components.FinancialPressureChart
import com.example.financial_health.viewmodel.FinancialHealthState
import com.example.financial_health.viewmodel.FinancialHealthViewModel
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialHealthScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FinancialHealthViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val decimalFormat = remember { DecimalFormat("#,###") }

    var showEditSheet by remember { mutableStateOf(false) }
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("financial_health_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "وضعیت مالی من",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = "تحلیل فشار مالی و تعهدات ماهانه",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("btn_back_financial_health")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.testTag("btn_edit_income_topbar")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "ویرایش درآمد",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero 3D Health Meter Card
            item {
                HealthOverviewHeroCard(
                    state = state,
                    onEditIncomeClick = { showEditSheet = true }
                )
            }

            // 2. Breakdown Matrix Card (درآمد، اقساط، هزینه‌های ثابت، مجموع)
            item {
                FinancialCommitmentsBreakdownCard(
                    state = state,
                    decimalFormat = decimalFormat,
                    onEditClick = { showEditSheet = true }
                )
            }

            // 3. Installments Detailed Analysis («تحلیل اقساط»)
            item {
                InstallmentsAnalysisSectionCard(
                    state = state,
                    decimalFormat = decimalFormat
                )
            }

            // 4. Next Month Commitments Prediction («ماه آینده»)
            item {
                NextMonthPredictionSectionCard(
                    state = state,
                    decimalFormat = decimalFormat
                )
            }

            // 5. Smart Alerts & Darino Analysis («تحلیل دارینو و هشدارهای هوشمند»)
            item {
                SmartAlertsSection(state = state)
            }

            // 6. Historical Financial Pressure Trend Chart (نمودار فشار مالی در ماه‌های گذشته)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "تحلیل سلامت مالی در ماه‌های اخیر",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    FinancialPressureChart(
                        dataPoints = state.historicalPressureList
                    )
                }
            }

            // Bottom Spacing
            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Modal Edit Sheet
    if (showEditSheet) {
        EditIncomeBottomSheet(
            currentIncome = state.monthlyIncome,
            currentFixedExpenses = state.fixedExpenses,
            sheetState = editSheetState,
            onDismiss = { showEditSheet = false },
            onSave = { income, fixed ->
                viewModel.saveProfile(income, state.totalInstallments, fixed)
                showEditSheet = false
            }
        )
    }
}

@Composable
private fun HealthOverviewHeroCard(
    state: FinancialHealthState,
    onEditIncomeClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val status = state.healthStatus
    val statusColor = status.color

    val animatedProgress by animateFloatAsState(
        targetValue = (state.pressurePercentage / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "HeroPressureGauge"
    )

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isDark) Color(0xFF141E30) else Color(0xFFFFFFFF),
        borderColor = statusColor.copy(alpha = 0.35f),
        elevation = 3.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_analytics,
                        contentDescription = "آیکون تحلیل سلامت مالی",
                        size = 44.dp,
                        accentColor = statusColor
                    )
                    Column {
                        Text(
                            text = "شاخص فشار مالی",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "سهم تعهدات از کل درآمد شما",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) status.darkContainerColor.copy(alpha = 0.7f) else status.lightContainerColor,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = status.badgeText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = if (isDark) Color.White else statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Big Percentage Centerpiece
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${IranianPhoneUtils.convertDigitsToPersian(state.pressurePercentage.toInt().toString())}٪",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 42.sp
                            ),
                            color = statusColor
                        )
                        Text(
                            text = "فشار مالی ماهانه",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Text(
                        text = status.defaultMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Progress Bar with 3-zone markers
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF1E2B42) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        statusColor
                                    )
                                )
                            )
                    )
                }

                // Range Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "۰٪ (آزاد)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "۳۰٪ (مرز امن)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "۵۰٪ (هشدار)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        text = "۱۰۰٪",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialCommitmentsBreakdownCard(
    state: FinancialHealthState,
    decimalFormat: DecimalFormat,
    onEditClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isDark) Color(0xFF141E30) else Color(0xFFFFFFFF),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ترکیب تعهدات و درآمد ماهانه",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(
                    onClick = onEditClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "تغییر درآمد",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp
                        )
                    )
                }
            }

            // Grid items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CommitmentRowItem(
                    title = "درآمد ماهانه من",
                    amount = state.monthlyIncome,
                    decimalFormat = decimalFormat,
                    color = MaterialTheme.colorScheme.primary,
                    icon = Icons.Rounded.AccountBalanceWallet,
                    percentageText = "پایه ۱۰۰٪"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )

                CommitmentRowItem(
                    title = "اقساط فعال",
                    amount = state.totalInstallments,
                    decimalFormat = decimalFormat,
                    color = Color(0xFF3B82F6),
                    icon = Icons.Rounded.ReceiptLong,
                    percentageText = "${IranianPhoneUtils.convertDigitsToPersian(((state.totalInstallments.toDouble() / state.monthlyIncome.coerceAtLeast(1)) * 100).toInt().toString())}٪ از درآمد"
                )

                CommitmentRowItem(
                    title = "هزینه‌های ثابت",
                    amount = state.fixedExpenses,
                    decimalFormat = decimalFormat,
                    color = Color(0xFF8B5CF6),
                    icon = Icons.Rounded.HomeWork,
                    percentageText = "${IranianPhoneUtils.convertDigitsToPersian(((state.fixedExpenses.toDouble() / state.monthlyIncome.coerceAtLeast(1)) * 100).toInt().toString())}٪ از درآمد"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )

                // Total commitments summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "مجموع کل تعهدات",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "اقساط + هزینه‌های ثابت",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = IranianPhoneUtils.convertDigitsToPersian(decimalFormat.format(state.totalCommitments)) + " تومان",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "معادل ${IranianPhoneUtils.convertDigitsToPersian(state.pressurePercentage.toInt().toString())}٪ درآمد",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = state.healthStatus.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommitmentRowItem(
    title: String,
    amount: Long,
    decimalFormat: DecimalFormat,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    percentageText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = percentageText,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = IranianPhoneUtils.convertDigitsToPersian(decimalFormat.format(amount)) + " تومان",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun InstallmentsAnalysisSectionCard(
    state: FinancialHealthState,
    decimalFormat: DecimalFormat
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isDark) Color(0xFF141E30) else Color(0xFFFFFFFF),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Soft3DIcon(
                    imageRes = R.drawable.img_3d_installment,
                    contentDescription = "آیکون تحلیل اقساط",
                    size = 34.dp,
                    accentColor = Color(0xFF3B82F6)
                )
                Column {
                    Text(
                        text = "تحلیل اقساط",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "وضعیت تعهدات وام‌ها و سررسیدها",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2x2 Metric Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 1: Count
                AnalysisMetricTile(
                    title = "تعداد اقساط فعال",
                    value = "${IranianPhoneUtils.convertDigitsToPersian(state.activeInstallmentsCount.toString())} مورد",
                    subtext = "در حال بازپرداخت",
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF3B82F6)
                )

                // Metric 2: Monthly Payment
                AnalysisMetricTile(
                    title = "مجموع پرداخت ماهانه",
                    value = state.totalInstallmentsMonthlyFormatted,
                    subtext = "تعهد ماه جاری",
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Metric 3: Nearest Due Date
                AnalysisMetricTile(
                    title = "نزدیک‌ترین سررسید",
                    value = "${IranianPhoneUtils.convertDigitsToPersian(state.nearestDueDateDays.toString())} روز دیگر",
                    subtext = state.nearestDueDateTitle,
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFFF59E0B)
                )

                // Metric 4: Largest Installment
                AnalysisMetricTile(
                    title = "بیشترین قسط",
                    value = state.largestInstallmentTitle,
                    subtext = IranianPhoneUtils.convertDigitsToPersian(decimalFormat.format(state.largestInstallmentAmount)) + " ت",
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun AnalysisMetricTile(
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    accentColor: Color
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF18243A) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
            )
        }
    }
}

@Composable
private fun NextMonthPredictionSectionCard(
    state: FinancialHealthState,
    decimalFormat: DecimalFormat
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val prediction = state.nextMonthPrediction

    Layered3DCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = if (isDark) Color(0xFF141E30) else Color(0xFFFFFFFF),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_calendar,
                        contentDescription = "آیکون تقویم ماه آینده",
                        size = 32.dp,
                        accentColor = Color(0xFF8B5CF6)
                    )
                    Column {
                        Text(
                            text = "ماه آینده",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "پیش‌بینی تعهدات و هزینه‌های ماه بعد",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "مجموع: ${IranianPhoneUtils.convertDigitsToPersian(decimalFormat.format(prediction.total))} ت",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Predictions List
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PredictionItemRow(
                    label = "اقساط",
                    amount = prediction.installments,
                    decimalFormat = decimalFormat,
                    color = Color(0xFF3B82F6)
                )
                PredictionItemRow(
                    label = "یادآورها",
                    amount = prediction.reminders,
                    decimalFormat = decimalFormat,
                    color = Color(0xFFF59E0B)
                )
                PredictionItemRow(
                    label = "هزینه خودرو",
                    amount = prediction.vehicleExpenses,
                    decimalFormat = decimalFormat,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
private fun PredictionItemRow(
    label: String,
    amount: Long,
    decimalFormat: DecimalFormat,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = IranianPhoneUtils.convertDigitsToPersian(decimalFormat.format(amount)) + " تومان",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SmartAlertsSection(state: FinancialHealthState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.TipsAndUpdates,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "هشدارهای هوشمند و تحلیل دارینو",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Comparison chip (ماه قبل vs ماه جاری)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ماه قبل: ${IranianPhoneUtils.convertDigitsToPersian(state.previousMonthPressure.toInt().toString())}٪",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "←",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ماه جاری: ${IranianPhoneUtils.convertDigitsToPersian(state.pressurePercentage.toInt().toString())}٪",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = state.healthStatus.color
                    )
                )
            }
        }

        // List of generated insights
        state.insights.forEach { insight ->
            FinancialInsightCard(insight = insight)
        }
    }
}
