package com.asktakvimi.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.asktakvimi.app.data.AppRepository

/**
 * Cihaz yeniden başladığında, uygulama güncellendiğinde veya saat/zaman dilimi
 * değiştiğinde alarmlar silinir. Bu alıcı hepsini yeniden kurar.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                Log.i("BootReceiver", "Alarmlar yeniden kuruluyor: ${intent.action}")
                AppRepository.ensureLoaded(context)
                Notifier.createChannels(context)
                ReminderScheduler.rescheduleAll(context, AppRepository.data.value)
            }
        }
    }
}
