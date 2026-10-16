package com.asktakvimi.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.domain.RomanticMessages
import java.time.LocalDate

/**
 * Alarm tetiklendiğinde bildirimi gösterir.
 *
 * Aynı bildirimin aynı gün tekrar tekrar gönderilmemesi için
 * gösterilen her bildirim küçük bir kayıtla işaretlenir.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Uygulama süreci yeni başlamış olabilir; veriyi diskten yükle.
        AppRepository.ensureLoaded(context)
        val data = AppRepository.data.value
        val settings = data.settings
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        when (intent.action) {
            ReminderScheduler.ACTION_REMINDER -> {
                val offset = intent.getIntExtra(ReminderScheduler.EXTRA_OFFSET, 0)
                val targetDate = intent.getStringExtra(ReminderScheduler.EXTRA_TARGET_DATE)
                    ?: LocalDate.now().toString()
                val key = "gosterildi_${offset}_$targetDate"

                val allowed = settings.notificationsEnabled && offset in settings.reminderOffsets
                if (allowed && !prefs.getBoolean(key, false)) {
                    Notifier.showReminder(context, settings, offset)
                    prefs.edit().putBoolean(key, true).apply()
                }
            }

            ReminderScheduler.ACTION_MOTIVATION -> {
                val today = LocalDate.now()
                val key = "motivasyon_$today"
                if (settings.dailyMotivationEnabled && !prefs.getBoolean(key, false)) {
                    Notifier.showMotivation(
                        context,
                        settings,
                        RomanticMessages.dailyMotivation(today)
                    )
                    prefs.edit().putBoolean(key, true).apply()
                }
            }
        }

        prune(prefs)

        // Sıradaki hatırlatmayı kur.
        ReminderScheduler.rescheduleAll(context, AppRepository.data.value)
    }

    /** Eski "gösterildi" işaretleri birikmesin. */
    private fun prune(prefs: android.content.SharedPreferences) {
        if (prefs.all.size > 60) prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS = "hatirlatma_durumu"
    }
}
