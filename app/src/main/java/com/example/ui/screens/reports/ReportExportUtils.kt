package com.example.ui.screens.reports

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StrictMode
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class to generate real PDF documents and save high-resolution report images to the phone gallery.
 */
object ReportExportUtils {

    /**
     * Generates a real, multi-section analytical PDF document and launches the system share/view intent.
     */
    fun exportReportAsPdf(
        context: Context,
        startDate: ShamsiDate,
        endDate: ShamsiDate,
        totalIncome: String,
        totalExpense: String,
        savings: String,
        savingsPercent: String,
        topExpenseCategory: String,
        expenseRatio: String,
        previousPeriodExpense: String,
        expenseChangePercent: String,
        isExpenseIncreased: Boolean
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width in points
            val pageHeight = 842 // Standard A4 height in points
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Paints
            val bgPaint = Paint().apply {
                color = AndroidColor.parseColor("#0F172A")
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            // Header banner background
            val headerPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, pageWidth.toFloat(), 130f,
                    AndroidColor.parseColor("#0D9488"),
                    AndroidColor.parseColor("#042F2E"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 130f, headerPaint)

            // Title text
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("گزارش جامع تحلیلی مالی دارینو", (pageWidth / 2).toFloat(), 55f, titlePaint)

            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#CCFBF1")
                textSize = 13f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("مرکز تحلیل هوشمند درآمد، هزینه‌ها و تعهدات مالی", (pageWidth / 2).toFloat(), 82f, subtitlePaint)

            // Date Range Bar
            val dateBarPaint = Paint().apply {
                color = AndroidColor.parseColor("#131D33")
            }
            val dateBarRect = RectF(30f, 150f, (pageWidth - 30).toFloat(), 200f)
            canvas.drawRoundRect(dateBarRect, 14f, 14f, dateBarPaint)

            val dateTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#38BDF8")
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            val rangeText = "بازه گزارش: از ${startDate.formatDisplay()} تا ${endDate.formatDisplay()}"
            canvas.drawText(rangeText, (pageWidth - 50).toFloat(), 180f, dateTextPaint)

            val issueDatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#94A3B8")
                textSize = 11f
                textAlign = Paint.Align.LEFT
            }
            val nowStr = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
            canvas.drawText("تاریخ صدور گزارش: $nowStr", 50f, 180f, issueDatePaint)

            // 3 Summary Cards
            val cardWidth = (pageWidth - 60 - 30) / 3f
            val cardY = 220f
            val cardHeight = 100f

            fun drawSummaryBox(x: Float, title: String, amount: String, colorHex: String) {
                val boxPaint = Paint().apply {
                    color = AndroidColor.parseColor("#131D33")
                }
                val rect = RectF(x, cardY, x + cardWidth, cardY + cardHeight)
                canvas.drawRoundRect(rect, 14f, 14f, boxPaint)

                // Accent top border
                val accentPaint = Paint().apply {
                    color = AndroidColor.parseColor(colorHex)
                }
                canvas.drawRoundRect(RectF(x, cardY, x + cardWidth, cardY + 6f), 3f, 3f, accentPaint)

                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.parseColor("#94A3B8")
                    textSize = 11f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(title, x + (cardWidth / 2), cardY + 36f, labelPaint)

                val amtPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.parseColor(colorHex)
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("$amount تومان", x + (cardWidth / 2), cardY + 66f, amtPaint)
            }

            drawSummaryBox(30f, "میزان پس‌انداز", savings, "#38BDF8")
            drawSummaryBox(30f + cardWidth + 15f, "مجموع هزینه", totalExpense, "#EF4444")
            drawSummaryBox(30f + (cardWidth + 15f) * 2, "مجموع درآمد", totalIncome, "#10B981")

            // Detail Section Card
            val detailY = 345f
            val detailHeight = 220f
            val detailRect = RectF(30f, detailY, (pageWidth - 30).toFloat(), detailY + detailHeight)
            val detailPaint = Paint().apply {
                color = AndroidColor.parseColor("#131D33")
            }
            canvas.drawRoundRect(detailRect, 16f, 16f, detailPaint)

            val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("خلاصه تحلیل و ارزیابی اقتصادی", (pageWidth - 50).toFloat(), detailY + 35f, sectionTitlePaint)

            val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#E2E8F0")
                textSize = 12.5f
                textAlign = Paint.Align.RIGHT
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#2DD4BF")
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.LEFT
            }

            fun drawDetailRow(y: Float, label: String, value: String) {
                canvas.drawText(label, (pageWidth - 55).toFloat(), y, itemPaint)
                canvas.drawText(value, 55f, y, valuePaint)
            }

            drawDetailRow(detailY + 75f, "• نسبت ذخیره و پس‌انداز به درآمد:", savingsPercent)
            drawDetailRow(detailY + 110f, "• بیشترین شاخه هزینه در این دوره:", topExpenseCategory)
            drawDetailRow(detailY + 145f, "• مجموع هزینه دوره قبل:", "$previousPeriodExpense تومان")
            drawDetailRow(detailY + 180f, "• تغییر نسبت به دوره قبل:", "$expenseChangePercent ${if(isExpenseIncreased) "افزایش" else "کاهش"}")

            // Watermark / Footer
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#64748B")
                textSize = 10f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("تهیه شده توسط سامانه هوشمند دارینو (Darino) • کلیه حقوق محفوظ است", (pageWidth / 2).toFloat(), (pageHeight - 35).toFloat(), footerPaint)

            pdfDocument.finishPage(page)

            // Save PDF to cache or external files directory
            val fileName = "Darino_Financial_Report_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Safe Intent handling for PDF
            var contentUri: Uri? = null
            try {
                contentUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (e: Exception) {
                try {
                    val builder = StrictMode.VmPolicy.Builder()
                    StrictMode.setVmPolicy(builder.build())
                    contentUri = Uri.fromFile(file)
                } catch (_: Exception) {}
            }

            if (contentUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_SUBJECT, "گزارش مالی دارینو")
                    putExtra(Intent.EXTRA_TEXT, "گزارش تحلیلی مالی دارینو (${startDate.formatDisplay()} تا ${endDate.formatDisplay()})")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val chooser = Intent.createChooser(shareIntent, "ارسال یا مشاهده فایل PDF گزارش").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                Toast.makeText(context, "فایل PDF گزارش با موفقیت ایجاد شد.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "فایل در مسیر ${file.absolutePath} ذخیره شد.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در ساخت فایل PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Renders a high-resolution, beautiful financial report infographic card and saves it directly to the Phone Gallery.
     */
    fun saveReportImageToGallery(
        context: Context,
        startDate: ShamsiDate,
        endDate: ShamsiDate,
        totalIncome: String,
        totalExpense: String,
        savings: String,
        savingsPercent: String,
        topExpenseCategory: String,
        topCategoryAmount: String,
        expenseRatio: String,
        previousPeriodExpense: String,
        expenseChangePercent: String,
        isExpenseIncreased: Boolean
    ) {
        try {
            val width = 1080
            val height = 1440
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Main Background gradient
            val bgShader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                AndroidColor.parseColor("#090E17"),
                AndroidColor.parseColor("#0F172A"),
                Shader.TileMode.CLAMP
            )
            val bgPaint = Paint().apply { shader = bgShader }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Border stroke
            val borderPaint = Paint().apply {
                color = AndroidColor.parseColor("#1E293B")
                style = Paint.Style.STROKE
                strokeWidth = 6f
            }
            canvas.drawRoundRect(RectF(16f, 16f, width - 16f, height - 16f), 32f, 32f, borderPaint)

            // Header Banner
            val headerPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), 180f,
                    AndroidColor.parseColor("#0F766E"),
                    AndroidColor.parseColor("#042F2E"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(RectF(40f, 40f, width - 40f, 220f), 24f, 24f, headerPaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 40f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("مرکز تحلیل مالی دارینو", (width / 2).toFloat(), 115f, titlePaint)

            val subTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#5EEAD4")
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("گزارشات هوشمند، بودجه و تعهدات", (width / 2).toFloat(), 165f, subTitlePaint)

            // Range Pill
            val rangePillPaint = Paint().apply {
                color = AndroidColor.parseColor("#131D33")
            }
            val rangeRect = RectF(60f, 250f, width - 60f, 330f)
            canvas.drawRoundRect(rangeRect, 18f, 18f, rangePillPaint)

            val rangeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#38BDF8")
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("بازه انتخابی: از ${startDate.formatDisplay()} تا ${endDate.formatDisplay()}", (width / 2).toFloat(), 302f, rangeTextPaint)

            // 3 Big Metric Cards
            val cardY = 360f
            val cardH = 210f
            val cardW = (width - 120 - 40) / 3f

            fun drawMetricBox(x: Float, label: String, amount: String, colorHex: String) {
                val boxRect = RectF(x, cardY, x + cardW, cardY + cardH)
                val boxPaint = Paint().apply {
                    color = AndroidColor.parseColor("#131D33")
                }
                canvas.drawRoundRect(boxRect, 22f, 22f, boxPaint)

                // Top Accent Line
                val linePaint = Paint().apply {
                    color = AndroidColor.parseColor(colorHex)
                }
                canvas.drawRoundRect(RectF(x, cardY, x + cardW, cardY + 10f), 5f, 5f, linePaint)

                val lPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.parseColor("#94A3B8")
                    textSize = 22f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(label, x + (cardW / 2), cardY + 70f, lPaint)

                val aPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.parseColor(colorHex)
                    textSize = 27f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(amount, x + (cardW / 2), cardY + 130f, aPaint)

                val cPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = AndroidColor.parseColor("#64748B")
                    textSize = 19f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("تومان", x + (cardW / 2), cardY + 175f, cPaint)
            }

            drawMetricBox(60f, "میزان پس‌انداز", savings, "#38BDF8")
            drawMetricBox(60f + cardW + 20f, "مجموع هزینه", totalExpense, "#EF4444")
            drawMetricBox(60f + (cardW + 20f) * 2, "مجموع درآمد", totalIncome, "#10B981")

            // Breakdown Card
            val breakRect = RectF(60f, 600f, width - 60f, 960f)
            val breakPaint = Paint().apply {
                color = AndroidColor.parseColor("#10192D")
            }
            canvas.drawRoundRect(breakRect, 26f, 26f, breakPaint)

            val breakTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("خلاصه تحلیل و مخارج", width - 100f, 660f, breakTitlePaint)

            // Progress bar
            val barBgPaint = Paint().apply {
                color = AndroidColor.parseColor("#1E293B")
            }
            canvas.drawRoundRect(RectF(100f, 700f, width - 100f, 730f), 12f, 12f, barBgPaint)
            
            // We need a ratio float here for the bar
            val ratioFloat = try { expenseRatio.replace("٪", "").toFloat() / 100f } catch(_: Exception) { 0.5f }

            val barFillPaint = Paint().apply {
                shader = LinearGradient(
                    100f, 700f, width * ratioFloat, 730f,
                    AndroidColor.parseColor("#0284C7"),
                    AndroidColor.parseColor("#38BDF8"),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(RectF(100f, 700f, 100f + (width - 200f) * ratioFloat.coerceIn(0f, 1f), 730f), 12f, 12f, barFillPaint)

            val ratioPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#CBD5E1")
                textSize = 23f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("نسبت مصارف به درآمد کل: $expenseRatio", width - 100f, 780f, ratioPaint)

            val topExpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#CBD5E1")
                textSize = 23f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("بیشترین هزینه: $topExpenseCategory ($topCategoryAmount تومان)", width - 100f, 830f, topExpPaint)

            val compPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#CBD5E1")
                textSize = 23f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("دوره قبل: $previousPeriodExpense تومان ($expenseChangePercent ${if(isExpenseIncreased) "افزایش" else "کاهش"})", width - 100f, 880f, compPaint)

            // Darino Official Seal Card
            val sealRect = RectF(60f, 990f, width - 60f, 1250f)
            val sealPaint = Paint().apply {
                color = AndroidColor.parseColor("#064E3B")
            }
            canvas.drawRoundRect(sealRect, 24f, 24f, sealPaint)

            val sealTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#34D399")
                textSize = 28f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("تأییدیه مدیریت مالی شخصی دارینو", (width / 2).toFloat(), 1070f, sealTitlePaint)

            val sealDescPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#A7F3D0")
                textSize = 22f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("وضعیت حساب: متعادل و پایدار | بدون کسری بودجه", (width / 2).toFloat(), 1130f, sealDescPaint)
            canvas.drawText("ذخیره شده در گالری گوشی شما", (width / 2).toFloat(), 1180f, sealDescPaint)

            // Footer
            val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#64748B")
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("اپلیکیشن مدیریت مالی دارینو • ثبت و نگهداری امن در حافظه دستگاه", (width / 2).toFloat(), 1380f, footPaint)

            // Insert into MediaStore (Phone Gallery)
            val resolver = context.contentResolver
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val imageDetails = ContentValues().apply {
                val imgName = "Darino_Report_${System.currentTimeMillis()}.png"
                put(MediaStore.Images.Media.DISPLAY_NAME, imgName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Darino")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(imageCollection, imageDetails)
            if (imageUri != null) {
                val outputStream = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    imageDetails.clear()
                    imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, imageDetails, null, null)
                }

                Toast.makeText(context, "تصویر گزارش با موفقیت در گالری گوشی ذخیره شد.", Toast.LENGTH_LONG).show()

                // Offer to share saved image
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_SUBJECT, "تصویر خلاصه گزارش دارینو")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری تصویر گزارش").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } else {
                Toast.makeText(context, "خطا در ذخیره تصویر در گالری", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در ذخیره تصویر: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
