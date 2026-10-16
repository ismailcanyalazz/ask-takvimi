package com.asktakvimi.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.data.ThemeMode
import com.asktakvimi.app.domain.RomanticMessages
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.SoftCard

/**
 * Uygulama ayarları (alt menüdeki "Ayarlar" sekmesi).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    data: AppData,
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenMessages: () -> Unit
) {
    val settings = data.settings
    var name by remember(settings.partnerName) { mutableStateOf(settings.partnerName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        SoftCard {
            CardTitle("Görünüm", Icons.Filled.Palette)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    ChoiceChip(
                        label = mode.label,
                        selected = settings.themeMode == mode,
                        onClick = {
                            AppRepository.updateSettings { it.copy(themeMode = mode) }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SoftCard {
            CardTitle("Sana nasıl sesleneyim?", Icons.Filled.Favorite)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    AppRepository.updateSettings { s -> s.copy(partnerName = it.trim()) }
                },
                placeholder = { Text("Örn. Aşkım") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            SwitchRow(
                title = "Romantik mesajlar",
                subtitle = "Ana ekrandaki sevgi dolu cümleleri gösterir.",
                checked = settings.romanticMessagesEnabled,
                onCheckedChange = { value ->
                    AppRepository.updateSettings { it.copy(romanticMessagesEnabled = value) }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        SoftCard {
            CardTitle("Döngü varsayımların")
            Spacer(Modifier.height(10.dp))
            NumberStepper(
                label = "Ortalama döngü uzunluğu",
                value = settings.avgCycleLength,
                suffix = "gün",
                range = 21..40,
                onChange = { value ->
                    AppRepository.updateSettings { it.copy(avgCycleLength = value) }
                }
            )
            Spacer(Modifier.height(16.dp))
            NumberStepper(
                label = "Ortalama regl süresi",
                value = settings.avgPeriodLength,
                suffix = "gün",
                range = 2..10,
                onChange = { value ->
                    AppRepository.updateSettings { it.copy(avgPeriodLength = value) }
                }
            )
            Spacer(Modifier.height(16.dp))
            SwitchRow(
                title = "Kayıtlarımdan öğren",
                subtitle = "Açıkken tahminler senin gerçek döngü geçmişinden hesaplanır.",
                checked = settings.useLearnedAverages,
                onCheckedChange = { value ->
                    AppRepository.updateSettings { it.copy(useLearnedAverages = value) }
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        SoftCard {
            SettingsLink(
                icon = Icons.Filled.NotificationsActive,
                title = "Bildirim ayarları",
                subtitle = "Saat, günler ve mesaj metinleri",
                onClick = onOpenNotifications
            )
            Spacer(Modifier.height(4.dp))
            SettingsLink(
                icon = Icons.Filled.Lock,
                title = "Gizlilik ve kilit",
                subtitle = "PIN, parmak izi ve gizli bildirimler",
                onClick = onOpenPrivacy
            )
            Spacer(Modifier.height(4.dp))
            SettingsLink(
                icon = Icons.Filled.Backup,
                title = "Yedekleme ve veri silme",
                subtitle = "Verilerini dışa aktar veya tamamen sil",
                onClick = onOpenBackup
            )
            Spacer(Modifier.height(4.dp))
            SettingsLink(
                icon = Icons.Filled.Favorite,
                title = "Romantik mesaj koleksiyonu",
                subtitle = "Sana yazılmış küçük notlar",
                onClick = onOpenMessages
            )
        }

        Spacer(Modifier.height(16.dp))

        InfoNote(
            "Aşk Takvimi, verilerini yalnızca bu telefonda saklar. " +
                "Uygulamanın internet izni bile yoktur; reklam, takip veya " +
                "analiz aracı içermez."
        )
        Spacer(Modifier.height(12.dp))
        InfoNote(RomanticMessages.FERTILITY_DISCLAIMER)

        Spacer(Modifier.height(16.dp))
        Text(
            "Aşk Takvimi ❤️ sürüm 1.0",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsLink(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(0.dp))
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
