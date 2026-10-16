package com.asktakvimi.app.domain

import com.asktakvimi.app.data.ReminderText
import java.time.LocalDate

/**
 * Uygulamadaki tüm romantik metinler tek yerde toplanır.
 * Böylece hem bildirimlerde hem ekranlarda aynı ses tonu korunur.
 */
object RomanticMessages {

    /** Bildirim gönderilebilecek gün farkları: 3, 2, 1 gün kala ve başlangıç günü. */
    val OFFSETS = listOf(3, 2, 1, 0)

    /** Ayarlar ekranında gösterilen başlık. */
    fun offsetLabel(offset: Int): String = when (offset) {
        0 -> "Tahmini başlangıç günü"
        1 -> "1 gün kala"
        else -> "$offset gün kala"
    }

    /** Varsayılan (romantik) bildirim metinleri. */
    fun defaultReminder(offset: Int): ReminderText = when (offset) {
        3 -> ReminderText(
            title = "Aşkım, küçük bir hatırlatma ❤️",
            body = "Selam aşkım, tahmini regline 3 gün kaldı. Kendine iyi bakmayı unutma. ❤️"
        )
        2 -> ReminderText(
            title = "Canım benim 💗",
            body = "Aşkım, reglin yaklaşıyor olabilir. Bugün biraz dinlenmeye ve kendine zaman ayırmaya ne dersin? 🌸"
        )
        1 -> ReminderText(
            title = "Yarın için küçük bir hatırlatma ❤️",
            body = "Selam aşkım, tahmini regline 1 gün kaldı. Yanında olduğumu unutma. Seni çok seviyorum. 🥹❤️"
        )
        else -> ReminderText(
            title = "Bugün kendine iyi bak aşkım 🌷",
            body = "Bugün regl dönemin başlayabilir. Rahatına bak, bol bol dinlen ve kendine nazik davran. Seni seviyorum. ❤️"
        )
    }

    /**
     * Gizlilik modu açıkken kullanılan metin.
     * Bildirim panelinde hiçbir sağlık bilgisi görünmez.
     */
    fun privateReminder(): ReminderText = ReminderText(
        title = "Sana küçük bir hatırlatma ❤️",
        body = "Bugün kendine biraz zaman ayırmayı unutma. Seni düşünen biri var. 💗"
    )

    /** Ana ekrandaki karta eşlik eden kısa romantik cümle. */
    fun homeMessage(daysUntil: Int?, isOnPeriod: Boolean): String = when {
        isOnPeriod ->
            "Bugün kendine ekstra nazik davran. Sıcak bir şeyler iç, biraz dinlen. Seni seviyorum. 💗"
        daysUntil == null ->
            "İlk kaydını eklediğinde burada sana özel hatırlatmalar görünecek. 🌸"
        daysUntil == 0 ->
            "Bugün başlayabilir. Rahatına bak, yanındayım. ❤️"
        daysUntil == 1 ->
            "Yarın için bugünden hazırlıklı ol. Yanında olduğumu unutma. 🥹❤️"
        daysUntil in 2..3 ->
            "Bugün kendine biraz daha nazik davranmayı unutma. Seni seviyorum. 💗"
        daysUntil in 4..7 ->
            "Bu haftayı yavaş ve keyifli geçir. Kendine iyi bakmayı unutma. 🌷"
        else ->
            "Bugün nasıl hissettiğini kaydetmek ister misin? Her gününü önemsiyorum. 🌙"
    }

    /** Uygulama içindeki romantik mesaj koleksiyonu. */
    val collection: List<String> = listOf(
        "Zor günlerinde de yanındayım, güzel günlerinde de. ❤️",
        "Kendine iyi bakman benim için dünyadaki en güzel haber. 🌷",
        "Yorulduğunda durmak da bir çözümdür. İzin var. 💗",
        "Bugün biraz yavaş ol, dünya seni bekleyebilir. 🌙",
        "Senin iyi hissetmen her şeyden önemli. 🌸",
        "Canın bir şey çekerse söyle, bahane arıyordum zaten. 😊",
        "Bu da geçecek, ben buradayım. ❤️",
        "Sarılmaya ihtiyacın varsa sırada ben varım. 🤍",
        "Kendine gösterdiğin şefkat kadar güzelsin. 💗",
        "Bugün için tek görevin: biraz dinlenmek. 🌿"
    )

    /** Gün bazlı, her gün aynı kalan ama günler arasında değişen motivasyon mesajı. */
    fun dailyMotivation(date: LocalDate): String {
        val index = (date.toEpochDay() % collection.size).toInt()
        return collection[if (index < 0) index + collection.size else index]
    }

    /** Doğurganlık tahminleri için her yerde aynı şekilde gösterilen uyarı. */
    const val FERTILITY_DISCLAIMER =
        "Yumurtlama ve doğurganlık tahminleri yalnızca bilgilendirme amaçlıdır. " +
            "Bunlar bir gebelikten korunma yöntemi değildir ve tıbbi tavsiye yerine geçmez."
}
