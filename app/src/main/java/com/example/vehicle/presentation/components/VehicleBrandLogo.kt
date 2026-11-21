package com.example.vehicle.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/** Offline automaker mark; custom brands use a compact text mark. */
@Composable
fun VehicleBrandLogo(brand: String, modifier: Modifier = Modifier) {
    val normalized = brand.trim().replace("‌", "").replace(" ", "").lowercase()
    val logoRes = when (normalized) {
        "هیوندای" -> R.drawable.ic_brand_hyundai
        "تویوتا" -> R.drawable.ic_brand_toyota
        "کیا" -> R.drawable.ic_brand_kia
        "بیامو", "bmw" -> R.drawable.ic_brand_bmw
        "فولکسواگن" -> R.drawable.ic_brand_volkswagen
        "نیسان" -> R.drawable.ic_brand_nissan
        "هوندا" -> R.drawable.ic_brand_honda
        "رنو" -> R.drawable.ic_brand_renault
        "سوزوکی" -> R.drawable.ic_brand_suzuki
        "میتسوبیشی" -> R.drawable.ic_brand_mitsubishi
        "مزدا" -> R.drawable.ic_brand_mazda
        "پژو" -> R.drawable.ic_brand_peugeot
        "سیتروئن" -> R.drawable.ic_brand_citroen
        "فورد" -> R.drawable.ic_brand_ford
        "آئودی" -> R.drawable.ic_brand_audi
        "شورولت" -> R.drawable.ic_brand_chevrolet
        "ولوو" -> R.drawable.ic_brand_volvo
        "پورشه" -> R.drawable.ic_brand_porsche
        "سوبارو" -> R.drawable.ic_brand_subaru
        "فیات" -> R.drawable.ic_brand_fiat
        else -> null
    }
    if (logoRes != null) {
        Image(
            painter = painterResource(id = logoRes),
            contentDescription = "لوگوی $brand",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else if (normalized == "سایر" || normalized.isBlank()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.DirectionsCar,
                contentDescription = "خودرو",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxSize(0.78f)
            )
        }
    } else {
        val mark = when (normalized) {
            "ایرانخودرو" -> "IKCO"
            "چری" -> "CHERY"
            "سایپا" -> "SAIPA"
            "مدیرانخودرو", "امویام" -> "MVM"
            "جک" -> "JAC"
            "کرمانموتور" -> "KMC"
            "فونیکس" -> "FX"
            "فیدلیتی" -> "F"
            "دیگنیتی" -> "D"
            "بهمنموتور" -> "BMC"
            "لاماری" -> "LAMARI"
            "byd" -> "BYD"
            "لکسوس" -> "LEXUS"
            "بنز" -> "MB"
            else -> brand.trim().take(5).uppercase()
        }
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = mark,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = when {
                        mark.length >= 6 -> 7.sp
                        mark.length >= 4 -> 9.sp
                        else -> 12.sp
                    },
                    maxLines = 1
                )
            }
        }
    }
}
