package com.example.ui.screens.settings.subviews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.settings.components.SettingsConfirmationDialog
import com.example.ui.screens.settings.components.SettingsEmptyState
import com.example.ui.screens.settings.components.SettingsHeader
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.viewmodel.FinancialViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.ButtonShape
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import com.example.ui.theme.RadiusSM

@Composable
fun CategoriesScreen(
    onBackClick: () -> Unit,
    viewModel: FinancialViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val expenseCategories = state.categories.filter { it.type == TransactionType.EXPENSE }
    val incomeCategories = state.categories.filter { it.type == TransactionType.INCOME }

    var isAddSheetOpen by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<com.example.ui.screens.finance.model.TransactionCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<com.example.ui.screens.finance.model.TransactionCategory?>(null) }

    val currentList = if (selectedTabIndex == 0) expenseCategories else incomeCategories

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            SettingsHeader(
                title = "مدیریت دسته‌بندی‌ها",
                subtitle = "دسته‌های هزینه‌ها و درآمدهای شخصی",
                showBack = true,
                showSearch = false,
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCategory = null
                    isAddSheetOpen = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(RadiusMD),
                modifier = Modifier.testTag("add_category_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "افزودن دسته‌بندی",
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "افزودن دسته",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "هزینه‌ها (${expenseCategories.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "درآمدها (${incomeCategories.size})",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                )
            }

            if (currentList.isEmpty()) {
                SettingsEmptyState(
                    title = "دسته‌بندی وجود ندارد",
                    description = "برای دسته‌بندی بهتر تراکنش‌ها، دسته‌های سفارشی خود را اضافه کنید.",
                    iconEmoji = if (selectedTabIndex == 0) "🛍️" else "💰",
                    actionButtonText = "افزودن دسته جدید",
                    onActionClick = {
                        editingCategory = null
                        isAddSheetOpen = true
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentList, key = { it.id }) { cat ->
                        CategoryManageCard(
                            category = cat,
                            onEdit = {
                                editingCategory = cat
                                isAddSheetOpen = true
                            },
                            onDelete = {
                                categoryToDelete = cat
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Category Sheet
    if (isAddSheetOpen) {
        AddEditCategorySheet(
            category = editingCategory,
            isExpense = selectedTabIndex == 0,
            onDismiss = { isAddSheetOpen = false },
            onSave = { savedCategory ->
                if (editingCategory != null) {
                    viewModel.updateCategory(savedCategory)
                } else {
                    viewModel.addCategory(savedCategory)
                }
                isAddSheetOpen = false
            }
        )
    }

    // Confirmation Dialog
    SettingsConfirmationDialog(
        isOpen = categoryToDelete != null,
        title = "حذف دسته‌بندی ${categoryToDelete?.title ?: ""}",
        message = "آیا از حذف این دسته‌بندی مطمئن هستید؟",
        confirmButtonText = "حذف دسته",
        onConfirm = {
            categoryToDelete?.let {
                viewModel.deleteCategory(it.id)
            }
            categoryToDelete = null
        },
        onDismiss = { categoryToDelete = null }
    )
}

@Composable
fun CategoryManageCard(
    category: com.example.ui.screens.finance.model.TransactionCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 4.dp else 1.5.dp,
                shape = RoundedCornerShape(RadiusMD)
            ),
        shape = RoundedCornerShape(RadiusMD),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.03f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(RadiusSM))
                        .background(category.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (category.iconRes != null) {
                        Image(
                            painter = painterResource(id = category.iconRes),
                            contentDescription = category.title,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(text = category.iconEmoji, fontSize = 20.sp)
                    }
                }

                Column {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (category.subCategories.isNotEmpty()) {
                        Text(
                            text = category.subCategories.joinToString("، "),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Edit and Delete Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "ویرایش",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "حذف",
                        tint = ExpenseRoseLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategorySheet(
    category: com.example.ui.screens.finance.model.TransactionCategory?,
    isExpense: Boolean,
    onDismiss: () -> Unit,
    onSave: (com.example.ui.screens.finance.model.TransactionCategory) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val availableEmojis = listOf("🍔", "🚗", "🏠", "🛒", "💊", "🎓", "🎮", "💳", "🎁", "✈️", "☕", "📱")
    val availableColors = listOf(
        Color(0xFFF97316), Color(0xFF3B82F6), Color(0xFF8B5CF6),
        Color(0xFF10B981), Color(0xFFEC4899), Color(0xFF06B6D4),
        Color(0xFFF59E0B), Color(0xFF6366F1), Color(0xFF14B8A6)
    )

    var title by remember { mutableStateOf(category?.title ?: "") }
    var selectedEmoji by remember { mutableStateOf(category?.iconEmoji ?: "🛍️") }
    var selectedColor by remember { mutableStateOf(category?.accentColor ?: Color(0xFFF97316)) }

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
                text = if (category != null) "ویرایش دسته‌بندی" else "افزودن دسته‌بندی جدید",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Icon Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "انتخاب آیکون",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(availableEmojis) { emoji ->
                        val isSelected = emoji == selectedEmoji
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(RadiusSM))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }

            // Color Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "رنگ برچسب",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(availableColors) { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("نام دسته‌بندی") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(RadiusMD)
            )

            Button(
                onClick = {
                    val saved = com.example.ui.screens.finance.model.TransactionCategory(
                        id = category?.id ?: "cat_${System.currentTimeMillis()}",
                        title = title.ifBlank { "دسته جدید" },
                        iconEmoji = selectedEmoji,
                        accentColor = selectedColor,
                        type = if (isExpense) com.example.ui.screens.finance.model.TransactionType.EXPENSE else com.example.ui.screens.finance.model.TransactionType.INCOME,
                        isDefault = category?.isDefault ?: false,
                        isActive = category?.isActive ?: true
                    )
                    onSave(saved)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_category_button"),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "ذخیره دسته‌بندی",
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
