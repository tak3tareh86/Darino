package com.example.ui.screens.installments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.AppSectionHeader
import com.example.ui.components.Soft3DIcon

@Composable
fun InstallmentsHeader(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    AppSectionHeader(
        title = "اقساط",
        subtitle = "مدیریت و پیگیری تعهدات مالی",
        modifier = modifier,
        leadingIcon = {
            Soft3DIcon(
                imageRes = R.drawable.img_3d_installment,
                contentDescription = "اقساط",
                size = 28.dp,
                accentColor = Color(0xFF0EA5E9),
                containerShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("installments_search_btn")
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = "جستجو",
                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .testTag("installments_filter_btn")
            ) {
                Icon(
                    Icons.Rounded.FilterList,
                    contentDescription = "فیلتر",
                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    )
}
