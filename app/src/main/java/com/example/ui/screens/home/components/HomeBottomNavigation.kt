package com.example.ui.screens.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Soft3DIcon
import com.example.ui.theme.RadiusXL

enum class BottomNavItem(
    val id: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val image3dRes: Int
) {
    HOME("home", "خانه", Icons.Rounded.Home, Icons.Outlined.Home, R.drawable.img_3d_home),
    FINANCE("finance", "مالی", Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, R.drawable.img_3d_wallet),
    CALENDAR("calendar", "تقویم", Icons.Rounded.CalendarMonth, Icons.Outlined.CalendarMonth, R.drawable.img_3d_calendar),
    INSTALLMENTS("installments", "اقساط", Icons.Rounded.AccountBalance, Icons.Outlined.AccountBalance, R.drawable.img_3d_installment),
    VEHICLE("vehicle", "خودرو", Icons.Rounded.DirectionsCar, Icons.Outlined.DirectionsCar, R.drawable.img_3d_car),
    REMINDERS("reminders", "یادآور", Icons.Rounded.Notifications, Icons.Outlined.Notifications, R.drawable.img_3d_bell_notification),
    REPORTS("reports", "گزارشات", Icons.Rounded.Assessment, Icons.Outlined.Assessment, R.drawable.img_3d_chart)
}

@Composable
fun HomeBottomNavigation(
    selectedItem: BottomNavItem = BottomNavItem.HOME,
    onItemSelected: (BottomNavItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(RadiusXL),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0xFF0F172A).copy(alpha = 0.1f),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.7f) else Color(0xFF0F172A).copy(alpha = 0.15f)
                ),
            shape = RoundedCornerShape(RadiusXL),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.9f),
                        if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem.entries.forEach { item ->
                    val isSelected = item == selectedItem
                    BottomNavTabItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemSelected(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavTabItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.04f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "TabScale"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "TabColor"
    )

    Box(
        modifier = modifier
            .testTag("nav_item_${item.id}")
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.12f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.10f else 0.04f)
                                )
                            )
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                } else {
                    Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (isSelected) {
                Soft3DIcon(
                    imageRes = item.image3dRes,
                    contentDescription = item.title,
                    size = 24.dp,
                    containerShape = RoundedCornerShape(8.dp)
                )
            } else {
                Icon(
                    imageVector = item.unselectedIcon,
                    contentDescription = item.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = item.title,
                style = if (isSelected) {
                    MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp)
                } else {
                    MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp)
                },
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
