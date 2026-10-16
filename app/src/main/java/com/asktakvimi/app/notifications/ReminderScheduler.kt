package com.asktakvimi.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.domain.CycleEngine
import com.asktakvimi.app.domain.RomanticMessages
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Hatırlatmaların zamanlanması.
 *
 * Her hatırlatma için ayrı bir alarm kurulur. Veri her değiştiğinde
 * ([AppRepository.onDataChanged]) tüm alarmlar iptal edilip yeniden kurulur;
 * böylece regl tarihi değişince eski bildirimler asla gelmez.
 *
 * Not: Android, pil tasarrufu nedeniyle alarmları birkaç dakika
 * geciktirebilir. Kesin alarm izni yoksa esnek alarm kullanılır.
 */
object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    const val ACTION_REMINDER = "com.asktakvimi.app.action.REMINDER"
    const val ACTION_MOTIVATION = "com.asktakvimi.app.action.MOTIVATION"
    const val EXTRA_OFFSET = "offset"
    const val EXTRA_TARGET_DATE = "target_date"

    private const val REQUEST_BASE = 1000
    private const val REQUEST_MOTIVATION = 2000

    /** Tüm alarmları iptal edip güncel tahminlere göre yeniden kurar. */
    fun rescheduleAll(context: Context, data: AppData) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        cancelAll(context, alarmManager)

        val settings = data.settings
        if (!settings.notificationsEnabled) {
            Log.i(TAG, "Bildirimler kapalı, alarm kurulmadı.")
            return
        }

        val today = LocalDate.now()
        val prediction = CycleEngine.predict(data.periods, settings, today)
        val now = LocalDateTime.now()

        prediction.predictedNextStart?.let { nextStart ->
            var targetStart = nextStart
            // Bu döngünün tüm hatırlatma saatleri geçtiyse bir sonraki döngüye geç.
            if (allTriggersPassed(targetStart, settings.reminderOffsets, settings, now)) {
                targetStart = nextStart.plusDays(prediction.cycleLengthUsed.toLong())
            }

            settings.reminderOffsets.filter { it in RomanticMessages.OFFSETS }.forEach { offset ->
                val triggerAt = targetStart.minusDays(offset.toLong())
                    .atTime(settings.reminderHour, settings.reminderMinute)
                if (triggerAt.isAfter(now)) {
                    schedule(
                        context = context,
                        alarmManager = alarmManager,
                        at = triggerAt,
                        requestCode = REQUEST_BASE + offset,
                        intent = reminderIntent(context, offset, targetStart.minusDays(offset.toLong()))
                    )
                }
            }
        }

        if (settings.dailyMotivationEnabled) {
            var at = LocalDate.now().atTime(settings.reminderHour, settings.reminderMinute)
            if (!at.isAfter(now)) at = at.plusDays(1)
            schedule(
                context = context,
                alarmManager = alarmManager,
                at = at,
                requestCode = REQUEST_MOTIVATION,
                intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_MOTIVATION)
            )
        }
    }

    private fun allTriggersPassed(
        start: LocalDate,
        offsets: Set<Int>,
        settings: com.asktakvimi.app.data.AppSettings,
        now: LocalDateTime
    ): Boolean = offsets.filter { it in RomanticMessages.OFFSETS }.all { offset ->
        !start.minusDays(offset.toLong())
            .atTime(settings.reminderHour, settings.reminderMinute)
            .isAfter(now)
    }

    private fun reminderIntent(context: Context, offset: Int, date: LocalDate): Intent =
        Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_OFFSET, offset)
            .putExtra(EXTRA_TARGET_DATE, date.toString())

    private fun schedule(
        context: Context,
        alarmManager: AlarmManager,
        at: LocalDateTime,
        requestCode: Int,
        intent: Intent
    ) {
        val triggerMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (canScheduleExact(context, alarmManager)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pending
                )
            } else {
                // Kesin alarm izni yoksa uygulama çökmez; yaklaşık zamanlı alarm kurulur.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pending
                )
            }
            Log.i(TAG, "Alarm kuruldu: $at (kod $requestCode)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Alarm kurulamadı", e)
        }
    }

    /** Android 12+ sürümlerinde kesin alarm izni kullanıcı tarafından kapatılmış olabilir. */
    fun canScheduleExact(context: Context, alarmManager: AlarmManager? = null): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = alarmManager ?: context.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun cancelAll(context: Context, alarmManager: AlarmManager) {
        RomanticMessages.OFFSETS.forEach { offset ->
            cancel(context, alarmManager, REQUEST_BASE + offset, ACTION_REMINDER)
        }
        cancel(context, alarmManager, REQUEST_MOTIVATION, ACTION_MOTIVATION)
    }

    private fun cancel(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        action: String
    ) {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
