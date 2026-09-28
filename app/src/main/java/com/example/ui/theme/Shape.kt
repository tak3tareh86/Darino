package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RadiusXS = 6.dp
val RadiusSM = 10.dp
val RadiusMD = 16.dp
val RadiusLG = 24.dp
val RadiusXL = 32.dp
val RadiusFull = 100.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(RadiusXS),
    small = RoundedCornerShape(RadiusSM),
    medium = RoundedCornerShape(RadiusMD),
    large = RoundedCornerShape(RadiusLG),
    extraLarge = RoundedCornerShape(RadiusXL)
)

val CardShape = RoundedCornerShape(RadiusLG)
val GridCardShape = RoundedCornerShape(RadiusMD)
val ButtonShape = RoundedCornerShape(RadiusMD)
val ChipShape = RoundedCornerShape(RadiusFull)
val HeaderShape = RoundedCornerShape(bottomStart = RadiusXL, bottomEnd = RadiusXL)
val BottomNavShape = RoundedCornerShape(RadiusXL)
