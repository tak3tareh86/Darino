package com.example.ui.screens.finance.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.finance.model.TransactionCategory
import com.example.ui.screens.finance.model.TransactionType
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.RadiusLG
import com.example.ui.theme.RadiusMD
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    categories: List<TransactionCategory>,
    onBackClick: () -> Unit,
    onSaveCategory: (TransactionCategory) -> Unit,
    onToggleCategoryActive: (String, Boolean) -> Unit,
    onDeleteCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<TransactionCategory?>(null) }

    val displayedCategories = categories.filter { it.type == selectedTab }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("category_management_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "مدیریت دسته‌بندی‌ها",
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
                onClick = {
                    editingCategory = null
                    showAddSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_add_category")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "افزودن دسته‌بندی")
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
            // Tabs for Expense / Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTab = TransactionType.EXPENSE },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == TransactionType.EXPENSE) ExpenseRoseLight else Color.Transparent
                ) {
                    Text(
                        text = "دسته‌های هزینه",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selectedTab == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (selectedTab == TransactionType.EXPENSE) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTab = TransactionType.INCOME },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTab == TransactionType.INCOME) EmeraldPrimaryLight else Color.Transparent
                ) {
                    Text(
                        text = "دسته‌های درآمد",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (selectedTab == TransactionType.INCOME) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (selectedTab == TransactionType.INCOME) Color.White else MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayedCategories, key = { it.id }) { cat ->
                    CategoryItemCard(
                        category = cat,
                        onEdit = {
                            editingCategory = cat
                            showAddSheet = true
                        },
                        onDelete = { categoryToDelete = cat },
                        onToggleActive = { onToggleCategoryActive(cat.id, it) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddEditCategorySheet(
            initialCategory = editingCategory,
            defaultType = selectedTab,
            onDismiss = { showAddSheet = false },
            onSave = { cat ->
                onSaveCategory(cat)
                showAddSheet = false
            }
        )
    }

    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("حذف دسته‌بندی") },
            text = { Text("آیا از حذف دسته‌بندی «${cat.title}» اطمینان دارید؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(cat.id)
                        categoryToDelete = null
                    }
                ) {
                    Text("حذف", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryItemCard(
    category: TransactionCategory,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(category.accentColor.copy(alpha = if (isDark) 0.2f else 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (category.iconRes != null) {
                            Image(
                                painter = painterResource(id = category.iconRes),
                                contentDescription = category.title,
                                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = category.iconEmoji, fontSize = 20.sp)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = category.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // Compact Premium Badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (category.isCustom) {
                                    if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF)
                                } else {
                                    if (isDark) Color(0xFF065F46).copy(alpha = 0.3f) else Color(0xFFECFDF5)
                                },
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = if (category.isCustom) "شخصی" else "پیش‌فرض",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (category.isCustom) Color(0xFF6366F1) else EmeraldPrimaryLight
                                )
                            }
                        }
                        
                        Text(
                            text = if (category.isActive) "فعال و در دسترس" else "غیرفعال شده",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (category.isActive) EmeraldPrimaryLight else Color(0xFF94A3B8)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Switch(
                        checked = category.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.scale(0.8f),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = category.accentColor,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                        )
                    )

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "ویرایش",
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (category.isCustom) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (isDark) Color(0xFF451A20) else Color(0xFFFEE2E2),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "حذف",
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }

            // Subcategories chips
            if (category.subCategories.isNotEmpty() && category.isActive) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    category.subCategories.forEach { sub ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = sub,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCategorySheet(
    initialCategory: TransactionCategory?,
    defaultType: TransactionType,
    onDismiss: () -> Unit,
    onSave: (TransactionCategory) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    var title by remember { mutableStateOf(initialCategory?.title ?: "") }
    var emoji by remember { mutableStateOf(initialCategory?.iconEmoji ?: "🏷️") }
    var subCategoriesText by remember { mutableStateOf(initialCategory?.subCategories?.joinToString("، ") ?: "") }
    var selectedType by remember { mutableStateOf(initialCategory?.type ?: defaultType) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = RadiusLG, topEnd = RadiusLG),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .testTag("add_category_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialCategory != null) "ویرایش دسته‌بندی" else "افزودن دسته‌بندی جدید",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Title & Emoji Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "عنوان دسته‌بندی",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusMD)
                    )
                }

                Column(modifier = Modifier.size(width = 80.dp, height = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "آیکون/ایموجی",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(RadiusMD)
                    )
                }
            }

            // Subcategories
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "زیردسته‌ها (با ویرگول جدا کنید)",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = subCategoriesText,
                    onValueChange = { subCategoriesText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("مثال: بنزین، سرویس، بیمه", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    shape = RoundedCornerShape(RadiusMD)
                )
            }

            errorMessage?.let {
                Text(text = it, style = MaterialTheme.typography.labelSmall, color = Color(0xFFEF4444))
            }

            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "لطفاً عنوان دسته‌بندی را وارد کنید."
                        return@Button
                    }
                    val subs = subCategoriesText.split("،", ",").map { it.trim() }.filter { it.isNotEmpty() }
                    val cat = TransactionCategory(
                        id = initialCategory?.id ?: UUID.randomUUID().toString(),
                        title = title,
                        type = selectedType,
                        iconEmoji = emoji.ifBlank { "🏷️" },
                        accentColor = initialCategory?.accentColor ?: Color(0xFF6366F1),
                        subCategories = subs,
                        isCustom = true,
                        isActive = initialCategory?.isActive ?: true
                    )
                    onSave(cat)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_category_btn"),
                shape = RoundedCornerShape(RadiusMD),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "ذخیره دسته‌بندی",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
