package com.example.ui.screens.finance.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.finance.model.Budget
import com.example.ui.screens.finance.model.BudgetStatus
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: List<Budget>,
    onBackClick: () -> Unit,
    onAddBudgetClick: () -> Unit,
    onEditBudgetClick: (Budget) -> Unit,
    onDeleteBudgetClick: (String) -> Unit,
    onToggleBudget: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }

    val warningBudgets = budgets.filter { it.enabled && (it.status == BudgetStatus.WARNING || it.status == BudgetStatus.EXCEEDED) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("budget_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مدیریت بودجه‌ها",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBudgetClick,
                containerColor = Color(0xFF6366F1),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_budget")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "افزودن بودجه")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Warning Banner if any budget exceeded/warning
            if (warningBudgets.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(RadiusMD),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${MoneyFormatter.toPersianDigits(warningBudgets.size.toString())} بودجه به سقف هشدار یا مصرف کامل رسیده‌اند!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            ),
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "📊", fontSize = 40.sp)
                        Text(
                            text = "هنوز بودجه‌ای تعریف نکرده‌اید",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "با تعریف سقف بودجه، هزینه‌های ماهانه خود را کنترل کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(budgets, key = { it.id }) { b ->
                        BudgetDetailCard(
                            budget = b,
                            onEdit = { onEditBudgetClick(b) },
                            onDelete = { budgetToDelete = b },
                            onToggle = { onToggleBudget(b.id, it) }
                        )
                    }
                }
            }
        }
    }

    budgetToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text("حذف بودجه") },
            text = { Text("آیا از حذف بودجه «${b.title}» اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBudgetClick(b.id)
                        budgetToDelete = null
                    }
                ) {
                    Text("حذف", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun BudgetDetailCard(
    budget: Budget,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val progressAnimated by animateFloatAsState(
        targetValue = (budget.usagePercentage.coerceIn(0, 100) / 100f),
        animationSpec = tween(500),
        label = "budget_card_progress"
    )

    val progressGradient = when (budget.status) {
        BudgetStatus.SAFE -> Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF34D399)))
        BudgetStatus.WARNING -> Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
        BudgetStatus.EXCEEDED -> Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF87171)))
    }

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusLG),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 3.dp,
        contentPadding = PaddingValues(14.dp),
        testTag = "budget_card_${budget.id}",
        onClick = onEdit
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title, Status Badge, Edit, Delete, Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = budget.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = budget.status.color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, budget.status.color.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = budget.status.title,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = budget.status.color
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Edit, contentDescription = "ویرایش", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444).copy(alpha = 0.8f))
                    }
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مصرف‌شده: ${budget.formattedSpent}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${MoneyFormatter.toPersianDigits(budget.usagePercentage.toString())}٪",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = budget.status.color
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressAnimated)
                            .clip(RoundedCornerShape(4.dp))
                            .background(progressGradient)
                    )
                }
            }

            // Bottom row: Total vs Remaining
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "سقف بودجه",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = budget.formattedAmount,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "باقیمانده",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = budget.formattedRemaining,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        color = if (budget.status == BudgetStatus.EXCEEDED) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }
        }
    }
}
