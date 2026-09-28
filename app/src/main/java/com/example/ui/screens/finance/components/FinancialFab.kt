package com.example.ui.screens.finance.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusMD

@Composable
fun FinancialFab(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 45f else 0f,
        label = "FabRotateAnim"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Speed dial menu items when expanded
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 72.dp)
        ) {
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                // Add Income Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusMD))
                        .clickable {
                            isExpanded = false
                            onAddIncome()
                        }
                        .testTag("fab_add_income")
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "ثبت درآمد جدید",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(6.dp, CircleShape, spotColor = EmeraldPrimaryLight)
                            .clip(CircleShape)
                            .background(EmeraldPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💰", fontSize = 18.sp)
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                // Add Expense Option
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(RadiusMD))
                        .clickable {
                            isExpanded = false
                            onAddExpense()
                        }
                        .testTag("fab_add_expense")
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "ثبت هزینه جدید",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(6.dp, CircleShape, spotColor = ExpenseRoseLight)
                            .clip(CircleShape)
                            .background(ExpenseRoseLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "💸", fontSize = 18.sp)
                    }
                }
            }
        }

        // Master 3D FAB Button
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFF0D9488).copy(alpha = 0.4f),
                    spotColor = Color(0xFF0D9488).copy(alpha = 0.6f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2DD4BF),
                            Color(0xFF0D9488),
                            Color(0xFF0F766E)
                        )
                    )
                )
                .clickable { isExpanded = !isExpanded }
                .testTag("finance_master_fab"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "افزودن تراکنش",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotationAngle)
            )
        }
    }
}
