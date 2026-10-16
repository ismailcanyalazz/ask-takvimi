package com.asktakvimi.app

import com.asktakvimi.app.data.AppSettings
import com.asktakvimi.app.data.Period
import com.asktakvimi.app.domain.Confidence
import com.asktakvimi.app.domain.CycleEngine
import com.asktakvimi.app.domain.DayKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tarih hesaplamaları ve döngü tahminlerinin birim testleri.
 * Hepsi sabit tarihlerle çalışır; "bugün" dışarıdan verildiği için
 * testler her gün aynı sonucu üretir.
 */
class CycleEngineTest {

    private val settings = AppSettings(avgCycleLength = 28, avgPeriodLength = 5)

    private fun period(start: String, end: String? = null) =
        Period(id = start, start = LocalDate.parse(start), end = end?.let { LocalDate.parse(it) })

    // ------------------------------------------------------------------
    // Ortalamalar
    // ------------------------------------------------------------------

    @Test
    fun `ardisik donguler arasindaki fark dogru hesaplanir`() {
        val periods = listOf(
            period("2026-01-01"),
            period("2026-01-29"),
            period("2026-02-27")
        )
        assertEquals(listOf(28, 29), CycleEngine.cycleLengths(periods))
    }

    @Test
    fun `tek kayitta ortalama varsayilan degere duser`() {
        val periods = listOf(period("2026-03-01"))
        assertEquals(30, CycleEngine.averageCycleLength(periods, fallback = 30))
    }

    @Test
    fun `ortalama dongu kayitlardan hesaplanir`() {
        val periods = listOf(
            period("2026-01-01"),
            period("2026-01-31"), // 30
            period("2026-03-02")  // 30
        )
        assertEquals(30, CycleEngine.averageCycleLength(periods, fallback = 28))
    }

    @Test
    fun `ortalama regl suresi tamamlanmis donemlerden hesaplanir`() {
        val periods = listOf(
            period("2026-01-01", "2026-01-05"), // 5 gün
            period("2026-01-29", "2026-02-01")  // 4 gün
        )
        assertEquals(5, CycleEngine.averagePeriodLength(periods, fallback = 7))
    }

    @Test
    fun `makul olmayan araliklar hesaba katilmaz`() {
        val periods = listOf(
            period("2026-01-01"),
            period("2026-01-03"), // 2 gün — yanlış giriş, sayılmaz
            period("2026-01-31")  // 28 gün
        )
        assertEquals(listOf(28), CycleEngine.cycleLengths(periods))
    }

    // ------------------------------------------------------------------
    // Tahminler
    // ------------------------------------------------------------------

    @Test
    fun `kalan gun sayisi dogru hesaplanir`() {
        val today = LocalDate.parse("2026-04-11")
        val periods = listOf(period("2026-04-01", "2026-04-05"))

        val p = CycleEngine.predict(periods, settings, today)

        assertEquals(LocalDate.parse("2026-04-29"), p.predictedNextStart)
        assertEquals(18, p.daysUntilNextPeriod)
        assertEquals(11, p.cycleDay)
    }

    @Test
    fun `ay ve yil gecisinde tahmin dogru`() {
        val today = LocalDate.parse("2026-01-05")
        val periods = listOf(period("2025-12-20", "2025-12-24"))

        val p = CycleEngine.predict(periods, settings, today)

        // 20 Aralık + 28 gün = 17 Ocak
        assertEquals(LocalDate.parse("2026-01-17"), p.predictedNextStart)
        assertEquals(12, p.daysUntilNextPeriod)
        assertEquals(17, p.cycleDay)
    }

    @Test
    fun `artik yil gecisi dogru hesaplanir`() {
        val today = LocalDate.parse("2028-02-20")
        val periods = listOf(period("2028-02-10"))

        val p = CycleEngine.predict(periods, settings, today)

        // 2028 artık yıl: 10 Şubat + 28 gün = 9 Mart
        assertEquals(LocalDate.parse("2028-03-09"), p.predictedNextStart)
    }

    @Test
    fun `gecikme negatif gun olarak gosterilir`() {
        val today = LocalDate.parse("2026-05-05")
        val periods = listOf(period("2026-04-01", "2026-04-05"))

        val p = CycleEngine.predict(periods, settings, today)

        // Tahmin 29 Nisan'dı, 6 gün geçti
        assertEquals(LocalDate.parse("2026-04-29"), p.predictedNextStart)
        assertEquals(-6, p.daysUntilNextPeriod)
    }

    @Test
    fun `cok gecikince bir sonraki dongoye gecilir`() {
        val today = LocalDate.parse("2026-05-30")
        val periods = listOf(period("2026-04-01", "2026-04-05"))

        val p = CycleEngine.predict(periods, settings, today)

        // 29 Nisan + 15 gün hoşgörü dolmuş → 27 Mayıs tahmini
        assertEquals(LocalDate.parse("2026-05-27"), p.predictedNextStart)
    }

    @Test
    fun `yumurtlama bir sonraki baslangictan 14 gun once`() {
        val today = LocalDate.parse("2026-04-11")
        val periods = listOf(period("2026-04-01", "2026-04-05"))

        val p = CycleEngine.predict(periods, settings, today)

        assertEquals(LocalDate.parse("2026-04-15"), p.ovulationDay)
        assertEquals(LocalDate.parse("2026-04-10"), p.fertileStart)
        assertEquals(LocalDate.parse("2026-04-16"), p.fertileEnd)
    }

    @Test
    fun `devam eden donem bugun regl olarak isaretlenir`() {
        val today = LocalDate.parse("2026-04-03")
        val periods = listOf(period("2026-04-01"))

        val p = CycleEngine.predict(periods, settings, today)

        assertTrue(p.isOnPeriodToday)
        assertEquals(3, p.cycleDay)
    }

    @Test
    fun `kayit yoksa tahmin uretilmez`() {
        val p = CycleEngine.predict(emptyList(), settings, LocalDate.parse("2026-04-11"))

        assertFalse(p.hasData)
        assertEquals(Confidence.LOW, p.confidence)
        assertEquals(null, p.predictedNextStart)
    }

    // ------------------------------------------------------------------
    // Güven düzeyi ve düzensizlik
    // ------------------------------------------------------------------

    @Test
    fun `duzenli kayitlarda guven yuksek`() {
        val periods = listOf(
            period("2026-01-01"),
            period("2026-01-29"),
            period("2026-02-26"),
            period("2026-03-26")
        )
        val p = CycleEngine.predict(periods, settings, LocalDate.parse("2026-04-01"))

        assertEquals(Confidence.HIGH, p.confidence)
        assertFalse(p.isIrregular)
    }

    @Test
    fun `degisken dongu duzensiz olarak isaretlenir`() {
        val periods = listOf(
            period("2026-01-01"),
            period("2026-01-23"), // 22
            period("2026-02-26"), // 34
            period("2026-03-21")  // 23
        )
        assertTrue(CycleEngine.isIrregular(periods))
        assertEquals(12, CycleEngine.cycleVariation(periods))
    }

    // ------------------------------------------------------------------
    // Takvim renklendirmesi
    // ------------------------------------------------------------------

    @Test
    fun `gercek regl gunleri isaretlenir`() {
        val today = LocalDate.parse("2026-04-11")
        val periods = listOf(period("2026-04-01", "2026-04-05"))
        val p = CycleEngine.predict(periods, settings, today)

        assertEquals(
            DayKind.ACTUAL_PERIOD,
            CycleEngine.dayKind(LocalDate.parse("2026-04-03"), periods, p, today)
        )
        assertEquals(
            DayKind.NONE,
            CycleEngine.dayKind(LocalDate.parse("2026-04-07"), periods, p, today)
        )
    }

    @Test
    fun `tahmini regl gunleri isaretlenir`() {
        val today = LocalDate.parse("2026-04-11")
        val periods = listOf(period("2026-04-01", "2026-04-05"))
        val p = CycleEngine.predict(periods, settings, today)

        // 29 Nisan + 5 günlük dönem
        assertEquals(
            DayKind.PREDICTED_PERIOD,
            CycleEngine.dayKind(LocalDate.parse("2026-04-30"), periods, p, today)
        )
    }

    @Test
    fun `yumurtlama gunu takvimde ayri gosterilir`() {
        val today = LocalDate.parse("2026-04-11")
        val periods = listOf(period("2026-04-01", "2026-04-05"))
        val p = CycleEngine.predict(periods, settings, today)

        assertEquals(
            DayKind.OVULATION,
            CycleEngine.dayKind(LocalDate.parse("2026-04-15"), periods, p, today)
        )
        assertEquals(
            DayKind.FERTILE,
            CycleEngine.dayKind(LocalDate.parse("2026-04-13"), periods, p, today)
        )
    }
}
