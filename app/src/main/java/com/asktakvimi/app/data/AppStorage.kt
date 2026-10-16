package com.asktakvimi.app.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * Verilerin cihazda saklanması.
 *
 * Veriler uygulamanın özel klasöründe (filesDir) tek bir JSON dosyasında tutulur.
 * Bu klasöre başka uygulamalar erişemez ve dosya hiçbir yere gönderilmez.
 * Aynı biçim, dışa aktarma (yedekleme) için de kullanılır.
 */
object AppStorage {

    private const val TAG = "AppStorage"
    private const val FILE_NAME = "ask_takvimi_veri.json"
    const val FORMAT_VERSION = 1

    private fun file(context: Context) = File(context.filesDir, FILE_NAME)

    /** Dosyadan oku. Dosya yoksa veya bozuksa boş veri döner (uygulama çökmez). */
    fun load(context: Context): AppData {
        val f = file(context)
        if (!f.exists()) return AppData()
        return try {
            decode(f.readText())
        } catch (e: Exception) {
            Log.e(TAG, "Veri okunamadı, boş veriyle devam ediliyor", e)
            AppData()
        }
    }

    /** Dosyaya yaz. Önce geçici dosyaya yazılır, sonra yerine taşınır (yarım kalma riski olmasın). */
    fun save(context: Context, data: AppData) {
        try {
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(encode(data))
            val target = file(context)
            if (target.exists()) target.delete()
            if (!tmp.renameTo(target)) {
                // Taşıma başarısız olursa doğrudan yaz
                target.writeText(encode(data))
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Veri kaydedilemedi", e)
        }
    }

    /** Tüm verileri kalıcı olarak siler. */
    fun wipe(context: Context) {
        file(context).delete()
        File(context.filesDir, "$FILE_NAME.tmp").delete()
    }

    // ------------------------------------------------------------------
    // JSON dönüşümleri
    // ------------------------------------------------------------------

    fun encode(data: AppData): String {
        val root = JSONObject()
        root.put("version", FORMAT_VERSION)
        root.put("uygulama", "Aşk Takvimi")

        val periods = JSONArray()
        data.periods.sortedBy { it.start }.forEach { p ->
            periods.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("baslangic", p.start.toString())
                    p.end?.let { put("bitis", it.toString()) }
                }
            )
        }
        root.put("donemler", periods)

        val logs = JSONArray()
        data.logs.values.sortedBy { it.date }.forEach { l ->
            logs.put(
                JSONObject().apply {
                    put("tarih", l.date.toString())
                    put("kanama", l.flow.name)
                    put("belirtiler", JSONArray().also { arr ->
                        l.symptoms.forEach { s -> arr.put(s.name) }
                    })
                    l.mood?.let { put("ruhHali", it.name) }
                    l.sleep?.let { put("uyku", it.name) }
                    l.appetite?.let { put("istah", it.name) }
                    if (l.note.isNotBlank()) put("not", l.note)
                }
            )
        }
        root.put("gunlukKayitlar", logs)

        root.put("ayarlar", encodeSettings(data.settings))
        return root.toString(2)
    }

    private fun encodeSettings(s: AppSettings): JSONObject = JSONObject().apply {
        put("kurulumTamam", s.onboardingDone)
        put("isim", s.partnerName)
        put("ortalamaDonguUzunlugu", s.avgCycleLength)
        put("ortalamaReglSuresi", s.avgPeriodLength)
        put("ogrenilenOrtalamalariKullan", s.useLearnedAverages)
        put("bildirimlerAcik", s.notificationsEnabled)
        put("bildirimSaati", s.reminderHour)
        put("bildirimDakikasi", s.reminderMinute)
        put("bildirimGunleri", JSONArray().also { arr -> s.reminderOffsets.sorted().forEach { arr.put(it) } })
        put("ozelMetinler", JSONObject().also { obj ->
            s.customTexts.forEach { (offset, text) ->
                obj.put(
                    offset.toString(),
                    JSONObject().apply {
                        put("baslik", text.title)
                        put("mesaj", text.body)
                    }
                )
            }
        })
        put("gizlilikModu", s.privacyMode)
        put("romantikMesajlar", s.romanticMessagesEnabled)
        put("gunlukMotivasyon", s.dailyMotivationEnabled)
        put("tema", s.themeMode.name)
        put("kilitAcik", s.lockEnabled)
        s.pinHash?.let { put("pinOzeti", it) }
        s.pinSalt?.let { put("pinTuzu", it) }
        put("biyometrik", s.biometricEnabled)
    }

    fun decode(text: String): AppData {
        val root = JSONObject(text)

        val periods = mutableListOf<Period>()
        root.optJSONArray("donemler")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val start = parseDate(o.optString("baslangic")) ?: continue
                periods.add(
                    Period(
                        id = o.optString("id").ifBlank { "p$i-$start" },
                        start = start,
                        end = parseDate(o.optString("bitis"))
                    )
                )
            }
        }

        val logs = mutableMapOf<LocalDate, DayLog>()
        root.optJSONArray("gunlukKayitlar")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val date = parseDate(o.optString("tarih")) ?: continue
                val symptoms = mutableSetOf<Symptom>()
                o.optJSONArray("belirtiler")?.let { sa ->
                    for (j in 0 until sa.length()) {
                        runCatching { Symptom.valueOf(sa.optString(j)) }.getOrNull()
                            ?.let { symptoms.add(it) }
                    }
                }
                logs[date] = DayLog(
                    date = date,
                    flow = runCatching { Flow.valueOf(o.optString("kanama")) }.getOrDefault(Flow.NONE),
                    symptoms = symptoms,
                    mood = runCatching { Mood.valueOf(o.optString("ruhHali")) }.getOrNull(),
                    sleep = runCatching { SleepQuality.valueOf(o.optString("uyku")) }.getOrNull(),
                    appetite = runCatching { Appetite.valueOf(o.optString("istah")) }.getOrNull(),
                    note = o.optString("not")
                )
            }
        }

        val settings = root.optJSONObject("ayarlar")?.let { decodeSettings(it) } ?: AppSettings()
        return AppData(periods = periods.sortedBy { it.start }, logs = logs, settings = settings)
    }

    private fun decodeSettings(o: JSONObject): AppSettings {
        val d = AppSettings()
        val offsets = mutableSetOf<Int>()
        o.optJSONArray("bildirimGunleri")?.let { arr ->
            for (i in 0 until arr.length()) offsets.add(arr.optInt(i))
        }
        val texts = mutableMapOf<Int, ReminderText>()
        o.optJSONObject("ozelMetinler")?.let { obj ->
            obj.keys().forEach { key ->
                val t = obj.optJSONObject(key) ?: return@forEach
                val offset = key.toIntOrNull() ?: return@forEach
                texts[offset] = ReminderText(t.optString("baslik"), t.optString("mesaj"))
            }
        }
        return AppSettings(
            onboardingDone = o.optBoolean("kurulumTamam", d.onboardingDone),
            partnerName = o.optString("isim", d.partnerName),
            avgCycleLength = o.optInt("ortalamaDonguUzunlugu", d.avgCycleLength),
            avgPeriodLength = o.optInt("ortalamaReglSuresi", d.avgPeriodLength),
            useLearnedAverages = o.optBoolean("ogrenilenOrtalamalariKullan", d.useLearnedAverages),
            notificationsEnabled = o.optBoolean("bildirimlerAcik", d.notificationsEnabled),
            reminderHour = o.optInt("bildirimSaati", d.reminderHour),
            reminderMinute = o.optInt("bildirimDakikasi", d.reminderMinute),
            reminderOffsets = if (offsets.isEmpty()) d.reminderOffsets else offsets,
            customTexts = texts,
            privacyMode = o.optBoolean("gizlilikModu", d.privacyMode),
            romanticMessagesEnabled = o.optBoolean("romantikMesajlar", d.romanticMessagesEnabled),
            dailyMotivationEnabled = o.optBoolean("gunlukMotivasyon", d.dailyMotivationEnabled),
            themeMode = runCatching { ThemeMode.valueOf(o.optString("tema")) }.getOrDefault(d.themeMode),
            lockEnabled = o.optBoolean("kilitAcik", d.lockEnabled),
            pinHash = o.optString("pinOzeti").ifBlank { null },
            pinSalt = o.optString("pinTuzu").ifBlank { null },
            biometricEnabled = o.optBoolean("biyometrik", d.biometricEnabled)
        )
    }

    private fun parseDate(value: String?): LocalDate? {
        if (value.isNullOrBlank() || value == "null") return null
        return runCatching { LocalDate.parse(value) }.getOrNull()
    }
}
