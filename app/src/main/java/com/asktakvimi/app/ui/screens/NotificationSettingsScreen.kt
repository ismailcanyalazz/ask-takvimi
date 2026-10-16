package com.asktakvimi.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.data.ReminderText
import com.asktakvimi.app.domain.RomanticMessages
import com.asktakvimi.app.notifications.Notifier
import com.asktakvimi.app.notifications.ReminderScheduler
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.SoftCard
import com.asktakvimi.app.ui.components.TimeField

/**
 * Bildirim ayarları: açma/kapama, saat, hangi günler, metin kişiselleştirme.
 */
@Composable
fun NotificationSettingsScreen(
    data: AppData,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val settings = data.settings
    var editingOffset by remember { mutableStateOf<Int?>(null) }

    val hasPermission = Notifier.hasPermission(context)
    val canExact = ReminderScheduler.canScheduleExact(context)

    ScreenScaffold(title = "Bildirim ayarları", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            // ---- İzin uyarısı ----
            if (!hasPermission) {
                SoftCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    CardTitle("Bildirim izni kapalı", Icons.Filled.NotificationsActive)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Telefonun bildirim izni verilmediği için hatırlatmalar " +
                            "ekranda görünmez. İzni vermek birkaç saniye sürer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRequestPermission,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("İzin ver") }
                        OutlinedButton(
                            onClick = { openAppSettings(context) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Ayarları aç") }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ---- Ana anahtar ----
            SoftCard {
                SwitchRow(
                    title = "Hatırlatmalar açık",
                    subtitle = "Kapatırsan hiçbir bildirim gönderilmez.",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { value ->
                        AppRepository.updateSettings { it.copy(notificationsEnabled = value) }
                    }
                )
                Spacer(Modifier.height(16.dp))
                TimeField(
                    label = "Bildirim saati",
                    hour = settings.reminderHour,
                    minute = settings.reminderMinute,
                    onTimeSelected = { h, m ->
                        AppRepository.updateSettings {
                            it.copy(reminderHour = h, reminderMinute = m)
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- Hangi günler ----
            SoftCard {
                CardTitle("Hangi günler hatırlatayım?")
                Spacer(Modifier.height(6.dp))
                RomanticMessages.OFFSETS.forEach { offset ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = offset in settings.reminderOffsets,
                            onCheckedChange = { checked ->
                                AppRepository.updateSettings { s ->
                                    val updated = s.reminderOffsets.toMutableSet()
                                    if (checked) updated.add(offset) else updated.remove(offset)
                                    s.copy(reminderOffsets = updated)
                                }
                            }
                        )
                        Text(
                            RomanticMessages.offsetLabel(offset),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            editingOffset = if (editingOffset == offset) null else offset
                        }) {
                            Text(if (editingOffset == offset) "Kapat" else "Metni düzenle")
                        }
                    }

                    if (editingOffset == offset) {
                        MessageEditor(
                            offset = offset,
                            current = settings.customTexts[offset]
                                ?: RomanticMessages.defaultReminder(offset),
                            onSave = { text ->
                                AppRepository.updateSettings { s ->
                                    s.copy(customTexts = s.customTexts + (offset to text))
                                }
                                editingOffset = null
                            },
                            onReset = {
                                AppRepository.updateSettings { s ->
                                    s.copy(customTexts = s.customTexts - offset)
                                }
                                editingOffset = null
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Günlük mesaj ----
            SoftCard {
                SwitchRow(
                    title = "Günlük sevgi mesajı",
                    subtitle = "Her gün aynı saatte kısa bir mesaj gönderir.",
                    checked = settings.dailyMotivationEnabled,
                    onCheckedChange = { value ->
                        AppRepository.updateSettings { it.copy(dailyMotivationEnabled = value) }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ---- Deneme ----
            SoftCard {
                CardTitle("Deneme bildirimi")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bildirimlerin nasıl göründüğünü hemen görmek için deneyebilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { Notifier.showReminder(context, settings, 3) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Deneme bildirimi gönder") }
            }

            Spacer(Modifier.height(16.dp))

            // ---- Kesin zamanlama bilgisi ----
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canExact) {
                SoftCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    CardTitle("Zamanlama izni")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Telefonun \"kesin alarm\" iznini vermediğin için bildirimler " +
                            "seçtiğin saatten biraz sonra gelebilir. İzni verirsen " +
                            "daha isabetli olur.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { openExactAlarmSettings(context) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Zamanlama ayarını aç") }
                }
                Spacer(Modifier.height(16.dp))
            }

            InfoNote(
                "Android, pil tasarrufu için bildirimleri birkaç dakika " +
                    "geciktirebilir; bu yüzden dakikası dakikasına teslim garanti edilemez. " +
                    "Telefonun pil optimizasyonu listesinde bu uygulamayı " +
                    "\"kısıtlama\" dışında bırakırsan hatırlatmalar daha güvenilir olur."
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MessageEditor(
    offset: Int,
    current: ReminderText,
    onSave: (ReminderText) -> Unit,
    onReset: () -> Unit
) {
    var title by remember(offset) { mutableStateOf(current.title) }
    var body by remember(offset) { mutableStateOf(current.body) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Başlık") },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Mesaj") },
            minLines = 3,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onSave(ReminderText(title.trim(), body.trim())) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Kaydet") }
            OutlinedButton(
                onClick = onReset,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) { Text("Varsayılana dön") }
        }
    }
}

/** Başlık + açıklama + anahtar satırı. */
@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun openAppSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(intent) }
}

private fun openExactAlarmSettings(context: android.content.Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    runCatching { context.startActivity(intent) }
}
