package com.asktakvimi.app.domain

import com.asktakvimi.app.data.AppSettings
import com.asktakvimi.app.data.Period
import java.time.LocalDate

/** Tahminlerin ne kadar güvenilir olduğunu anlatır. */
enum class Confidence(val label: String, val explanation: String) {
    LOW(
        "Yaklaşık tahmin",
        "Henüz yeterli kayıt yok. Tahminler girdiğin ortalama değerlere dayanıyor ve değişebilir."
    ),
    MEDIUM(
        "Gelişen tahmin",
        "Birkaç kaydın var. Yeni her regl kaydıyla tahminler daha isabetli hale gelecek."
    ),
    HIGH(
        "Daha isabetli tahmin",
        "Düzenli kayıtların sayesinde tahminler geçmiş döngülerinden hesaplanıyor."
    )
}

/** Takvimde bir günün nasıl boyanacağını belirler. */
enum class DayKind {
    ACTUAL_PERIOD,      // Kullanıcının işaretlediği gerçek regl günü
    PREDICTED_PERIOD,   // Tahmini regl günü
    OVULATION,          // Tahmini yumurtlama günü
    FERTILE,            // Tahmini doğurganlık penceresi
    NONE
}

/** Bir anlık döngü özeti. Tüm alanlar veri yoksa null olabilir. */
data class CyclePrediction(
    val lastPeriodStart: LocalDate? = null,
    val currentPeriodEnd: LocalDate? = null,
    val predictedNextStart: LocalDate? = null,
    val predictedNextEnd: LocalDate? = null,
    val daysUntilNextPeriod: Int? = null,
    val cycleDay: Int? = null,
    val ovulationDay: LocalDate? = null,
    val fertileStart: LocalDate? = null,
    val fertileEnd: LocalDate? = null,
    val cycleLengthUsed: Int = 28,
    val periodLengthUsed: Int = 5,
    val confidence: Confidence = Confidence.LOW,
    val isIrregular: Boolean = false,
    val isOnPeriodToday: Boolean = false,
    val hasData: Boolean = false
)

/**
 * Tüm tarih ve döngü hesaplamaları burada toplanır.
 *
 * Bu sınıf bilinçli olarak Android'e hiç bağımlı değildir; böylece
 * birim testleriyle (src/test) doğrudan sınanabilir.
 */
object CycleEngine {

    /** Makul kabul edilen döngü uzunluğu aralığı. Dışındakiler hesaba katılmaz. */
    const val MIN_CYCLE = 15
    const val MAX_CYCLE = 60

    /** Yumurtlamadan sonraki (luteal) evre, kişiden kişiye az değişir. */
    const val LUTEAL_PHASE = 14

    /** Ortalama hesaplanırken en çok kaç döngü geriye bakılır. */
    const val WINDOW = 6

    /** Bu değerden fazla oynama varsa döngü düzensiz sayılır. */
    const val IRREGULAR_THRESHOLD = 8

    /**
     * Tahmini tarih geçtiğinde kaç gün boyunca "gecikti" olarak gösterilir.
     * Bu süre dolmadan bir sonraki döngüye atlanmaz.
     */
    const val LATE_GRACE = 15

    /** Dönemleri başlangıç tarihine göre sıralar (eskiden yeniye). */
    fun sorted(periods: List<Period>): List<Period> = periods.sortedBy { it.start }

    /**
     * Ardışık regl başlangıçları arasındaki gün farkları.
     * Örn. 1 Ocak ve 29 Ocak → [28]
     */
    fun cycleLengths(periods: List<Period>): List<Int> {
        val s = sorted(periods)
        if (s.size < 2) return emptyList()
        return s.zipWithNext { a, b ->
            (b.start.toEpochDay() - a.start.toEpochDay()).toInt()
        }.filter { it in MIN_CYCLE..MAX_CYCLE }
    }

    /** Son [WINDOW] döngünün ortalaması; veri yoksa [fallback]. */
    fun averageCycleLength(periods: List<Period>, fallback: Int): Int {
        val lengths = cycleLengths(periods).takeLast(WINDOW)
        if (lengths.isEmpty()) return fallback
        return Math.round(lengths.average()).toInt().coerceIn(MIN_CYCLE, MAX_CYCLE)
    }

    /** Tamamlanmış dönemlerin ortalama süresi; veri yoksa [fallback]. */
    fun averagePeriodLength(periods: List<Period>, fallback: Int): Int {
        val lengths = sorted(periods).mapNotNull { it.lengthInDays }
            .filter { it in 1..15 }
            .takeLast(WINDOW)
        if (lengths.isEmpty()) return fallback
        return Math.round(lengths.average()).toInt().coerceIn(1, 15)
    }

    /** Son döngülerdeki en uzun ile en kısa arasındaki fark. Veri yoksa null. */
    fun cycleVariation(periods: List<Period>): Int? {
        val lengths = cycleLengths(periods).takeLast(WINDOW)
        if (lengths.size < 2) return null
        return (lengths.max() - lengths.min())
    }

    /** Döngü düzensiz mi? (En az 3 kayıt ve yüksek oynama) */
    fun isIrregular(periods: List<Period>): Boolean {
        val v = cycleVariation(periods) ?: return false
        return cycleLengths(periods).size >= 3 && v >= IRREGULAR_THRESHOLD
    }

    /**
     * Verilen günde süren (devam eden veya o günü kapsayan) dönem.
     * Bitişi girilmemiş dönemler, [today] gününe kadar sürüyor kabul edilir.
     */
    fun periodOn(periods: List<Period>, date: LocalDate, today: LocalDate): Period? =
        sorted(periods).lastOrNull { period ->
            if (date.isBefore(period.start)) return@lastOrNull false
            val end = period.end
            if (end != null) !date.isAfter(end) else !date.isAfter(today)
        }

    /**
     * Ana tahmin fonksiyonu.
     *
     * @param today Testlerde sabitlenebilsin diye dışarıdan verilir.
     */
    fun predict(
        periods: List<Period>,
        settings: AppSettings,
        today: LocalDate
    ): CyclePrediction {
        val sortedPeriods = sorted(periods)
        val last = sortedPeriods.lastOrNull()
            ?: return CyclePrediction(
                cycleLengthUsed = settings.avgCycleLength,
                periodLengthUsed = settings.avgPeriodLength,
                confidence = Confidence.LOW,
                hasData = false
            )

        val cycleLen = if (settings.useLearnedAverages) {
            averageCycleLength(sortedPeriods, settings.avgCycleLength)
        } else {
            settings.avgCycleLength
        }
        val periodLen = if (settings.useLearnedAverages) {
            averagePeriodLength(sortedPeriods, settings.avgPeriodLength)
        } else {
            settings.avgPeriodLength
        }

        // Bir sonraki tahmini başlangıç: son başlangıcın üzerine bir döngü eklenir.
        // Tarih geçtiyse de hemen bir sonraki döngüye atlamayız; gecikmeyi
        // gösterebilmek için [LATE_GRACE] gün boyunca aynı tahmin korunur.
        var nextStart = last.start.plusDays(cycleLen.toLong())
        var guard = 0
        while (nextStart.plusDays(LATE_GRACE.toLong()).isBefore(today) && guard < 500) {
            nextStart = nextStart.plusDays(cycleLen.toLong())
            guard++
        }
        val activePeriod = periodOn(sortedPeriods, today, today)

        val cycleDay = (today.toEpochDay() - last.start.toEpochDay()).toInt() + 1
        val ovulation = nextStart.minusDays(LUTEAL_PHASE.toLong())

        val lengths = cycleLengths(sortedPeriods)
        val variation = cycleVariation(sortedPeriods)
        val irregular = isIrregular(sortedPeriods)
        val confidence = when {
            lengths.size >= 3 && (variation ?: 0) < IRREGULAR_THRESHOLD -> Confidence.HIGH
            lengths.isNotEmpty() -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        val currentEnd = activePeriod?.end
            ?: activePeriod?.start?.plusDays((periodLen - 1).toLong())

        return CyclePrediction(
            lastPeriodStart = last.start,
            currentPeriodEnd = currentEnd,
            predictedNextStart = nextStart,
            predictedNextEnd = nextStart.plusDays((periodLen - 1).toLong()),
            daysUntilNextPeriod = (nextStart.toEpochDay() - today.toEpochDay()).toInt(),
            cycleDay = if (cycleDay >= 1) cycleDay else null,
            ovulationDay = ovulation,
            fertileStart = ovulation.minusDays(5),
            fertileEnd = ovulation.plusDays(1),
            cycleLengthUsed = cycleLen,
            periodLengthUsed = periodLen,
            confidence = confidence,
            isIrregular = irregular,
            isOnPeriodToday = activePeriod != null,
            hasData = true
        )
    }

    /**
     * Takvim için: ileriye dönük tahmini regl başlangıçları.
     * [horizon] kadar döngü ileri hesaplanır.
     */
    fun futureStarts(
        periods: List<Period>,
        cycleLength: Int,
        today: LocalDate,
        horizon: Int = 14
    ): List<LocalDate> {
        val last = sorted(periods).lastOrNull() ?: return emptyList()
        val result = mutableListOf<LocalDate>()
        for (k in 1..horizon) {
            val d = last.start.plusDays((cycleLength.toLong() * k))
            result.add(d)
        }
        // Bugünden çok geride kalan tahminleri gösterme
        return result.filter { !it.isBefore(today.minusDays(60)) }
    }

    /**
     * Bir günün takvimde nasıl görüneceğini belirler.
     * Gerçek kayıtlar her zaman tahminlerin önüne geçer.
     */
    fun dayKind(
        date: LocalDate,
        periods: List<Period>,
        prediction: CyclePrediction,
        today: LocalDate
    ): DayKind {
        // 1) Gerçek regl günleri
        val sortedPeriods = sorted(periods)
        for (p in sortedPeriods) {
            val end = p.end ?: p.start.plusDays((prediction.periodLengthUsed - 1).toLong())
            if (!date.isBefore(p.start) && !date.isAfter(end)) {
                // Bitişi girilmemiş dönemin gelecekteki günleri tahmin sayılır
                return if (p.end == null && date.isAfter(today)) {
                    DayKind.PREDICTED_PERIOD
                } else {
                    DayKind.ACTUAL_PERIOD
                }
            }
        }
        if (!prediction.hasData) return DayKind.NONE

        // 2) Tahmini regl günleri
        val starts = futureStarts(periods, prediction.cycleLengthUsed, today)
        for (s in starts) {
            val e = s.plusDays((prediction.periodLengthUsed - 1).toLong())
            if (!date.isBefore(s) && !date.isAfter(e)) return DayKind.PREDICTED_PERIOD
        }

        // 3) Yumurtlama ve doğurganlık penceresi
        for (s in starts) {
            val ov = s.minusDays(LUTEAL_PHASE.toLong())
            if (date == ov) return DayKind.OVULATION
            if (!date.isBefore(ov.minusDays(5)) && !date.isAfter(ov.plusDays(1))) {
                return DayKind.FERTILE
            }
        }
        prediction.ovulationDay?.let { ov ->
            if (date == ov) return DayKind.OVULATION
            if (!date.isBefore(ov.minusDays(5)) && !date.isAfter(ov.plusDays(1))) {
                return DayKind.FERTILE
            }
        }
        return DayKind.NONE
    }
}
