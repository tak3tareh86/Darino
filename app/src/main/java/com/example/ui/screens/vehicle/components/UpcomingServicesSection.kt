package com.example.ui.screens.vehicle.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Layered3DCard
import com.example.ui.screens.vehicle.model.ServiceReminderType
import com.example.ui.screens.vehicle.model.UpcomingServiceItem
import com.example.ui.theme.RadiusMD

@Composable
fun UpcomingServicesSection(
    services: List<UpcomingServiceItem>,
    modifier: Modifier = Modifier,
    onServiceActionClick: (UpcomingServiceItem) -> Unit = {},
    onAddNewReminderClick: () -> Unit = {},
    onSeeAllClick: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(RadiusMD))
                .clickable { isExpanded = !isExpanded },
            shape = RoundedCornerShape(RadiusMD),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "تغییر وضعیت نمایش",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "سرویس‌های پیش‌رو (${services.size} مورد)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onSeeAllClick != null) {
                        Text(
                            text = "مشاهده همه",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onSeeAllClick)
                                .testTag("see_all_services_btn")
                        )
                    }

                    Text(
                        text = "+ افزودن",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onAddNewReminderClick)
                            .testTag("add_reminder_btn")
                    )
                }
            }
        }

        if (isExpanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                services.forEach { service ->
                    ServiceReminderCard(
                        service = service,
                        onActionClick = { onServiceActionClick(service) }
                    )
                }
            }
        }
    }
}

@Composable
fun ServiceReminderCard(
    service: UpcomingServiceItem,
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val isByMileage = service.reminderType == ServiceReminderType.BY_MILEAGE

    Layered3DCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(RadiusMD),
        backgroundColor = MaterialTheme.colorScheme.surface,
        elevation = 2.dp,
        contentPadding = PaddingValues(14.dp),
        testTag = "service_reminder_${service.id}"
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Right Side: 3D Icon + Title + Type Indicator Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 3D Soft Icon
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            service.accentColor.copy(alpha = if (isDark) 0.25f else 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = service.iconRes),
                        contentDescription = service.title,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = service.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Distinctive Type Tag (Date vs Mileage)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isByMileage) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = BorderStroke(0.6.dp, if (isByMileage) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isByMileage) Icons.Rounded.Speed else Icons.Rounded.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isByMileage) Color(0xFF3B82F6) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = service.reminderType.label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isByMileage) Color(0xFF3B82F6) else Color(0xFFF59E0B)
                                )
                            }
                        }

                        Text(
                            text = service.dueText,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (service.isUrgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Left Side: Action button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onActionClick)
                    .testTag("action_service_${service.id}"),
                shape = RoundedCornerShape(10.dp),
                color = service.accentColor.copy(alpha = if (isDark) 0.2f else 0.12f),
                border = BorderStroke(1.dp, service.accentColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "ثبت انجام",
                        tint = service.accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "ثبت انجام",
                        style = MaterialTheme.typography.labelSmall,
                        color = service.accentColor
                    )
                }
            }
        }
    }
}
