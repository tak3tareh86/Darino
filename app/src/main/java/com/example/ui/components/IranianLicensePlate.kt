package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.IranianPhoneUtils

/**
 * Standard realistic Iranian vehicle license plate component:
 * - Left: Blue strip with Iranian 3-color flag and "I.R. IRAN"
 * - Center: [2 Digits] [Persian Letter] [3 Digits]
 * - Right: "ایران" on top, with [2 Digits City Code] underneath
 */
@Composable
fun IranianLicensePlate(
    plate: String,
    modifier: Modifier = Modifier,
    isSmall: Boolean = true
) {
    val englishPlate = IranianPhoneUtils.convertDigitsToEnglish(plate)
    var part1 = "" // 2 digits
    var letter = ""
    var part2 = "" // 3 digits
    var iranCode = ""
    var parseSuccess = false

    try {
        val normalized = englishPlate
            .replace("ي", "ی")
            .replace("ك", "ک")
            .trim()

        // 1. Check if plate has explicit "ایران" keyword with city code
        val iranAfterRegex = Regex("""ایران\s*(\d{2})""")
        val iranBeforeRegex = Regex("""(\d{2})\s*ایران""")

        var workingPlate = normalized
        val afterMatch = iranAfterRegex.find(workingPlate)
        val beforeMatch = iranBeforeRegex.find(workingPlate)

        if (afterMatch != null) {
            iranCode = afterMatch.groupValues[1]
            workingPlate = workingPlate.replace(afterMatch.value, " ").trim()
        } else if (beforeMatch != null) {
            iranCode = beforeMatch.groupValues[1]
            workingPlate = workingPlate.replace(beforeMatch.value, " ").trim()
        }

        // 2. Extract Persian letter
        val letterMatch = Regex("""[^\d\s\-_]""").find(workingPlate)
        if (letterMatch != null) {
            letter = letterMatch.value
            workingPlate = workingPlate.replace(letter, " ").trim()
        } else {
            letter = "ب"
        }

        // 3. Extract remaining digit groups
        val remainingDigits = Regex("""\d+""").findAll(workingPlate).map { it.value }.toList()

        if (iranCode.isNotEmpty()) {
            if (remainingDigits.size >= 2) {
                if (remainingDigits[0].length == 2 && remainingDigits[1].length == 3) {
                    part1 = remainingDigits[0]
                    part2 = remainingDigits[1]
                } else if (remainingDigits[0].length == 3 && remainingDigits[1].length == 2) {
                    part1 = remainingDigits[1]
                    part2 = remainingDigits[0]
                } else {
                    part1 = remainingDigits[0]
                    part2 = remainingDigits[1]
                }
                parseSuccess = true
            } else if (remainingDigits.size == 1) {
                val single = remainingDigits[0]
                if (single.length >= 5) {
                    part1 = single.substring(0, 2)
                    part2 = single.substring(2)
                    parseSuccess = true
                }
            }
        } else {
            // No "ایران" keyword found in raw string
            if (remainingDigits.size >= 3) {
                val threeDigitIdx = remainingDigits.indexOfFirst { it.length == 3 }
                if (threeDigitIdx != -1) {
                    part2 = remainingDigits[threeDigitIdx]
                    val otherTwo = remainingDigits.filterIndexed { idx, _ -> idx != threeDigitIdx }
                    part1 = otherTwo[0]
                    iranCode = otherTwo.getOrElse(1) { "۲۱" }
                } else {
                    part1 = remainingDigits[0]
                    part2 = remainingDigits[1]
                    iranCode = remainingDigits[2]
                }
                parseSuccess = true
            } else if (remainingDigits.size == 2) {
                part1 = remainingDigits[0]
                part2 = remainingDigits[1]
                iranCode = "۲۱"
                parseSuccess = true
            }
        }

        if (iranCode.isBlank()) {
            iranCode = "۱۱"
        }
    } catch (e: Exception) {
        parseSuccess = false
    }

    val height = if (isSmall) 34.dp else 42.dp
    val width = if (isSmall) 152.dp else 195.dp
    val fontSizeMain = if (isSmall) 14.sp else 17.sp
    val fontSizeLetter = if (isSmall) 13.sp else 16.sp
    val fontSizeCity = if (isSmall) 12.sp else 15.sp
    val fontSizeLabel = if (isSmall) 7.sp else 8.5.sp

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = modifier
                .height(height)
                .width(width),
            shape = RoundedCornerShape(4.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF1F2937))
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Blue Flag Strip (IRAN flag on the far left)
                Box(
                    modifier = Modifier
                        .width(if (isSmall) 16.dp else 20.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF003399)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Iranian 3-color flag
                        Column(
                            modifier = Modifier.size(if (isSmall) 8.dp else 10.dp, if (isSmall) 5.dp else 7.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF239f40)))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White))
                            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFff0000)))
                        }
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "I.R.",
                            color = Color.White,
                            fontSize = 5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 5.sp
                        )
                        Text(
                            text = "IRAN",
                            color = Color.White,
                            fontSize = 4.5.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 4.5.sp
                        )
                    }
                }

                if (parseSuccess) {
                    // Center section: [2 digits] [letter] [3 digits]
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // 2 Digits
                        Text(
                            text = IranianPhoneUtils.convertDigitsToPersian(part1),
                            fontSize = fontSizeMain,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )

                        // Persian Letter
                        Text(
                            text = letter,
                            fontSize = fontSizeLetter,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )

                        // 3 Digits
                        Text(
                            text = IranianPhoneUtils.convertDigitsToPersian(part2),
                            fontSize = fontSizeMain,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.25f))
                    )

                    // Right section: "ایران" on top, 2 digits city code below
                    Column(
                        modifier = Modifier
                            .width(if (isSmall) 34.dp else 42.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFF9FAFB))
                            .padding(vertical = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "ایران",
                            fontSize = fontSizeLabel,
                            color = Color(0xFF374151),
                            fontWeight = FontWeight.Bold,
                            lineHeight = fontSizeLabel
                        )
                        Text(
                            text = IranianPhoneUtils.convertDigitsToPersian(iranCode),
                            fontSize = fontSizeCity,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black,
                            lineHeight = fontSizeCity
                        )
                    }
                } else {
                    // Fallback for custom plate strings
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = IranianPhoneUtils.convertDigitsToPersian(englishPlate),
                            fontSize = if (isSmall) 11.sp else 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
