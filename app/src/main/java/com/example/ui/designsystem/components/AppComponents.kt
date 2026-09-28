package com.example.ui.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.designsystem.AppColors
import com.example.ui.designsystem.AppElevation
import com.example.ui.designsystem.AppFormatters
import com.example.ui.designsystem.AppMotion
import com.example.ui.designsystem.AppRadius
import com.example.ui.designsystem.AppSpacing

// ============================================================================
// 1. GLOBAL CARD SYSTEM
// ============================================================================

/**
 * Master AppCard: Standard foundational card with multi-layered depth,
 * spring press animation, subtle ambient border highlight, and dark-mode support.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.lg),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = AppElevation.Level2,
    borderStroke: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.lg),
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = isSystemInDarkTheme()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.975f else 1.0f,
        animationSpec = AppMotion.SpringPress,
        label = "AppCardScale"
    )

    val currentElevation by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) AppElevation.Level1.value else elevation.value,
        animationSpec = AppMotion.SpringPress,
        label = "AppCardElevation"
    )

    val ambientBorder = borderStroke ?: BorderStroke(
        width = 1.dp,
        brush = AppColors.cardGlowGradient()
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .shadow(
                elevation = currentElevation.dp,
                shape = shape,
                ambientColor = AppColors.shadowAmbient(),
                spotColor = AppColors.shadowSpot()
            )
            .clip(shape)
            .background(backgroundColor)
            .border(ambientBorder, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * AppElevatedCard: High elevation card for floating summaries and critical widgets.
 */
@Composable
fun AppElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppRadius.lg),
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.lg),
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AppCard(
        modifier = modifier,
        shape = shape,
        backgroundColor = backgroundColor,
        elevation = AppElevation.Level3,
        contentPadding = contentPadding,
        testTag = testTag,
        onClick = onClick,
        content = content
    )
}

/**
 * AppHeroCard: Full brand gradient card with soft inner glow, designed for
 * primary balance cards, vehicle overview, and monthly summaries.
 */
@Composable
fun AppHeroCard(
    modifier: Modifier = Modifier,
    gradient: Brush = AppColors.EmeraldGradient,
    shape: Shape = RoundedCornerShape(AppRadius.xl),
    contentPadding: PaddingValues = PaddingValues(AppSpacing.xl),
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1.0f,
        animationSpec = AppMotion.SpringPress,
        label = "HeroCardScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .shadow(
                elevation = AppElevation.Level4,
                shape = shape,
                ambientColor = AppColors.EmeraldLight.copy(alpha = 0.35f),
                spotColor = AppColors.EmeraldDeep.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(gradient)
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.1f))
                    )
                ),
                shape = shape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * AppActionCard: Interactive row card with 3D icon, title, subtitle, optional badge,
 * and RTL-aware navigation chevron.
 */
@Composable
fun AppActionCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    vectorIcon: ImageVector? = null,
    iconAccentColor: Color = MaterialTheme.colorScheme.primary,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    testTag: String? = null,
    onClick: () -> Unit
) {
    AppCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        onClick = onClick,
        testTag = testTag
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                if (iconRes != null || vectorIcon != null) {
                    App3DIcon(
                        imageRes = iconRes,
                        vectorIcon = vectorIcon,
                        contentDescription = title,
                        accentColor = iconAccentColor,
                        size = 44.dp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (badgeText != null) {
                    StatusBadge(
                        text = badgeText,
                        status = StatusType.NEUTRAL,
                        customColor = badgeColor
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * AppInsightCard: Visual tip / insight banner with warm lightbulb icon and soft tinted container.
 */
@Composable
fun AppInsightCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    accentColor: Color = AppColors.InfoLight,
    testTag: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) accentColor.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.08f)
    val borderColor = accentColor.copy(alpha = 0.25f)

    AppCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = bgColor,
        borderStroke = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(AppSpacing.md),
        testTag = testTag
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ============================================================================
// 2. 3D ICON SYSTEM
// ============================================================================

enum class App3DSize(val dp: Dp) {
    SMALL(36.dp),
    MEDIUM(48.dp),
    LARGE(60.dp),
    HERO(76.dp)
}

/**
 * App3DIcon: Master container wrapper for rendered 3D assets or vector glyphs.
 * Guarantees uniform perspective, soft glow, layered depth, and crisp highlights.
 */
@Composable
fun App3DIcon(
    modifier: Modifier = Modifier,
    @DrawableRes imageRes: Int? = null,
    vectorIcon: ImageVector? = null,
    contentDescription: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    size: Dp = App3DSize.MEDIUM.dp,
    shape: Shape = RoundedCornerShape(AppRadius.md)
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = AppElevation.Level2,
                shape = shape,
                ambientColor = accentColor.copy(alpha = 0.3f),
                spotColor = accentColor.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = if (isDark) 0.28f else 0.16f),
                        accentColor.copy(alpha = if (isDark) 0.12f else 0.04f)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = if (isDark) 0.35f else 0.85f),
                        accentColor.copy(alpha = 0.2f)
                    )
                ),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageRes != null) {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(size * 0.92f)
                    .clip(shape),
                contentScale = ContentScale.Crop
            )
        } else if (vectorIcon != null) {
            Icon(
                imageVector = vectorIcon,
                contentDescription = contentDescription,
                tint = accentColor,
                modifier = Modifier.size(size * 0.52f)
            )
        }
    }
}

// ============================================================================
// 3. FINANCIAL AMOUNT COMPONENT
// ============================================================================

enum class AmountDisplayMode {
    LARGE,    // Big display for hero cards & account summaries
    REGULAR,  // Standard display for list items & transaction rows
    COMPACT   // Minimal inline display with "میلیون / هزار"
}

enum class AmountPolarity {
    AUTO,     // Positive green, negative red, zero neutral
    POSITIVE, // Force green
    NEGATIVE, // Force red
    NEUTRAL   // Use standard text color
}

/**
 * FinancialAmount: Highly structured financial amount display with Persian digits,
 * visual hierarchy, and distinct currency suffix.
 */
@Composable
fun FinancialAmount(
    amount: Long,
    modifier: Modifier = Modifier,
    currency: String = "تومان",
    mode: AmountDisplayMode = AmountDisplayMode.REGULAR,
    polarity: AmountPolarity = AmountPolarity.AUTO,
    showSign: Boolean = false,
    testTag: String? = null
) {
    val isDark = isSystemInDarkTheme()

    val textColor = when (polarity) {
        AmountPolarity.POSITIVE -> if (isDark) AppColors.EmeraldDark else AppColors.EmeraldDeep
        AmountPolarity.NEGATIVE -> if (isDark) AppColors.ExpenseDark else AppColors.ExpenseLight
        AmountPolarity.NEUTRAL -> MaterialTheme.colorScheme.onSurface
        AmountPolarity.AUTO -> when {
            amount > 0 -> if (isDark) AppColors.EmeraldDark else AppColors.EmeraldDeep
            amount < 0 -> if (isDark) AppColors.ExpenseDark else AppColors.ExpenseLight
            else -> MaterialTheme.colorScheme.onSurface
        }
    }

    val formattedAmount = when (mode) {
        AmountDisplayMode.COMPACT -> AppFormatters.formatCompact(amount, currency = "")
        else -> AppFormatters.formatAmount(amount, showSign = showSign)
    }

    Row(
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = formattedAmount,
            style = when (mode) {
                AmountDisplayMode.LARGE -> MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp
                )
                AmountDisplayMode.REGULAR -> MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                AmountDisplayMode.COMPACT -> MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            },
            color = textColor,
            maxLines = 1
        )

        Text(
            text = currency,
            style = when (mode) {
                AmountDisplayMode.LARGE -> MaterialTheme.typography.bodyMedium.copy(
                    color = textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                else -> MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            },
            modifier = Modifier.padding(bottom = if (mode == AmountDisplayMode.LARGE) 4.dp else 1.dp)
        )
    }
}

/**
 * PersianDateText: Standardized Persian date with optional calendar icon.
 */
@Composable
fun PersianDateText(
    date: String,
    modifier: Modifier = Modifier,
    showIcon: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showIcon) {
            Icon(
                imageVector = Icons.Rounded.CalendarToday,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = AppFormatters.toPersianDigits(date),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = color
        )
    }
}

// ============================================================================
// 4. STATUS & BADGE SYSTEM
// ============================================================================

enum class StatusType {
    SUCCESS,
    WARNING,
    DANGER,
    INFO,
    NEUTRAL
}

/**
 * StatusBadge: Multi-layered status pill with colored indicator dot, icon, and text.
 */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    status: StatusType = StatusType.NEUTRAL,
    icon: ImageVector? = null,
    customColor: Color? = null,
    testTag: String? = null
) {
    val isDark = isSystemInDarkTheme()

    val badgeColor = customColor ?: when (status) {
        StatusType.SUCCESS -> if (isDark) AppColors.EmeraldDark else AppColors.EmeraldLight
        StatusType.WARNING -> if (isDark) AppColors.WarningDark else AppColors.WarningLight
        StatusType.DANGER -> if (isDark) AppColors.ExpenseDark else AppColors.ExpenseLight
        StatusType.INFO -> if (isDark) AppColors.InfoDark else AppColors.InfoLight
        StatusType.NEUTRAL -> if (isDark) AppColors.TextSecondaryDark else AppColors.TextSecondaryLight
    }

    Surface(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .shadow(
                elevation = AppElevation.Level1,
                shape = RoundedCornerShape(AppRadius.pill),
                ambientColor = badgeColor.copy(alpha = 0.2f),
                spotColor = badgeColor.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(AppRadius.pill),
        color = badgeColor.copy(alpha = if (isDark) 0.18f else 0.12f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(12.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
            }

            Text(
                text = AppFormatters.toPersianDigits(text),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                color = badgeColor
            )
        }
    }
}

/**
 * StatusChip: Selectable/Filterable pill chip for periods, tags, and tabs.
 */
@Composable
fun StatusChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    val isDark = isSystemInDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }

    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderStroke = if (isSelected) null else BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )

    Surface(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clip(RoundedCornerShape(AppRadius.pill))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(AppRadius.pill),
        color = bgColor,
        border = borderStroke,
        shadowElevation = if (isSelected) AppElevation.Level2 else AppElevation.Level0
    ) {
        Row(
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = AppFormatters.toPersianDigits(text),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = contentColor
            )
        }
    }
}

// ============================================================================
// 5. BUTTON SYSTEM
// ============================================================================

/**
 * PrimaryButton: Standard brand-filled button with minimum 48dp touch target,
 * spring press animation, loading spinner, and icon support.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.96f else 1.0f,
        animationSpec = AppMotion.SpringPress,
        label = "PrimaryButtonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .defaultMinSize(minHeight = 48.dp)
            .shadow(
                elevation = if (enabled) AppElevation.Level2 else AppElevation.Level0,
                shape = RoundedCornerShape(AppRadius.md),
                ambientColor = AppColors.EmeraldLight.copy(alpha = 0.35f),
                spotColor = AppColors.EmeraldDeep.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(AppRadius.md))
            .background(
                if (enabled) AppColors.EmeraldGradient
                else SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            )
            .then(
                if (enabled && !isLoading) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = AppSpacing.xl, vertical = AppSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * SecondaryButton: Soft tonal button for secondary workflows.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = AppMotion.SpringPress,
        label = "SecondaryButtonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * DangerButton: Red destructive action button for deletions and resets.
 */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    testTag: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = AppMotion.SpringPress,
        label = "DangerButtonScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppColors.ExpenseLight.copy(alpha = 0.14f))
            .border(1.dp, AppColors.ExpenseLight.copy(alpha = 0.35f), RoundedCornerShape(AppRadius.md))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppColors.ExpenseLight,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = AppColors.ExpenseLight,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============================================================================
// 6. INPUT SYSTEM
// ============================================================================

/**
 * AppTextField: Master input component with standardized height, label,
 * placeholder, helper/error text, leading/trailing icons, and Persian text alignment.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    testTag: String? = null
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            shape = RoundedCornerShape(AppRadius.md),
            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.2.dp,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.md, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Start
                        ),
                        singleLine = true,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (trailingIcon != null) {
                    IconButton(
                        onClick = { onTrailingIconClick?.invoke() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = AppSpacing.xs)
            )
        }
    }
}

/**
 * AppSearchField: Standardized top search field with search icon and clear action.
 */
@Composable
fun AppSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "جستجو...",
    testTag: String = "app_search_field"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(AppRadius.lg),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = onClearQuery,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// 7. TOP APP BAR SYSTEM
// ============================================================================

/**
 * AppTopBar: Centralized Top App Bar for secondary screens with RTL-aware
 * back button, title, subtitle, and custom action buttons.
 */
@Composable
fun AppTopBar(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable (() -> Unit)? = null,
    testTag: String? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = AppElevation.Level1
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md, vertical = AppSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    actions()
                }
            }
        }
    }
}

// ============================================================================
// 8. DIALOG SYSTEM
// ============================================================================

/**
 * AppDialog: Master confirmation dialog with soft 3D icon, title, description,
 * primary action, and dismiss action.
 */
@Composable
fun AppDialog(
    isOpen: Boolean,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "تایید",
    dismissText: String = "انصراف",
    isDestructive: Boolean = false,
    @DrawableRes iconRes: Int? = null,
    vectorIcon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    if (!isOpen) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(AppSpacing.lg),
            contentAlignment = Alignment.Center
        ) {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppRadius.xl),
                contentPadding = PaddingValues(AppSpacing.xl)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
                ) {
                    if (iconRes != null || vectorIcon != null) {
                        App3DIcon(
                            imageRes = iconRes,
                            vectorIcon = vectorIcon,
                            contentDescription = title,
                            accentColor = if (isDestructive) AppColors.ExpenseLight else iconColor,
                            size = 64.dp
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                    ) {
                        SecondaryButton(
                            text = dismissText,
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )

                        if (isDestructive) {
                            DangerButton(
                                text = confirmText,
                                onClick = {
                                    onConfirm()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            PrimaryButton(
                                text = confirmText,
                                onClick = {
                                    onConfirm()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 9. EMPTY / LOADING / ERROR SYSTEM
// ============================================================================

/**
 * AppEmptyState: Universal empty state container with 3D illustration,
 * title, description, and optional call to action button.
 */
@Composable
fun AppEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    @DrawableRes illustrationRes: Int? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xl)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)
    ) {
        if (illustrationRes != null) {
            App3DIcon(
                imageRes = illustrationRes,
                contentDescription = title,
                size = 100.dp,
                shape = RoundedCornerShape(AppRadius.xl)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }

        if (actionText != null && onActionClick != null) {
            PrimaryButton(
                text = actionText,
                onClick = onActionClick
            )
        }
    }
}

/**
 * AppLoadingState: Smooth loading indicator with message.
 */
@Composable
fun AppLoadingState(
    modifier: Modifier = Modifier,
    message: String = "در حال بارگذاری اطلاعات..."
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// 10. SKELETON SHIMMER SYSTEM
// ============================================================================

/**
 * Shimmer brush for skeleton placeholders.
 */
@Composable
fun appShimmerBrush(): Brush {
    val isDark = isSystemInDarkTheme()
    val baseColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val highlightColor = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)

    val transition = rememberInfiniteTransition(label = "AppShimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AppShimmerAnim"
    )

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * SkeletonCard: Shimmer card placeholder.
 */
@Composable
fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp,
    shape: Shape = RoundedCornerShape(AppRadius.lg)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(appShimmerBrush())
    )
}

/**
 * SkeletonText: Shimmer text placeholder line.
 */
@Composable
fun SkeletonText(
    modifier: Modifier = Modifier,
    width: Dp = 120.dp,
    height: Dp = 16.dp,
    shape: Shape = RoundedCornerShape(AppRadius.xs)
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(appShimmerBrush())
    )
}
