package com.example.ui.screens.settings.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.EmeraldPrimaryLight
import com.example.ui.theme.ExpenseRoseLight
import com.example.ui.theme.InfoIndigoLight
import com.example.ui.theme.WarningAmberLight

enum class AppThemeMode(val title: String, val description: String) {
    SYSTEM("سیستم", "پیروی خودکار از حالت سیستم عامل"),
    LIGHT("روشن", "تم روشن با کنتراست بالا و پس‌زمینه شفاف"),
    DARK("تاریک", "تم تاریک برای راحتی چشم در نور کم")
}

enum class AppLanguage(val title: String, val nativeName: String, val code: String) {
    PERSIAN("فارسی", "فارسی (ایران)", "fa"),
    ENGLISH("انگلیسی", "English (US)", "en")
}

enum class AppCurrency(val title: String, val symbol: String, val code: String) {
    TOMAN("تومان", "تومان", "IRT"),
    RIAL("ریال", "ریال", "IRR"),
    USD("دلار", "$", "USD"),
    EUR("یورو", "€", "EUR")
}

enum class AppCalendar(val title: String, val description: String) {
    SHAMSI("شمسی (هجری خورشیدی)", "تقویم پیش‌فرض رسمی ایران"),
    GREGORIAN("میلادی", "تقویم بین‌المللی میلادی")
}

enum class WeekStartDay(val title: String) {
    SATURDAY("شنبه (پیش‌فرض ایران)"),
    SUNDAY("یکشنبه"),
    MONDAY("دوشنبه")
}

enum class AppLockType(val title: String, val description: String) {
    NONE("بدون قفل", "ورود مستقیم بدون نیاز به احراز هویت"),
    PIN("رمز عددی (PIN)", "ورود با رمز ۴ یا ۶ رقمی امن"),
    BIOMETRIC("اثر انگشت / تشخیص چهره", "احراز هویت بیومتریک سریع")
}

enum class AccentColorOption(val title: String, val primaryColor: Color, val containerColor: Color) {
    TEAL("زمردی پیش‌فرض", Color(0xFF0D9488), Color(0xFFCCFBF1)),
    BLUE("آبی اقیانوسی", Color(0xFF2563EB), Color(0xFFDBEAFE)),
    PURPLE("بنفش سلطنتی", Color(0xFF7C3AED), Color(0xFFEDE9FE)),
    GREEN("سبز بهاری", Color(0xFF16A34A), Color(0xFFDCFCE7)),
    ORANGE("نارنجی پرانرژی", Color(0xFFEA580C), Color(0xFFFFEDD5))
}

enum class AlertDeliveryPreference(
    val title: String,
    val description: String,
    val iconEmoji: String
) {
    BOTH("پیامک و نوتیفیکیشن", "ارسال همزمان پیامک به تلفن همراه و نمایش اعلان سیستمی", "🔔📲"),
    NOTIFICATION_ONLY("فقط نوتیفیکیشن", "ارسال تنها اعلان هوشمند بدون پیامک", "🔔"),
    SMS_ONLY("فقط پیامک", "ارسال تنها پیامک متنی به شماره همراه", "💬"),
    DISABLED("غیرفعال", "غیرفعال‌سازی موقت ارسال هشدارها", "🔕")
}

enum class SettingsDestination {
    MAIN,
    GENERAL,
    ACCOUNTS,
    CATEGORIES,
    VEHICLES,
    NOTIFICATIONS,
    SMS,
    REMINDERS,
    APPEARANCE,
    BACKUP,
    DATA_MANAGEMENT,
    SECURITY,
    APP_LOCK,
    LOCK_AND_AUTH,
    SUPPORT,
    SUPPORT_BACKUP,
    FINANCIAL_HEALTH,
    ABOUT
}

data class AccountSettingItem(
    val id: String,
    val title: String,
    val bankName: String,
    val accountNumberMasked: String,
    val balanceFormatted: String,
    val accountType: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color
)

data class CategorySettingItem(
    val id: String,
    val title: String,
    val iconEmoji: String,
    @DrawableRes val iconRes: Int? = null,
    val accentColor: Color,
    val transactionCount: Int,
    val isExpense: Boolean = true
)

data class VehicleSettingItem(
    val id: String,
    val name: String,
    val modelYear: String,
    val mileageFormatted: String,
    val plateNumber: String,
    val statusText: String,
    val statusColor: Color,
    @DrawableRes val iconRes: Int
)

data class FaqItem(
    val id: String,
    val question: String,
    val answer: String,
    val category: String
)

data class SettingsSearchItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val section: String,
    val destination: SettingsDestination,
    @DrawableRes val iconRes: Int? = null,
    val iconEmoji: String? = null
)

object SettingsMockDataSource {
    val accounts = listOf(
        AccountSettingItem(
            id = "acc_1",
            title = "کیف پول نقدی",
            bankName = "موجودی جیبی",
            accountNumberMasked = "حساب پیش‌فرض نقدینگی",
            balanceFormatted = "۲,۵۰۰,۰۰۰",
            accountType = "کیف پول",
            iconRes = R.drawable.img_3d_wallet,
            accentColor = EmeraldPrimaryLight
        ),
        AccountSettingItem(
            id = "acc_2",
            title = "حساب جاری ملت",
            bankName = "بانک ملت",
            accountNumberMasked = "۶۱۰۴ •••• ۴۹۱۸",
            balanceFormatted = "۱۵,۸۰۰,۰۰۰",
            accountType = "بانک",
            iconRes = R.drawable.img_3d_bank,
            accentColor = Color(0xFFE11D48)
        ),
        AccountSettingItem(
            id = "acc_3",
            title = "کارت حقوق ملی",
            bankName = "بانک ملی",
            accountNumberMasked = "۶۰۳۷ •••• ۸۲۲۱",
            balanceFormatted = "۸,۲۰۰,۰۰۰",
            accountType = "کارت",
            iconRes = R.drawable.img_3d_card,
            accentColor = Color(0xFF2563EB)
        ),
        AccountSettingItem(
            id = "acc_4",
            title = "سپرده پاسارگاد",
            bankName = "بانک پاسارگاد",
            accountNumberMasked = "۵۰۲۲ •••• ۷۷۱۳",
            balanceFormatted = "۴,۲۰۰,۰۰۰",
            accountType = "بانک",
            iconRes = R.drawable.img_3d_bank,
            accentColor = WarningAmberLight
        )
    )

    val expenseCategories = listOf(
        CategorySettingItem("exp_food", "غذا و رستوران", "🍔", R.drawable.img_3d_food, Color(0xFFF97316), 42, true),
        CategorySettingItem("exp_car", "هزینه‌های خودرو", "🚗", R.drawable.img_3d_car, Color(0xFF3B82F6), 18, true),
        CategorySettingItem("exp_home", "مسکن و قبوض", "🏠", R.drawable.img_3d_home, Color(0xFF10B981), 7, true),
        CategorySettingItem("exp_shop", "خرید روزمره", "🛒", R.drawable.img_3d_shopping, Color(0xFF8B5CF6), 31, true),
        CategorySettingItem("exp_health", "سلامت و دارو", "💊", null, Color(0xFFEC4899), 9, true),
        CategorySettingItem("exp_edu", "آموزش و کتاب", "🎓", null, Color(0xFF06B6D4), 4, true),
        CategorySettingItem("exp_fun", "تفریح و سفر", "🎮", null, Color(0xFFF59E0B), 12, true),
        CategorySettingItem("exp_install", "پرداخت اقساط", "💳", R.drawable.img_3d_installment, Color(0xFF6366F1), 6, true),
        CategorySettingItem("exp_misc", "سایر مخارج", "📦", null, Color(0xFF64748B), 15, true)
    )

    val incomeCategories = listOf(
        CategorySettingItem("inc_salary", "حقوق و دستمزد", "💰", R.drawable.img_3d_wallet, EmeraldPrimaryLight, 6, false),
        CategorySettingItem("inc_bonus", "پاداش و عیدی", "🎁", null, Color(0xFFEC4899), 2, false),
        CategorySettingItem("inc_project", "فروش و فریلنسری", "💻", null, InfoIndigoLight, 8, false),
        CategorySettingItem("inc_invest", "سود و سرمایه‌گذاری", "📈", R.drawable.img_3d_chart, WarningAmberLight, 4, false),
        CategorySettingItem("inc_other", "سایر دریافتی‌ها", "💵", null, Color(0xFF14B8A6), 3, false)
    )

    val vehicles = listOf(
        VehicleSettingItem(
            id = "veh_peugeot",
            name = "پژو 206 تیپ ۵",
            modelYear = "مدل ۱۳۹۹",
            mileageFormatted = "۱۲۵,۸۰۰ کیلومتر",
            plateNumber = "ایران ۶۸ - ۷۴۱ ج ۳۵",
            statusText = "وضعیت عالی",
            statusColor = EmeraldPrimaryLight,
            iconRes = R.drawable.img_3d_car_peugeot
        ),
        VehicleSettingItem(
            id = "veh_dena",
            name = "دنا پلاس توربو",
            modelYear = "مدل ۱۴۰۱",
            mileageFormatted = "۸۲,۴۰۰ کیلومتر",
            plateNumber = "ایران ۱۱ - ۵۱۸ ق ۷۲",
            statusText = "نزدیک موعد سرویس",
            statusColor = WarningAmberLight,
            iconRes = R.drawable.img_3d_car_dena
        )
    )

    val faqs = listOf(
        FaqItem(
            id = "faq_1",
            question = "چگونه یک حساب بانکی یا کیف پول جدید اضافه کنم؟",
            answer = "از بخش تنظیمات مالی > مدیریت حساب‌ها یا از طریق دکمه شناور در صفحه مالی می‌توانید با ثبت نام، موجودی اولیه و نوع حساب، کارت یا حساب جدید تعریف نمایید.",
            category = "مالی"
        ),
        FaqItem(
            id = "faq_2",
            question = "آیا یادآوری‌های سرویس خودرو به صورت هوشمند محاسبه می‌شوند؟",
            answer = "بله، برنامه با توجه به میانگین پیمایش روزانه ثبت‌شده توسط شما، تاریخ تقریبی رسیدن به کیلومتر تعویض روغن یا سرویس دوره‌ای را پیش‌بینی و یادآوری می‌کند.",
            category = "خودرو"
        ),
        FaqItem(
            id = "faq_3",
            question = "آیا اطلاعات من در گوشی ذخیره می‌شود و امنیت دارد؟",
            answer = "تمامی اطلاعات به صورت محلی و کاملاً آفلاین روی حافظه دستگاه شما رمزگذاری می‌شوند. همچنین می‌توانید با فعال‌سازی رمز عبور یا اثر انگشت، امنیت برنامه را دوچندان کنید.",
            category = "امنیت"
        ),
        FaqItem(
            id = "faq_4",
            question = "چگونه از اطلاعاتم فایل پشتیبان تهیه کنم؟",
            answer = "در بخش تنظیمات > پشتیبان‌گیری و داده‌ها، گزینه «ایجاد پشتیبان» را انتخاب کنید تا یک نسخه خروجی امن از کل داده‌های مالی و خودرو برای شما آماده شود.",
            category = "پشتیبان‌گیری"
        )
    )

    val searchableItems = listOf(
        SettingsSearchItem("s_gen", "تنظیمات عمومی", "زبان برنامه، واحد پول (ریال و تومان)، حالت شب و روز و ارسال هشدارها", "تنظیمات عمومی", SettingsDestination.GENERAL, R.drawable.img_3d_settings_gear),
        SettingsSearchItem("s_lang", "زبان برنامه", "فارسی یا انگلیسی", "تنظیمات عمومی", SettingsDestination.GENERAL, null, "🌐"),
        SettingsSearchItem("s_currency", "واحد پول (ریال و تومان)", "تغییر واحد پول به ریال یا تومان", "تنظیمات عمومی", SettingsDestination.GENERAL, null, "🪙"),
        SettingsSearchItem("s_night_mode", "حالت شب و روز", "تم روشن، تاریک و هماهنگ با سیستم", "تنظیمات عمومی", SettingsDestination.GENERAL, null, "🌓"),
        SettingsSearchItem("s_alerts", "ارسال پیامک و نوتیفیکیشن", "تنظیم نحوه ارسال هشدارها: هر دو، فقط نوتیفیکیشن، فقط پیامک", "تنظیمات عمومی", SettingsDestination.GENERAL, R.drawable.img_3d_bell_notification),
        SettingsSearchItem("s_backup_sup", "پشتیبانی و بک‌آپ‌گیری", "تهیه نسخه پشتیبان، بازیابی فایل، راهنما و ارتباط با کارشناسان", "پشتیبانی و بک‌آپ", SettingsDestination.SUPPORT_BACKUP, R.drawable.img_3d_cloud_backup),
        SettingsSearchItem("s_sec", "امنیت و قفل ورود", "قفل با اثر انگشت، رمز عبور و حریم خصوصی", "امنیت", SettingsDestination.SECURITY, R.drawable.img_3d_shield_security),
        SettingsSearchItem("s_about", "درباره برنامه", "اطلاعات نسخه و مجوزها", "درباره", SettingsDestination.ABOUT, R.drawable.img_3d_settings_gear)
    )
}
