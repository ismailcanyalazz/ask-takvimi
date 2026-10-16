package com.asktakvimi.app

import android.app.Application
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.notifications.Notifier
import com.asktakvimi.app.notifications.ReminderScheduler

/**
 * Uygulama açılışında:
 *  1. Veriler diskten yüklenir,
 *  2. Bildirim kanalları oluşturulur,
 *  3. Veri her değiştiğinde alarmların yenilenmesi bağlanır.
 */
class AskTakvimiApp : Application() {

    override fun onCreate() {
        super.onCreate()

        AppRepository.init(this)
        Notifier.createChannels(this)

        // Regl tarihi veya ayarlar değişir değişmez eski alarmlar iptal edilip
        // yenileri kurulur.
        AppRepository.onDataChanged = { data ->
            ReminderScheduler.rescheduleAll(this, data)
        }

        ReminderScheduler.rescheduleAll(this, AppRepository.data.value)
    }
}
