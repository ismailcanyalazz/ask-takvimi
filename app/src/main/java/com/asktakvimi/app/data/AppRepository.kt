package com.asktakvimi.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.UUID

/**
 * Uygulamanın tek veri kaynağı.
 *
 * Tüm ekranlar buradaki [data] akışını dinler; her değişiklik anında
 * diske yazılır ve bildirim planlaması yenilenir.
 */
object AppRepository {

    private var appContext: Context? = null

    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    /**
     * Veri her değiştiğinde çağrılır. Application sınıfı burayı
     * bildirim planlayıcısına bağlar (döngüsel bağımlılık olmasın diye).
     */
    var onDataChanged: ((AppData) -> Unit)? = null

    /** Uygulama açılışında (Application.onCreate) bir kez çağrılır. */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        _data.value = AppStorage.load(context.applicationContext)
    }

    /**
     * Arka plan alıcıları (bildirim/boot) için: süreç yeni başladıysa
     * veriyi diskten yükler.
     */
    fun ensureLoaded(context: Context) {
        if (appContext == null) init(context)
    }

    private fun mutate(transform: (AppData) -> AppData) {
        val updated = transform(_data.value)
        _data.value = updated
        appContext?.let { AppStorage.save(it, updated) }
        onDataChanged?.invoke(updated)
    }

    // ------------------------------------------------------------------
    // Regl dönemleri
    // ------------------------------------------------------------------

    /**
     * Yeni bir regl başlangıcı kaydeder.
     * Önceki dönem hâlâ açıksa, yeni başlangıcın bir gün öncesinde kapatılır.
     */
    fun startPeriod(date: LocalDate) = mutate { current ->
        if (current.periods.any { it.start == date }) return@mutate current
        val closed = current.periods.map { p ->
            if (p.end == null && p.start.isBefore(date)) p.copy(end = date.minusDays(1)) else p
        }
        current.copy(
            periods = (closed + Period(id = UUID.randomUUID().toString(), start = date))
                .sortedBy { it.start }
        )
    }

    /** Devam eden (veya verilen) dönemin bitiş gününü kaydeder. */
    fun setPeriodEnd(periodId: String, end: LocalDate?) = mutate { current ->
        current.copy(
            periods = current.periods.map { p ->
                if (p.id != periodId) p
                else if (end != null && end.isBefore(p.start)) p
                else p.copy(end = end)
            }
        )
    }

    /** Bir dönemin tarihlerini düzenler. */
    fun updatePeriod(periodId: String, start: LocalDate, end: LocalDate?) = mutate { current ->
        current.copy(
            periods = current.periods.map { p ->
                if (p.id != periodId) p
                else p.copy(start = start, end = if (end != null && end.isBefore(start)) null else end)
            }.sortedBy { it.start }
        )
    }

    /** Bir dönemi siler. */
    fun deletePeriod(periodId: String) = mutate { current ->
        current.copy(periods = current.periods.filterNot { it.id == periodId })
    }

    /** Bugün regl var mı yok mu durumunu tek dokunuşla değiştirir. */
    fun togglePeriodDay(date: LocalDate) {
        val existing = _data.value.periods.firstOrNull { it.start == date }
        if (existing != null) deletePeriod(existing.id) else startPeriod(date)
    }

    // ------------------------------------------------------------------
    // Günlük belirti kayıtları
    // ------------------------------------------------------------------

    fun saveLog(log: DayLog) = mutate { current ->
        val logs = current.logs.toMutableMap()
        if (log.isEmpty) logs.remove(log.date) else logs[log.date] = log
        current.copy(logs = logs)
    }

    fun deleteLog(date: LocalDate) = mutate { current ->
        current.copy(logs = current.logs - date)
    }

    fun logFor(date: LocalDate): DayLog = _data.value.logs[date] ?: DayLog(date)

    // ------------------------------------------------------------------
    // Ayarlar
    // ------------------------------------------------------------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) = mutate { current ->
        current.copy(settings = transform(current.settings))
    }

    // ------------------------------------------------------------------
    // Yedekleme / silme
    // ------------------------------------------------------------------

    /** Dışa aktarma için JSON metni üretir. */
    fun exportJson(): String = AppStorage.encode(_data.value)

    /**
     * Yedekten geri yükler. Biçim bozuksa false döner ve mevcut veriye dokunulmaz.
     */
    fun importJson(text: String): Boolean = try {
        val restored = AppStorage.decode(text)
        mutate { restored }
        true
    } catch (e: Exception) {
        false
    }

    /** Tüm kişisel verileri kalıcı olarak siler. */
    fun wipeAll() {
        appContext?.let { AppStorage.wipe(it) }
        val empty = AppData(settings = AppSettings(onboardingDone = false))
        _data.value = empty
        appContext?.let { AppStorage.save(it, empty) }
        onDataChanged?.invoke(empty)
    }
}
