package com.example.vehicle.presentation.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caverock.androidsvg.SVG

private data class VehicleBrandAsset(
    val fileName: String,
    val darkBackground: Boolean = false,
    val saipaWordmark: Boolean = false
)

/**
 * Displays the bundled manufacturer mark for the selected vehicle brand.
 * Unknown/custom brands deliberately use a neutral car icon rather than a
 * fabricated text-based logo.
 */
@Composable
fun VehicleBrandLogo(brand: String, modifier: Modifier = Modifier) {
    val normalized = brand.trim()
        .replace("‌", "")
        .replace(" ", "")
        .lowercase()

    val asset = when (normalized) {
        "ایرانخودرو" -> VehicleBrandAsset("iran-khodro.svg")
        "سایپا" -> VehicleBrandAsset("saipa.svg", saipaWordmark = true)
        "مدیرانخودرو", "امویام" -> VehicleBrandAsset("mvm.png", darkBackground = true)
        "کرمانموتور" -> VehicleBrandAsset("kerman-motor.svg")
        "بهمنموتور", "فیدلیتی", "دیگنیتی" -> VehicleBrandAsset("bahman.svg")
        "چری" -> VehicleBrandAsset("chery-logo.png")
        "جک" -> VehicleBrandAsset("jac-logo.png")
        "فونیکس" -> VehicleBrandAsset("fownix.png", darkBackground = true)
        "لاماری" -> VehicleBrandAsset("lamari.png", darkBackground = true)
        "byd" -> VehicleBrandAsset("byd-logo.svg")
        "هیوندای" -> VehicleBrandAsset("hyundai-logo.svg")
        "کیا" -> VehicleBrandAsset("kia-logo.svg")
        "تویوتا" -> VehicleBrandAsset("toyota-logo.svg")
        "نیسان" -> VehicleBrandAsset("nissan-logo.svg")
        "مزدا" -> VehicleBrandAsset("mazda-logo.svg")
        "هوندا" -> VehicleBrandAsset("honda-logo.png")
        "رنو" -> VehicleBrandAsset("renault-logo.svg")
        "سوزوکی" -> VehicleBrandAsset("suzuki-logo.svg")
        "میتسوبیشی" -> VehicleBrandAsset("mitsubishi-logo.svg")
        "لکسوس" -> VehicleBrandAsset("lexus-logo.png")
        "بنز" -> VehicleBrandAsset("mercedes-benz-logo.svg")
        "بیامو", "bmw" -> VehicleBrandAsset("bmw-logo.svg")
        "فولکسواگن" -> VehicleBrandAsset("volkswagen-logo.svg")
        else -> null
    }

    val context = LocalContext.current
    val bitmap = remember(asset?.fileName) {
        asset?.let { loadVehicleBrandBitmap(context, it.fileName) }
    }

    if (asset == null || bitmap == null) {
        Icon(
            imageVector = Icons.Rounded.DirectionsCar,
            contentDescription = if (brand.isBlank() || normalized == "سایر") "خودرو" else "نشان برند $brand در دسترس نیست",
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    } else if (asset.saipaWordmark) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "SAIPA",
                color = Color(0xFF001278),
                fontSize = 6.sp,
                lineHeight = 7.sp,
                letterSpacing = 0.7.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    } else if (asset.darkBackground) {
        Surface(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF263449)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "لوگوی $brand",
                modifier = Modifier.fillMaxSize().padding(3.dp),
                contentScale = ContentScale.Fit
            )
        }
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "لوگوی $brand",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}

private fun loadVehicleBrandBitmap(context: Context, fileName: String): Bitmap? =
    runCatching {
        if (fileName.endsWith(".svg", ignoreCase = true)) {
            val svg = SVG.getFromAsset(context.assets, "vehicle_brand_logos/$fileName")
            val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            svg.setDocumentWidth(256f)
            svg.setDocumentHeight(256f)
            svg.renderToCanvas(Canvas(bitmap))
            bitmap
        } else {
            context.assets.open("vehicle_brand_logos/$fileName").use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
    }.getOrNull()
