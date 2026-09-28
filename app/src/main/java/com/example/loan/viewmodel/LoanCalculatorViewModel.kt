package com.example.loan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loan.data.LoanRepository
import com.example.loan.domain.CalculationMethod
import com.example.loan.domain.CalculationResult
import com.example.loan.domain.ComparisonLoanInput
import com.example.loan.domain.DurationType
import com.example.loan.domain.InterestPeriod
import com.example.loan.domain.LoanCalculation
import com.example.loan.domain.LoanCalculatorEngine
import com.example.loan.domain.LoanType
import com.example.util.IranianPhoneUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class LoanCalculatorViewModel(
    private val repository: LoanRepository = LoanRepository(),
    private val engine: LoanCalculatorEngine = LoanCalculatorEngine()
) : ViewModel() {

    private val _state = MutableStateFlow(LoanCalculatorState())
    val state: StateFlow<LoanCalculatorState> = _state.asStateFlow()

    init {
        // Observe saved calculations
        viewModelScope.launch {
            repository.savedCalculations.collect { list ->
                _state.update { it.copy(savedCalculations = list) }
            }
        }
        // Run initial calculation with default parameters
        performCalculation()
        performComparison()
    }

    fun setTab(tab: LoanScreenTab) {
        _state.update { it.copy(currentTab = tab, errorMessage = null) }
    }

    fun onLoanTypeChanged(type: LoanType) {
        _state.update {
            it.copy(
                loanType = type,
                interestRateInput = if (type == LoanType.PERSONAL_LOAN) "4" else type.defaultInterest.toString(),
                interestRate = if (type == LoanType.PERSONAL_LOAN) 4.0 else type.defaultInterest,
                durationInput = type.defaultDuration.toString(),
                duration = type.defaultDuration,
                durationType = DurationType.MONTHS,
                calculationType = if (type == LoanType.PERSONAL_LOAN) CalculationMethod.CUSTOM else CalculationMethod.ANNUITY,
                errorMessage = null
            )
        }
        performCalculation()
    }

    fun onAmountInputChanged(raw: String) {
        val clean = IranianPhoneUtils.convertDigitsToEnglish(raw).filter { it.isDigit() }
        val parsed = clean.toLongOrNull() ?: 0L
        _state.update {
            it.copy(
                amountInput = clean,
                amount = parsed,
                errorMessage = null
            )
        }
        performCalculation()
    }

    fun onQuickAmountSelected(amount: Long) {
        _state.update {
            it.copy(
                amountInput = amount.toString(),
                amount = amount,
                errorMessage = null
            )
        }
        performCalculation()
    }

    fun onInterestRateInputChanged(raw: String) {
        val clean = IranianPhoneUtils.convertDigitsToEnglish(raw).replace(',', '.')
        val parsed = clean.toDoubleOrNull() ?: 0.0
        _state.update {
            it.copy(
                interestRateInput = clean,
                interestRate = parsed,
                errorMessage = null
            )
        }
        performCalculation()
    }

    fun onQuickInterestSelected(rate: Double) {
        _state.update {
            it.copy(
                interestRateInput = if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString(),
                interestRate = rate,
                errorMessage = null
            )
        }
        performCalculation()
    }

    fun onInterestPeriodChanged(period: InterestPeriod) {
        _state.update { it.copy(interestPeriod = period) }
        performCalculation()
    }

    fun onDurationInputChanged(raw: String) {
        val clean = IranianPhoneUtils.convertDigitsToEnglish(raw).filter { it.isDigit() }
        val parsed = clean.toIntOrNull() ?: 0
        _state.update {
            it.copy(
                durationInput = clean,
                duration = parsed,
                errorMessage = null
            )
        }
        performCalculation()
    }

    fun onQuickDurationSelected(months: Int) {
        _state.update {
            it.copy(
                durationInput = months.toString(),
                duration = months,
                durationType = DurationType.MONTHS,
                errorMessage = null
            )
        }
        performCalculation()
    }

    fun onDurationTypeChanged(type: DurationType) {
        _state.update { it.copy(durationType = type) }
        performCalculation()
    }

    fun onCalculationMethodChanged(method: CalculationMethod) {
        _state.update { it.copy(calculationType = method) }
        performCalculation()
    }

    fun calculate() {
        performCalculation()
    }

    private fun performCalculation() {
        val current = _state.value
        val validation = engine.validateInputs(
            amount = if (current.amountInput.isBlank()) null else current.amount,
            interestRate = if (current.interestRateInput.isBlank()) null else current.interestRate,
            duration = if (current.durationInput.isBlank()) null else current.duration
        )

        when (validation) {
            is LoanCalculatorEngine.ValidationResult.Invalid -> {
                _state.update {
                    it.copy(
                        errorMessage = validation.message,
                        result = null,
                        monthlyPayment = 0L,
                        totalPayment = 0L,
                        totalInterest = 0L,
                        realCostPercentage = 0.0
                    )
                }
            }
            is LoanCalculatorEngine.ValidationResult.Valid -> {
                val res = engine.calculate(
                    amount = current.amount,
                    interestRate = current.interestRate,
                    interestPeriod = current.interestPeriod,
                    duration = current.duration,
                    durationType = current.durationType,
                    method = current.calculationType
                )
                _state.update {
                    it.copy(
                        result = res,
                        monthlyPayment = res.monthlyPayment,
                        totalPayment = res.totalPayment,
                        totalInterest = res.totalInterest,
                        realCostPercentage = res.realCostPercentage,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun saveCalculation(customTitle: String? = null) {
        val current = _state.value
        val res = current.result ?: run {
            performCalculation()
            _state.value.result ?: return
        }

        val title = if (!customTitle.isNullOrBlank()) {
            customTitle
        } else {
            val amountFormatted = CalculationResult.formatMoney(current.amount)
            "${current.loanType.title} ($amountFormatted)"
        }

        val calculation = LoanCalculation(
            id = "calc_${UUID.randomUUID()}",
            title = title,
            loanType = current.loanType,
            amount = current.amount,
            interestRate = current.interestRate,
            duration = current.duration,
            durationType = current.durationType,
            monthlyPayment = res.monthlyPayment,
            totalPayment = res.totalPayment,
            totalInterest = res.totalInterest,
            realCostPercentage = res.realCostPercentage,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveCalculation(calculation)
            _state.update {
                it.copy(successFeedbackMessage = "محاسبه با موفقیت ذخیره شد")
            }
        }
    }

    fun deleteCalculation(id: String) {
        viewModelScope.launch {
            repository.deleteCalculation(id)
            _state.update {
                it.copy(successFeedbackMessage = "محاسبه حذف شد")
            }
        }
    }

    fun loadCalculationIntoCalculator(calculation: LoanCalculation) {
        _state.update {
            it.copy(
                currentTab = LoanScreenTab.CALCULATOR,
                loanType = calculation.loanType,
                amountInput = calculation.amount.toString(),
                amount = calculation.amount,
                interestRateInput = calculation.interestRate.toString(),
                interestRate = calculation.interestRate,
                durationInput = calculation.duration.toString(),
                duration = calculation.duration,
                durationType = calculation.durationType,
                errorMessage = null,
                successFeedbackMessage = "اطلاعات وام بارگذاری شد"
            )
        }
        performCalculation()
    }

    fun clearFeedback() {
        _state.update { it.copy(successFeedbackMessage = null, errorMessage = null) }
    }

    // Comparison methods
    fun updateCompLoan1(input: ComparisonLoanInput) {
        _state.update { it.copy(compLoan1Input = input) }
        performComparison()
    }

    fun updateCompLoan2(input: ComparisonLoanInput) {
        _state.update { it.copy(compLoan2Input = input) }
        performComparison()
    }

    private fun performComparison() {
        val l1 = _state.value.compLoan1Input
        val l2 = _state.value.compLoan2Input
        val res = engine.compareLoans(l1, l2)
        _state.update { it.copy(comparisonResult = res) }
    }
}
