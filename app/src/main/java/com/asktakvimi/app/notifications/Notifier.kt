package com.asktakvimi.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.asktakvimi.app.MainActivity
import com.asktakvimi.app.R
import com.asktakvimi.app.data.AppSettings
import com.asktakvimi.app.data.ReminderText
import com.asktakvimi.app.domain.RomanticMessages

/**
 * Bildirimlerin oluşturulup gösterilmesi.
 */
object Notifier {

    const val CHANNEL_ID = "regl_hatirlatmalari"
    const val CHANNEL_MOTIVATION_ID = "gunluk_mesajlar"

    private const val NOTIFICATION_ID_BASE = 4100
    private const val NOTIFICATION_ID_MOTIVATION = 4200

    /** Uygulama açılışında bir kez çağrılır. Kanal zaten varsa bir şey olmaz. */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val reminders = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableLights(true)
            enableVibration(true)
        }
        manager.createNotificationChannel(reminders)

        val motivation = NotificationChannel(
            CHANNEL_MOTIVATION_ID,
            "Günlük sevgi mesajı",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Her gün gönderilen kısa, moral veren mesaj"
        }
        manager.createNotificationChannel(motivation)
    }

    /** Bildirim gösterme izni var mı? (Android 13 öncesinde her zaman true) */
    fun hasPermission(context: Context): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Ayarlara göre gösterilecek metni seçer:
     * gizlilik modu açıksa sağlık bilgisi içermeyen metin kullanılır.
     */
    fun textFor(settings: AppSettings, offset: Int): ReminderText {
        if (settings.privacyMode) return RomanticMessages.privateReminder()
        val custom = settings.customTexts[offset]
        if (custom != null && custom.title.isNotBlank() && custom.body.isNotBlank()) return custom
        return RomanticMessages.defaultReminder(offset)
    }

    /** Regl hatırlatması gösterir. */
    fun showReminder(context: Context, settings: AppSettings, offset: Int) {
        val text = textFor(settings, offset)
        val notification = baseBuilder(context, CHANNEL_ID, settings)
            .setContentTitle(text.title)
            .setContentText(text.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        notify(context, NOTIFICATION_ID_BASE + offset, notification)
    }

    /** Günlük motivasyon mesajı gösterir. */
    fun showMotivation(context: Context, settings: AppSettings, message: String) {
        val notification = baseBuilder(context, CHANNEL_MOTIVATION_ID, settings)
            .setContentTitle("Günaydın 🌷")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        notify(context, NOTIFICATION_ID_MOTIVATION, notification)
    }

    private fun baseBuilder(
        context: Context,
        channelId: String,
        settings: AppSettings
    ): NotificationCompat.Builder {
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pending)
            .setAutoCancel(true)
            // Gizlilik modunda bildirim kilit ekranında gizlenir.
            .setVisibility(
                if (settings.privacyMode) NotificationCompat.VISIBILITY_PRIVATE
                else NotificationCompat.VISIBILITY_PUBLIC
            )
    }

    private fun notify(context: Context, id: Int, notification: Notification) {
        if (!hasPermission(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // İzin sonradan kapatılmış olabilir; uygulama çökmemeli.
        }
    }
}
