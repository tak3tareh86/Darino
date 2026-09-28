package com.example.loan.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.loan.domain.CalculationMethod
import com.example.loan.domain.CalculationResult
import com.example.loan.domain.DurationType
import com.example.loan.domain.InterestPeriod
import com.example.loan.domain.LoanCalculation
import com.example.loan.domain.LoanType
import com.example.loan.viewmodel.LoanCalculatorState
import com.example.loan.viewmodel.LoanCalculatorViewModel
import com.example.loan.viewmodel.LoanScreenTab
import com.example.ui.components.Soft3DIcon
import com.example.ui.screens.installments.model.InstallmentCategory
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.WarningAmberLight
import com.example.util.IranianPhoneUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCalculatorScreen(
    onBackClick: () -> Unit,
    onCreateInstallment: (InstallmentCategory, String, String, String, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoanCalculatorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var showSaveDialog by remember { mutableStateOf(false) }
    var saveTitleInput by remember { mutableStateOf("") }

    LaunchedEffect(state.successFeedbackMessage) {
        state.successFeedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "محاسبه‌گر اقساط و وام",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = "محاسبه سود، اقساط ماهانه و هزینه واقعی تسهیلات",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("loan_calculator_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Tab Switcher: محاسبه‌گر | مقایسه وام‌ها | محاسبات ذخیره شده
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LoanScreenTab.entries.forEach { tab ->
                    val isSelected = state.currentTab == tab
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.setTab(tab) }
                            .testTag("loan_tab_${tab.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        shadowElevation = if (isSelected) 3.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.5.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tab Content
            when (state.currentTab) {
                LoanScreenTab.CALCULATOR -> {
                    CalculatorMainTab(
                        state = state,
                        viewModel = viewModel,
                        onOpenSaveDialog = {
                            saveTitleInput = "${state.loanType.title} (${CalculationResult.formatMoney(state.amount)})"
                            showSaveDialog = true
                        },
                        onCreateInstallmentClick = {
                            val res = state.result
                            val cat = when (state.loanType) {
                                LoanType.BANK_LOAN -> InstallmentCategory.BANK_LOANS
                                LoanType.CAR_LOAN -> InstallmentCategory.CAR_INSURANCE
                                LoanType.PERSONAL_LOAN -> InstallmentCategory.HOME_LOANS
                                else -> InstallmentCategory.MISC
                            }
                            val durationMonths = when (state.durationType) {
                                DurationType.MONTHS -> state.duration
                                DurationType.YEARS -> state.duration * 12
                            }
                            val totalFormatted = res?.totalPaymentFormatted ?: CalculationResult.formatMoney(state.amount)
                            val monthlyFormatted = res?.monthlyPaymentFormatted ?: ""
                            val title = "${state.loanType.title} (${CalculationResult.formatMoney(state.amount)})"

                            onCreateInstallment(cat, title, totalFormatted, monthlyFormatted, durationMonths)
                        }
                    )
                }

                LoanScreenTab.COMPARISON -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            LoanComparisonScreen(
                                loan1Input = state.compLoan1Input,
                                loan2Input = state.compLoan2Input,
                                comparisonResult = state.comparisonResult,
                                onUpdateLoan1 = { viewModel.updateCompLoan1(it) },
                                onUpdateLoan2 = { viewModel.updateCompLoan2(it) }
                            )
                        }
                    }
                }

                LoanScreenTab.SAVED -> {
                    SavedCalculationsTab(
                        savedList = state.savedCalculations,
                        onLoadClick = { viewModel.loadCalculationIntoCalculator(it) },
                        onDeleteClick = { viewModel.deleteCalculation(it.id) }
                    )
                }
            }
        }
    }

    // Save Calculation Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "ذخیره محاسبه وام",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "برای این محاسبه یک عنوان مشخص کنید تا در آینده به راحتی به آن دسترسی داشته باشید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = saveTitleInput,
                        onValueChange = { saveTitleInput = it },
                        label = { Text("عنوان محاسبه") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_save_title_input"),
                        shape = RoundedCornerShape(RadiusMD),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCalculation(saveTitleInput)
                        showSaveDialog = false
                    },
                    shape = RoundedCornerShape(RadiusMD)
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("انصراف")
                }
            },
            shape = RoundedCornerShape(RadiusLG)
        )
    }
}

@Composable
private fun CalculatorMainTab(
    state: LoanCalculatorState,
    viewModel: LoanCalculatorViewModel,
    onOpenSaveDialog: () -> Unit,
    onCreateInstallmentClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Loan Type Selection Chips (Horizontally scrollable)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "نوع وام یا تسهیلات",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LoanType.entries.forEach { type ->
                        val isSelected = state.loanType == type
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.onLoanTypeChanged(type) }
                                .testTag("select_loantype_${type.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Soft3DIcon(
                                    imageRes = type.iconRes,
                                    contentDescription = type.title,
                                    size = 22.dp,
                                    containerShape = RoundedCornerShape(6.dp)
                                )
                                Text(
                                    text = type.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.5.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Loan Amount Field
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مبلغ وام (اصل تسهیلات)",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = CalculationResult.formatMoney(state.amount),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = state.amountInput,
                        onValueChange = { viewModel.onAmountInputChanged(it) },
                        placeholder = { Text("مثلاً ۱۰۰۰۰۰۰۰۰") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_amount_input"),
                        shape = RoundedCornerShape(RadiusMD),
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = "تومان",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )

                    // Quick Preset Chips for Amount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            20_000_000L to "۲۰ میلیون",
                            50_000_000L to "۵۰ میلیون",
                            100_000_000L to "۱۰۰ میلیون",
                            200_000_000L to "۲۰۰ میلیون",
                            500_000_000L to "۵۰۰ میلیون"
                        )
                        presets.forEach { (amt, label) ->
                            val isCurrent = state.amount == amt
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.onQuickAmountSelected(amt) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Interest Rate & Period Field
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نرخ سود تسهیلات",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Annual vs Monthly Switch
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            InterestPeriod.entries.forEach { period ->
                                val isSel = state.interestPeriod == period
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.onInterestPeriodChanged(period) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (isSel) 2.dp else 0.dp
                                ) {
                                    Text(
                                        text = period.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.interestRateInput,
                        onValueChange = { viewModel.onInterestRateInputChanged(it) },
                        placeholder = { Text("مثلاً ۲۳") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_interest_input"),
                        shape = RoundedCornerShape(RadiusMD),
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = "درصد ٪",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )

                    // Quick Preset Chips for Interest
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val ratePresets = listOf(
                            0.0 to "۰٪ (بدون کارمزد)",
                            4.0 to "۴٪ (قرض‌الحسنه)",
                            18.0 to "۱۸٪ (بانکی)",
                            23.0 to "۲۳٪ (مصوب)",
                            28.0 to "۲۸٪ (خرید کالا)"
                        )
                        ratePresets.forEach { (rate, label) ->
                            val isCurrent = state.interestRate == rate
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.onQuickInterestSelected(rate) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Repayment Duration Field
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(RadiusLG),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مدت بازپرداخت",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Months vs Years Switch
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            DurationType.entries.forEach { dtype ->
                                val isSel = state.durationType == dtype
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { viewModel.onDurationTypeChanged(dtype) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.surface else Color.Transparent,
                                    shadowElevation = if (isSel) 2.dp else 0.dp
                                ) {
                                    Text(
                                        text = dtype.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.durationInput,
                        onValueChange = { viewModel.onDurationInputChanged(it) },
                        placeholder = { Text("مثلاً ۳۶") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loan_duration_input"),
                        shape = RoundedCornerShape(RadiusMD),
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = state.durationType.shortTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    )

                    // Quick Preset Chips for Duration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val durationPresets = listOf(
                            6 to "۶ ماه",
                            12 to "۱۲ ماه (۱ سال)",
                            24 to "۲۴ ماه (۲ سال)",
                            36 to "۳۶ ماه (۳ سال)",
                            60 to "۶۰ ماه (۵ سال)"
                        )
                        durationPresets.forEach { (m, label) ->
                            val isCurrent = state.durationType == DurationType.MONTHS && state.duration == m
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.onQuickDurationSelected(m) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Calculation Method Selector (Annuity, Simple Interest, Custom)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "شیوه محاسبه سود",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CalculationMethod.entries.forEach { method ->
                    val isSelected = state.calculationType == method
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(RadiusMD))
                            .clickable { viewModel.onCalculationMethodChanged(method) },
                        shape = RoundedCornerShape(RadiusMD),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .border(
                                        BorderStroke(
                                            2.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = method.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = method.subtitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error message banner if validation fails
        if (state.errorMessage != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusMD),
                    color = ExpenseRoseLight.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ExpenseRoseLight.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = ExpenseRoseLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = state.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = ExpenseRoseLight
                        )
                    }
                }
            }
        }

        // 6. Result Card
        if (state.result != null) {
            item {
                LoanResultCard(
                    result = state.result,
                    onSaveClick = onOpenSaveDialog,
                    onCreateInstallmentClick = onCreateInstallmentClick
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SavedCalculationsTab(
    savedList: List<LoanCalculation>,
    onLoadClick: (LoanCalculation) -> Unit,
    onDeleteClick: (LoanCalculation) -> Unit
) {
    if (savedList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "هنوز محاسبه‌ای ذخیره نکرده‌اید",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "در تب محاسبه‌گر، پس از محاسبه وام روی «ذخیره محاسبه» کلیک کنید.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(savedList, key = { it.id }) { calc ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(RadiusLG),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = calc.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "مبلغ: ${CalculationResult.formatMoney(calc.amount)} | سود: ${IranianPhoneUtils.convertDigitsToPersian(calc.interestRate.toString())}٪ | ${IranianPhoneUtils.convertDigitsToPersian(calc.duration.toString())} ${calc.durationType.shortTitle}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { onDeleteClick(calc) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "حذف",
                                    tint = ExpenseRoseLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Metrics Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "قسط ماهانه",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CalculationResult.formatMoney(calc.monthlyPayment),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column {
                                Text(
                                    text = "سود کل",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CalculationResult.formatMoney(calc.totalInterest),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = ExpenseRoseLight
                                )
                            }

                            Column {
                                Text(
                                    text = "کل بازپرداخت",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CalculationResult.formatMoney(calc.totalPayment),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Load button
                        Button(
                            onClick = { onLoadClick(calc) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(RadiusMD),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "بارگذاری در محاسبه‌گر",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
