package com.example.vehicle.domain

import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleEntity

/**
 * Vehicle-specific maintenance interval resolver.
 *
 * Intervals are only considered manufacturer-specific when the vehicle profile matches
 * a documented application rule. Unknown vehicle variants must not inherit TARA intervals.
 */
data class VehicleServiceInterval(
    val intervalKm: Int,
    val intervalMonths: Int,
    val source: String,
    val manufacturerSpecific: Boolean
) {
    val hasMileageInterval: Boolean get() = intervalKm > 0
    val hasTimeInterval: Boolean get() = intervalMonths > 0
    val isUsable: Boolean get() = hasMileageInterval || hasTimeInterval
}

object VehicleMaintenanceCatalog {

    private const val CONSERVATIVE_OIL_KM = 5_000
    private const val CONSERVATIVE_OIL_MONTHS = 3
    private const val INSPECTION_KM = 10_000
    private const val INSPECTION_MONTHS = 6

    fun resolve(vehicle: VehicleEntity, serviceType: ServiceType): VehicleServiceInterval {
        val identity = normalize("${vehicle.brand} ${vehicle.model} ${vehicle.engine} ${vehicle.transmission}")
        val isTara = identity.contains("تارا") || identity.contains("tara")
        val isAutomaticTara = isTara && listOf("اتومات", "automatic", "auto", "dae").any { token -> identity.contains(token) }

        return when (serviceType) {
            // Conservative app default requested for engine oil. The exact owner-manual,
            // oil specification and severe-use schedule always take precedence.
            ServiceType.OIL_CHANGE -> VehicleServiceInterval(
                CONSERVATIVE_OIL_KM,
                CONSERVATIVE_OIL_MONTHS,
                "پیش‌فرض محافظه‌کارانه دارینو: هر ۵٬۰۰۰ کیلومتر یا ۳ ماه؛ دفترچهٔ همان خودرو، مشخصات روغن و شرایط کارکرد اولویت دارد. این عدد الزام یکسان سازنده برای همهٔ خودروها نیست.",
                false
            )

            // A combined air/oil-filter entry cannot safely share one universal interval:
            // the oil filter usually follows the oil service, while the air filter varies.
            ServiceType.FILTERS -> unknown(
                "بازهٔ فیلتر روغن و فیلتر هوا به مدل، موتور و شرایط گردوغبار وابسته است؛ بازهٔ دفترچه را وارد کنید."
            )

            // These are inspections, not automatic replacement intervals.
            ServiceType.BRAKE_PADS -> VehicleServiceInterval(
                INSPECTION_KM, INSPECTION_MONTHS,
                "بازهٔ پیشنهادی محافظه‌کارانه برای بازدید است، نه تعویض قطعه؛ میزان فرسودگی و دستور سازنده ملاک است.",
                false
            )
            ServiceType.TIRE_CHANGE -> VehicleServiceInterval(
                INSPECTION_KM, INSPECTION_MONTHS,
                "بازهٔ پیشنهادی بازدید لاستیک است، نه عمر یا موعد قطعی تعویض؛ فشار، سایش، سن و دستور سازنده ملاک‌اند.",
                false
            )
            ServiceType.BATTERY -> VehicleServiceInterval(
                INSPECTION_KM, INSPECTION_MONTHS,
                "بازهٔ پیشنهادی بازدید سلامت باتری است، نه موعد قطعی تعویض؛ آزمون باتری و توصیهٔ سازنده ملاک‌اند.",
                false
            )

            // Published TARA product/warranty schedule. It is intentionally not applied
            // to other models, engines or transmissions.
            ServiceType.BRAKE_FLUID_CHANGE -> if (isTara) VehicleServiceInterval(
                40_000, 24,
                "راهنمای محصول و گارانتی تارا: هر ۴۰٬۰۰۰ کیلومتر یا ۲ سال، هرکدام زودتر برسد. برای مدل دیگر دفترچهٔ اختصاصی لازم است.",
                true
            ) else unknown("برای این مدل، بازهٔ مستند روغن ترمز در بانک دارینو موجود نیست؛ طبق دفترچهٔ اختصاصی خودرو وارد کنید.")

            ServiceType.ALTERNATOR_BELT_CHANGE -> if (isTara) VehicleServiceInterval(
                80_000, 36,
                "راهنمای محصول و گارانتی تارا: هر ۸۰٬۰۰۰ کیلومتر یا ۳ سال. این بازه برای موتورهای دیگر قابل تعمیم نیست.",
                true
            ) else unknown("بازهٔ تسمه دینام به موتور و نسخهٔ خودرو وابسته است؛ طبق دفترچهٔ اختصاصی وارد کنید.")

            ServiceType.TIMING_BELT_CHANGE -> if (isTara) VehicleServiceInterval(
                80_000, 36,
                "راهنمای محصول و گارانتی تارا: هر ۸۰٬۰۰۰ کیلومتر یا ۳ سال. بازهٔ موتورهای دیگر ممکن است متفاوت باشد.",
                true
            ) else unknown("بازهٔ تسمه تایم به کد موتور وابسته است؛ برای جلوگیری از آسیب موتور، فقط بازهٔ دفترچهٔ همان موتور را وارد کنید.")

            ServiceType.COOLANT_CHANGE -> if (isTara) VehicleServiceInterval(
                0, 24,
                "راهنمای محصول و گارانتی تارا: هر ۲ سال؛ در این مرجع فاصلهٔ کیلومتری جداگانه تعیین نشده است.",
                true
            ) else unknown("نوع مایع خنک‌کننده و فاصلهٔ تعویض به مدل و موتور وابسته است؛ طبق دفترچهٔ اختصاصی وارد کنید.")

            ServiceType.TRANSMISSION_OIL_CHANGE -> if (isAutomaticTara) VehicleServiceInterval(
                60_000, 60,
                "راهنمای محصول و گارانتی تارا با گیربکس اتوماتیک DAE شش‌سرعته: هر ۶۰٬۰۰۰ کیلومتر یا ۵ سال. برای گیربکس دیگر این بازه معتبر فرض نمی‌شود.",
                true
            ) else unknown(
                if (isTara) "نوع گیربکس تارا در اطلاعات ثبت‌شده مشخص نیست. فقط اگر گیربکس DAE شش‌سرعتهٔ اتوماتیک است، بازهٔ ۶۰٬۰۰۰ کیلومتر/۵ سالِ راهنمای تارا قابل استفاده است؛ در غیر این صورت دفترچه را ملاک قرار دهید."
                else "نوع گیربکس و مشخصات روغن تعیین‌کننده‌اند؛ بازهٔ مستند این مدل در بانک دارینو موجود نیست."
            )

            ServiceType.HYDRAULIC_STEERING_OIL_CHANGE -> unknown(
                "این سرویس فقط برای فرمان هیدرولیک کاربرد دارد؛ بسیاری از خودروها فرمان برقی دارند و روغن هیدرولیک فرمان ندارند. نوع سیستم و دفترچه را بررسی کنید."
            )

            ServiceType.ENGINE_TUNE -> unknown(
                "زمان بازدید شمع‌ها و سیستم موتور به کد موتور، نوع شمع و سوخت وابسته است؛ بازهٔ دفترچهٔ اختصاصی را وارد کنید."
            )

            ServiceType.CUSTOM -> unknown(
                "برای سرویس سفارشی فاصلهٔ ثابت جهانی وجود ندارد؛ بازه را طبق دفترچه یا نظر متخصص وارد کنید."
            )
        }
    }

    private fun unknown(source: String) = VehicleServiceInterval(
        intervalKm = 0,
        intervalMonths = 0,
        source = source,
        manufacturerSpecific = false
    )

    private fun normalize(value: String): String = value
        .lowercase()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace('۰', '0').replace('۱', '1').replace('۲', '2')
        .replace('۳', '3').replace('۴', '4').replace('۵', '5')
        .replace('۶', '6').replace('۷', '7').replace('۸', '8').replace('۹', '9')
        .replace(Regex("[\\s_\\-]+"), "")
}
