package com.example.vehicle.domain

import com.example.vehicle.data.ServiceType
import com.example.vehicle.data.VehicleEntity

/**
 * Maintenance guidance resolver.
 * No universal interval is assumed; the selected vehicle's owner's manual is authoritative.
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

    fun resolve(vehicle: VehicleEntity, serviceType: ServiceType): VehicleServiceInterval {
        val vehicleName = "${vehicle.brand} ${vehicle.model}"
        val guidance = when (serviceType) {
            ServiceType.OIL_CHANGE ->
                "نوع و گرانروی روغن، شرایط کارکرد و بازهٔ تعویض را از دفترچهٔ راهنمای $vehicleName بررسی کنید."
            ServiceType.FILTERS ->
                "فیلتر روغن، هوا و کابین ممکن است بازه‌های متفاوتی داشته باشند؛ نوع فیلتر و زمان تعویض را از دفترچهٔ همین خودرو بررسی کنید."
            ServiceType.FUEL_FILTER_CHANGE ->
                "بازهٔ تعویض فیلتر بنزین به موتور و سیستم سوخت‌رسانی وابسته است؛ دستورالعمل همین خودرو را ملاک قرار دهید."
            ServiceType.BRAKE_PADS ->
                "لنت ترمز بر اساس میزان سایش و مشخصات محور بررسی می‌شود؛ بازدید فنی و دستورالعمل سازندهٔ همین خودرو ملاک است."
            ServiceType.CLUTCH_DISC_PLATE_CHANGE ->
                "دیسک و صفحه کلاچ عمر ثابت و یکسانی ندارند؛ علائم لغزش و وضعیت فنی را بررسی و دفترچهٔ همین خودرو را ملاک قرار دهید."
            ServiceType.SPARK_PLUG_WIRE_CHANGE ->
                "نوع شمع، موتور و ساختار سیستم جرقه‌زنی (وجود یا نبود وایر) تعیین‌کننده است؛ دفترچهٔ همین خودرو را بررسی کنید."
            ServiceType.ENGINE_TUNE ->
                "زمان بازدید و تنظیم موتور به کد موتور و سیستم سوخت‌رسانی وابسته است؛ دفترچهٔ همین خودرو را ملاک قرار دهید."
            ServiceType.TIRE_CHANGE ->
                "تعویض لاستیک بر اساس سایش، سن، آسیب و مشخصات سازنده انجام می‌شود؛ بازدید منظم و دفترچهٔ خودرو ملاک است."
            ServiceType.BATTERY ->
                "باتری موعد تعویض ثابتی ندارد؛ نتیجهٔ تست سلامت، مشخصات باتری و توصیهٔ سازندهٔ همین خودرو را ملاک قرار دهید."
            ServiceType.TRANSMISSION_OIL_CHANGE ->
                "نوع گیربکس و استاندارد روغن تعیین‌کننده‌اند؛ فقط مشخصات و بازهٔ درج‌شده در دفترچهٔ همین خودرو معتبر است."
            ServiceType.HYDRAULIC_STEERING_OIL_CHANGE ->
                "این سرویس فقط برای سیستم فرمان هیدرولیک کاربرد دارد؛ ابتدا نوع سیستم فرمان و روغن مجاز را در دفترچه بررسی کنید."
            ServiceType.BRAKE_FLUID_CHANGE ->
                "استاندارد روغن ترمز و بازهٔ تعویض را از دفترچهٔ راهنمای همین خودرو بررسی کنید."
            ServiceType.ALTERNATOR_BELT_CHANGE ->
                "وضعیت و بازهٔ سرویس تسمه دینام به موتور و نوع تسمه وابسته است؛ دفترچهٔ همین خودرو را ملاک قرار دهید."
            ServiceType.TIMING_BELT_CHANGE ->
                "بازهٔ تسمه تایم به کد موتور وابسته است؛ برای جلوگیری از آسیب موتور فقط دستورالعمل همان موتور را ملاک قرار دهید."
            ServiceType.COOLANT_CHANGE ->
                "نوع مایع خنک‌کننده، استاندارد و بازهٔ تعویض را از دفترچهٔ همین خودرو بررسی کنید."
            ServiceType.CUSTOM ->
                "برای سرویس سفارشی فاصلهٔ ثابت قابل تعمیم وجود ندارد؛ بازه را طبق دفترچهٔ خودرو یا نظر متخصص وارد کنید."
        }

        return VehicleServiceInterval(
            intervalKm = 0,
            intervalMonths = 0,
            source = guidance,
            manufacturerSpecific = false
        )
    }
}
