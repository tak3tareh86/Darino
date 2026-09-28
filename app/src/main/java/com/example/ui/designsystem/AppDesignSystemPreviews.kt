package com.example.ui.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.designsystem.components.AmountDisplayMode
import com.example.ui.designsystem.components.AmountPolarity
import com.example.ui.designsystem.components.AppActionCard
import com.example.ui.designsystem.components.AppCard
import com.example.ui.designsystem.components.AppEmptyState
import com.example.ui.designsystem.components.AppHeroCard
import com.example.ui.designsystem.components.AppInsightCard
import com.example.ui.designsystem.components.AppSearchField
import com.example.ui.designsystem.components.AppTextField
import com.example.ui.designsystem.components.DangerButton
import com.example.ui.designsystem.components.FinancialAmount
import com.example.ui.designsystem.components.PrimaryButton
import com.example.ui.designsystem.components.SecondaryButton
import com.example.ui.designsystem.components.SkeletonCard
import com.example.ui.designsystem.components.StatusBadge
import com.example.ui.designsystem.components.StatusChip
import com.example.ui.designsystem.components.StatusType
import com.example.ui.theme.FinanceManagerTheme

@Preview(name = "Design System Components - Light", showBackground = true)
@Composable
fun AppDesignSystemLightPreview() {
    FinanceManagerTheme(darkTheme = false) {
        Surface {
            DesignSystemShowcase()
        }
    }
}

@Preview(name = "Design System Components - Dark", showBackground = true)
@Composable
fun AppDesignSystemDarkPreview() {
    FinanceManagerTheme(darkTheme = true) {
        Surface {
            DesignSystemShowcase()
        }
    }
}

@Composable
private fun DesignSystemShowcase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        // 1. Hero Card
        AppHeroCard {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                StatusBadge(text = "خلاصه کل موجودی", status = StatusType.SUCCESS)
                FinancialAmount(
                    amount = 45200000,
                    mode = AmountDisplayMode.LARGE,
                    polarity = AmountPolarity.NEUTRAL
                )
            }
        }

        // 2. Financial Amount Hierarchy
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                FinancialAmount(amount = 18500000, mode = AmountDisplayMode.LARGE, polarity = AmountPolarity.POSITIVE, showSign = true)
                FinancialAmount(amount = -4200000, mode = AmountDisplayMode.REGULAR, polarity = AmountPolarity.NEGATIVE, showSign = true)
                FinancialAmount(amount = 125000000, mode = AmountDisplayMode.COMPACT)
            }
        }

        // 3. Status Badges & Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            StatusBadge(text = "موفق", status = StatusType.SUCCESS)
            StatusBadge(text = "هشدار", status = StatusType.WARNING)
            StatusBadge(text = "بدهی", status = StatusType.DANGER)
            StatusBadge(text = "اطلاعات", status = StatusType.INFO)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            StatusChip(text = "این ماه", isSelected = true, onClick = {})
            StatusChip(text = "ماه قبل", isSelected = false, onClick = {})
            StatusChip(text = "سالانه", isSelected = false, onClick = {})
        }

        // 4. Action Card & Insight Card
        AppActionCard(
            title = "پژو ۲۰۶ تیپ ۵",
            subtitle = "کیلومتر فعلی: ۱۲۵,۰۰۰ کیلومتر",
            iconRes = R.drawable.img_3d_car,
            badgeText = "سرویس لازم",
            badgeColor = AppColors.WarningLight,
            onClick = {}
        )

        AppInsightCard(
            title = "تحلیل هوشمند ماهانه",
            description = "هزینه‌های این ماه شما نسبت به ماه گذشته ۱۲٪ کاهش یافته است."
        )

        // 5. Buttons
        PrimaryButton(
            text = "ثبت تراکنش جدید",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            SecondaryButton(
                text = "انصراف",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
            DangerButton(
                text = "حذف مورد",
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }

        // 6. Search & Inputs
        AppSearchField(
            query = "",
            onQueryChange = {},
            onClearQuery = {}
        )

        AppTextField(
            value = "۱۲,۵۰۰,۰۰۰",
            onValueChange = {},
            label = "مبلغ تراکنش",
            placeholder = "مبلغ را وارد کنید"
        )

        // 7. Skeleton
        SkeletonCard(height = 64.dp)
    }
}
