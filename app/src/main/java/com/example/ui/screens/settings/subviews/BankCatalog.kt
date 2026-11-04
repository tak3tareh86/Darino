package com.example.ui.screens.settings.subviews

data class BankCatalogEntry(
    val id: String,
    val title: String,
    val logoKey: String,
    val group: String,
    val aliases: List<String> = emptyList()
)

object BankCatalog {
    val entries = listOf(
        BankCatalogEntry("melli", "بانک ملی ایران", "Melli", "بانک‌های فعال", listOf("ملی", "بانک ملی")),
        BankCatalogEntry("sepah", "بانک سپه", "Sepah", "بانک‌های فعال", listOf("سپه")),
        BankCatalogEntry("keshavarzi", "بانک کشاورزی", "Keshavarzi", "بانک‌های فعال", listOf("کشاورزی")),
        BankCatalogEntry("maskan", "بانک مسکن", "Maskan", "بانک‌های فعال", listOf("مسکن")),
        BankCatalogEntry("sanat-madan", "بانک صنعت و معدن", "Sanat_Madan", "بانک‌های فعال", listOf("صنعت و معدن", "صنعت معدن")),
        BankCatalogEntry("tosee-saderat", "بانک توسعه صادرات ایران", "Tosee_Saderat", "بانک‌های فعال", listOf("توسعه صادرات", "توسعه‌صادرات")),
        BankCatalogEntry("postbank", "پست بانک ایران", "Postbank", "بانک‌های فعال", listOf("پست بانک", "پست‌بانک", "پستبانک")),
        BankCatalogEntry("tosee-taavon", "بانک توسعه تعاون", "Tosee_Taavon", "بانک‌های فعال", listOf("توسعه تعاون")),
        BankCatalogEntry("saderat", "بانک صادرات ایران", "Saderat", "بانک‌های فعال", listOf("صادرات", "بانک صادرات")),
        BankCatalogEntry("refah", "بانک رفاه کارگران", "Refah", "بانک‌های فعال", listOf("رفاه", "رفاه کارگران")),
        BankCatalogEntry("tejarat", "بانک تجارت", "Tejarat", "بانک‌های فعال", listOf("تجارت")),
        BankCatalogEntry("mellat", "بانک ملت", "Mellat", "بانک‌های فعال", listOf("ملت")),
        BankCatalogEntry("sina", "بانک سینا", "Sina", "بانک‌های فعال", listOf("سینا")),
        BankCatalogEntry("karafarin", "بانک کارآفرین", "Karafarin", "بانک‌های فعال", listOf("کارآفرین", "کار افرین")),
        BankCatalogEntry("saman", "بانک سامان", "Saman", "بانک‌های فعال", listOf("سامان")),
        BankCatalogEntry("eghtesad-novin", "بانک اقتصاد نوین", "Eghtesad_Novin", "بانک‌های فعال", listOf("اقتصاد نوین", "اقتصادنوین")),
        BankCatalogEntry("parsian", "بانک پارسیان", "Parsian", "بانک‌های فعال", listOf("پارسیان")),
        BankCatalogEntry("pasargad", "بانک پاسارگاد", "Pasargad", "بانک‌های فعال", listOf("پاسارگاد")),
        BankCatalogEntry("sarmayeh", "بانک سرمایه", "Sarmayeh", "بانک‌های فعال", listOf("سرمایه")),
        BankCatalogEntry("shahr", "بانک شهر", "Shahr", "بانک‌های فعال", listOf("شهر")),
        BankCatalogEntry("dey", "بانک دی", "Dey", "بانک‌های فعال", listOf("دی")),
        BankCatalogEntry("gardeshgari", "بانک گردشگری", "Gardeshgari", "بانک‌های فعال", listOf("گردشگری")),
        BankCatalogEntry("iran-zamin", "بانک ایران‌زمین", "Iran_Zamin", "بانک‌های فعال", listOf("ایران زمین", "ایران‌زمین")),
        BankCatalogEntry("khavarmianeh", "بانک خاورمیانه", "Khavar_Mianeh", "بانک‌های فعال", listOf("خاورمیانه", "خاور میانه")),
        BankCatalogEntry("mehr-iran", "بانک قرض‌الحسنه مهر ایران", "Mehr_Iran", "بانک‌های فعال", listOf("مهر ایران", "مهرایران", "قرض الحسنه مهر ایران")),
        BankCatalogEntry("resalat", "بانک قرض‌الحسنه رسالت", "Resalat", "بانک‌های فعال", listOf("رسالت", "قرض الحسنه رسالت")),
        BankCatalogEntry("melal", "مؤسسه اعتباری ملل", "Melall", "مؤسسات اعتباری", listOf("ملل", "موسسه ملل", "مؤسسه ملل")),
        BankCatalogEntry("ayandeh", "بانک آینده (سابق)", "Ayandeh", "نام‌های قدیمی و ادغام‌شده", listOf("آینده", "بانک آینده")),
        BankCatalogEntry("ansar", "بانک انصار (ادغام‌شده در سپه)", "Ansar", "نام‌های قدیمی و ادغام‌شده", listOf("انصار")),
        BankCatalogEntry("ghavamin", "بانک قوامین (ادغام‌شده در سپه)", "Ghavamin", "نام‌های قدیمی و ادغام‌شده", listOf("قوامین")),
        BankCatalogEntry("hekmat", "بانک حکمت ایرانیان (ادغام‌شده در سپه)", "Hekmat", "نام‌های قدیمی و ادغام‌شده", listOf("حکمت ایرانیان", "حکمت")),
        BankCatalogEntry("mehr-eqtesad", "بانک مهر اقتصاد (ادغام‌شده در سپه)", "Mehr_Eghtesad", "نام‌های قدیمی و ادغام‌شده", listOf("مهر اقتصاد")),
        BankCatalogEntry("kosar", "مؤسسه اعتباری کوثر (ادغام‌شده)", "Kosar", "نام‌های قدیمی و ادغام‌شده", listOf("کوثر", "موسسه کوثر", "مؤسسه کوثر")),
        BankCatalogEntry("noor", "مؤسسه اعتباری نور (تعیین‌تکلیف‌شده)", "Noor", "نام‌های قدیمی و ادغام‌شده", listOf("نور", "موسسه نور", "مؤسسه نور")),
        BankCatalogEntry("tosee", "مؤسسه اعتباری توسعه (تعیین‌تکلیف‌شده)", "Tosee", "نام‌های قدیمی و ادغام‌شده", listOf("موسسه توسعه", "مؤسسه توسعه")),
        BankCatalogEntry("caspian", "مؤسسه اعتباری کاسپین", "Caspian", "نام‌های قدیمی و ادغام‌شده", listOf("کاسپین", "موسسه کاسپین", "مؤسسه کاسپین")),
        BankCatalogEntry("iran-venezuela", "بانک مشترک ایران و ونزوئلا", "Iran_Venezuela", "بانک‌های مشترک و خارجی", listOf("ایران ونزوئلا", "ایران و ونزوئلا")),
        BankCatalogEntry("iran-europe", "بانک تجارتی ایران و اروپا", "Iran_Europe", "بانک‌های مشترک و خارجی", listOf("ایران و اروپا", "ایران اروپا")),
        BankCatalogEntry("standard-chartered", "بانک استاندارد چارترد", "Standard_Chartered", "بانک‌های مشترک و خارجی", listOf("استاندارد چارترد", "standard chartered")),
        BankCatalogEntry("futurebank", "فیوچر بانک", "Futurebank", "بانک‌های مشترک و خارجی", listOf("فیوچر بانک", "future bank")),
        BankCatalogEntry("taavon-eslami", "تعاون اسلامی برای سرمایه‌گذاری", "Taavon_Eslami", "بانک‌های مشترک و خارجی", listOf("تعاون اسلامی", "مصرف التعاون الاسلامی"))
    )

    private fun normalize(value: String): String = value.trim()
        .replace("ي", "ی").replace("ك", "ک")
        .replace(Regex("[\\s‌]+"), "")
        .lowercase()

    fun find(value: String): BankCatalogEntry? {
        val key = normalize(value)
        return entries.firstOrNull { entry ->
            normalize(entry.title) == key || normalize(entry.id) == key ||
                entry.aliases.any { normalize(it) == key }
        }
    }

    fun search(query: String): List<BankCatalogEntry> {
        if (query.isBlank()) return entries
        val key = normalize(query)
        return entries.filter { entry ->
            normalize(entry.title).contains(key) ||
                entry.aliases.any { normalize(it).contains(key) }
        }
    }
}
