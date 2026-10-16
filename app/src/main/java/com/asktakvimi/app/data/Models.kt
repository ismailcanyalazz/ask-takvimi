package com.asktakvimi.app.data

import java.time.LocalDate

/**
 * Kaydedilmiş bir regl dönemi.
 *
 * @param start Reglin başladığı gün (kullanıcı tarafından işaretlenir).
 * @param end   Reglin bittiği gün. Devam eden dönemlerde null olur.
 */
data class Period(
    val id: String,
    val start: LocalDate,
    val end: LocalDate? = null
) {
    /** Dönem kaç gün sürdü? Bitiş girilmemişse null döner. */
    val lengthInDays: Int?
        get() = end?.let { (it.toEpochDay() - start.toEpochDay()).toInt() + 1 }

    /**
     * Verilen gün bu dönemin içinde mi?
     * Bitişi girilmemiş dönem [today] gününe kadar sürüyor sayılır.
     */
    fun contains(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        if (date.isBefore(start)) return false
        val last = end ?: return !date.isAfter(today)
        return !date.isAfter(last)
    }
}

/** Kanama yoğunluğu. */
enum class Flow(val label: String) {
    NONE("Yok"),
    LIGHT("Hafif"),
    MEDIUM("Orta"),
    HEAVY("Yoğun")
}

/** Gün içinde işaretlenebilen belirtiler. */
enum class Symptom(val label: String) {
    CRAMPS("Karın ağrısı"),
    BACK_PAIN("Bel ağrısı"),
    HEADACHE("Baş ağrısı"),
    FATIGUE("Yorgunluk"),
    BLOATING("Şişkinlik"),
    SKIN("Cilt değişikliği"),
    TENDER_BREASTS("Göğüs hassasiyeti"),
    NAUSEA("Mide bulantısı"),
    CRAVINGS("Tatlı/yemek isteği"),
    DIZZINESS("Baş dönmesi")
}

/** Ruh hali. */
enum class Mood(val label: String, val emoji: String) {
    HAPPY("Mutlu", "😊"),
    CALM("Sakin", "🙂"),
    TIRED("Yorgun", "😴"),
    SENSITIVE("Hassas", "🥺"),
    IRRITABLE("Sinirli", "😤"),
    SAD("Üzgün", "😢")
}

/** Uyku durumu. */
enum class SleepQuality(val label: String) {
    GOOD("İyi uyudum"),
    NORMAL("Normal"),
    BAD("Kötü uyudum")
}

/** İştah değişikliği. */
enum class Appetite(val label: String) {
    LOW("Az"),
    NORMAL("Normal"),
    HIGH("Fazla")
}

/**
 * Bir güne ait kayıt. Hiçbir alan zorunlu değildir; kullanıcı sadece
 * işaretlemek istediklerini doldurur.
 */
data class DayLog(
    val date: LocalDate,
    val flow: Flow = Flow.NONE,
    val symptoms: Set<Symptom> = emptySet(),
    val mood: Mood? = null,
    val sleep: SleepQuality? = null,
    val appetite: Appetite? = null,
    val note: String = ""
) {
    /** Kayıtta gerçekten bir bilgi var mı? Boş kayıtlar saklanmaz. */
    val isEmpty: Boolean
        get() = flow == Flow.NONE && symptoms.isEmpty() && mood == null &&
                sleep == null && appetite == null && note.isBlank()
}

/** Tema tercihi. */
enum class ThemeMode(val label: String) {
    SYSTEM("Telefon ayarına uy"),
    LIGHT("Açık tema"),
    DARK("Koyu tema")
}

/**
 * Bildirim metni. Kullanıcı ayarlardan değiştirebilir;
 * boş bırakılırsa varsayılan romantik metin kullanılır.
 */
data class ReminderText(val title: String, val body: String)

/**
 * Uygulama ayarları. Tümü cihazda saklanır.
 *
 * @param reminderOffsets Hangi günlerde hatırlatma gelsin? 3 = regle 3 gün kala,
 *                        0 = tahmini başlangıç günü.
 */
data class AppSettings(
    val onboardingDone: Boolean = false,
    val partnerName: String = "",
    val avgCycleLength: Int = 28,
    val avgPeriodLength: Int = 5,
    val useLearnedAverages: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val reminderOffsets: Set<Int> = setOf(3, 2, 1, 0),
    val customTexts: Map<Int, ReminderText> = emptyMap(),
    val privacyMode: Boolean = false,
    val romanticMessagesEnabled: Boolean = true,
    val dailyMotivationEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lockEnabled: Boolean = false,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val biometricEnabled: Boolean = false
)

/** Uygulamanın tüm verisi tek bir durumda toplanır. */
data class AppData(
    val periods: List<Period> = emptyList(),
    val logs: Map<LocalDate, DayLog> = emptyMap(),
    val settings: AppSettings = AppSettings()
)
