package com.example.ui.screens.installments.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.RadiusXL

@Composable
fun InstallmentsHeader(
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    val headerGradient = if (isDark) Brush.verticalGradient(listOf(Color(0xFF131D31), Color(0xFF0F172A), Color(0xFF0B2926))) else Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.primaryContainer))
    Box(modifier = modifier.fillMaxWidth()
        .shadow(4.dp, RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL), ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        .clip(RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL)).background(headerGradient).statusBarsPadding()
        .padding(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 2.dp)) {
        Row(Modifier.fillMaxWidth().height(34.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Soft3DIcon(imageRes = R.drawable.img_3d_installment, contentDescription = "اقساط", size = 28.dp, accentColor = Color(0xFF0EA5E9), containerShape = RoundedCornerShape(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text("اقساط", style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface)
                    Text("مدیریت و پیگیری تعهدات مالی", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSearchClick, modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).testTag("installments_search_btn")) {
                    Icon(Icons.Rounded.Search, contentDescription = "جستجو", tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onFilterClick, modifier = Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).testTag("installments_filter_btn")) {
                    Icon(Icons.Rounded.FilterList, contentDescription = "فیلتر", tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
