package com.example.ui.screens.check.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.caverock.androidsvg.SVG
import com.example.ui.screens.settings.subviews.BankCatalog
import org.json.JSONObject

private object CheckBankLogoAssets {
    private val files = listOf("bank-logos-1.json", "bank-logos-2.json", "bank-logos-3.json", "bank-logos-4.json", "bank-logos-5.json", "bank-logos-6.json")
    @Volatile private var cache: Map<String, String>? = null

    fun load(context: android.content.Context): Map<String, String> {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: buildMap {
                files.forEach { file ->
                    runCatching {
                        val json = JSONObject(context.assets.open(file).bufferedReader().use { it.readText() })
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            put(key, json.getString(key))
                        }
                    }
                }
            }.also { cache = it }
        }
    }
}

private fun checkBankSvgBitmap(source: String): Bitmap? = runCatching {
    Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).also { bitmap ->
        SVG.getFromString(source).apply {
            setDocumentWidth("256px")
            setDocumentHeight("256px")
            renderToCanvas(Canvas(bitmap))
        }
    }
}.getOrNull()

@Composable
fun CheckBankLogo(bankName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val entry = remember(bankName) { BankCatalog.find(bankName) }
    val svg = remember(entry) { entry?.let { CheckBankLogoAssets.load(context)[it.logoKey] } }
    val bitmap = remember(svg) { svg?.let(::checkBankSvgBitmap) }
    Surface(
        modifier = modifier.size(58.dp),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), contentDescription = "لوگوی ${entry?.title ?: bankName}", modifier = Modifier.size(46.dp), contentScale = ContentScale.Fit)
            } else {
                Icon(Icons.Rounded.AccountBalance, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
        }
    }
}
