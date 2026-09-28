package com.example.reports.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.reports.domain.ReportPeriod
import com.example.reports.domain.ReportTab
import com.example.reports.presentation.components.InsightCard
import com.example.reports.presentation.components.ReportExportSheet
import com.example.reports.presentation.components.ReportFilterSheet
import com.example.reports.presentation.components.ReportSummaryHeaderCard
import com.example.reports.viewmodel.ReportsViewModel
import com.example.ui.components.Layered3DCard
import com.example.ui.components.Soft3DIcon
import com.example.util.IranianPhoneUtils

/**
 * Main Hub: Darino Financial Analysis Center (مرکز تحلیل مالی دارینو)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsDashboardScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToFinancial: () -> Unit = {},
    onNavigateToInstallments: () -> Unit = {},
    onNavigateToVehicles: () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: ReportsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.exportSuccessMessage) {
        state.exportSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_dashboard_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Header Row: Title + Filter + Export
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_analytics,
                        contentDescription = "مرکز تحلیل مالی دارینو",
                        size = 40.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "مرکز تحلیل مالی دارینو",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "گزارشات هوشمند، بودجه و تعهدات",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Buttons (Filter & Share/Export)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.openFilterSheet() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("filter_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "فیلترها",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { viewModel.openExportSheet() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .testTag("export_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.IosShare,
                            contentDescription = "خروجی و اشتراک",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Summary Card
            if (state.summary != null) {
                ReportSummaryHeaderCard(summary = state.summary!!)
            }

            // 3. Navigation Tabs (4 Sections)
            ReportsTabRow(
                selectedTab = state.activeTab,
                onTabSelected = { viewModel.selectTab(it) }
            )

            // 4. Tab Content with Loading / Crossfade
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (state.summary != null) {
                val summary = state.summary!!

                Crossfade(
                    targetState = state.activeTab,
                    label = "ReportsTabCrossfade",
                    modifier = Modifier.weight(1f)
                ) { targetTab ->
                    when (targetTab) {
                        ReportTab.FINANCIAL -> {
                            FinancialReportScreen(
                                summary = summary,
                                selectedPeriod = state.selectedPeriod,
                                onPeriodSelected = { viewModel.selectPeriod(it) }
                            )
                        }
                        ReportTab.INSTALLMENTS -> {
                            InstallmentReportScreen(
                                installmentSummary = summary.installmentSummary
                            )
                        }
                        ReportTab.VEHICLE -> {
                            VehicleReportScreen(
                                vehicleSummary = summary.vehicleSummary
                            )
                        }
                        ReportTab.INSIGHTS -> {
                            DarinoInsightsTabContent(
                                insights = summary.insights
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (state.showFilterSheet) {
        ReportFilterSheet(
            currentFilter = state.filter,
            onApplyFilter = { viewModel.applyFilter(it) },
            onDismiss = { viewModel.closeFilterSheet() }
        )
    }

    // Export Bottom Sheet
    if (state.showExportSheet) {
        ReportExportSheet(
            summary = state.summary,
            onExportPdf = { viewModel.exportPdfMock() },
            onExportImage = { viewModel.exportImageMock() },
            onDismiss = { viewModel.closeExportSheet() }
        )
    }
}

/**
 * 4-Tab Navigation Row
 */
@Composable
private fun ReportsTabRow(
    selectedTab: ReportTab,
    onTabSelected: (ReportTab) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReportTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                val primaryColor = MaterialTheme.colorScheme.primary

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onTabSelected(tab) }
                        .testTag("report_tab_${tab.name}"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) primaryColor else Color.Transparent,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Darino Smart Insights Tab View
 */
@Composable
private fun DarinoInsightsTabContent(
    insights: List<com.example.reports.domain.FinancialInsight>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("darino_insights_tab"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Layered3DCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 3.dp,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Soft3DIcon(
                        imageRes = R.drawable.img_3d_analytics,
                        contentDescription = "تحلیل‌های هوشمند دارینو",
                        size = 36.dp,
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "تحلیل‌های هوشمند دارینو (Darino Insights)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تحلیل داده‌محور بر پایه قوانین بودجه‌بندی و تعهدات مالی",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "توصیه‌ها و هشدارهای این دوره",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${IranianPhoneUtils.convertDigitsToPersian(insights.size.toString())} مورد شناسایی شد",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(insights, key = { it.id }) { insight ->
            InsightCard(insight = insight)
        }
    }
}
