package com.asktakvimi.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.domain.RomanticMessages
import com.asktakvimi.app.ui.components.DateField
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.SoftCard
import com.asktakvimi.app.ui.components.TimeField
import java.time.LocalDate

/**
 * İlk kurulum sihirbazı. Hiçbir alan zorunlu değildir;
 * kullanıcı isterse her şeyi sonradan Ayarlar'dan değiştirebilir.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }

    var name by remember { mutableStateOf("") }
    var lastStart by remember { mutableStateOf<LocalDate?>(null) }
    var cycleLength by remember { mutableIntStateOf(28) }
    var periodLength by remember { mutableIntStateOf(5) }
    var hour by remember { mutableIntStateOf(9) }
    var minute by remember { mutableIntStateOf(0) }
    var notificationsOn by remember { mutableStateOf(true) }

    fun finish() {
        AppRepository.updateSettings { s ->
            s.copy(
                onboardingDone = true,
                partnerName = name.trim(),
                avgCycleLength = cycleLength,
                avgPeriodLength = periodLength,
                reminderHour = hour,
                reminderMinute = minute,
                notificationsEnabled = notificationsOn
            )
        }
        lastStart?.let { AppRepository.startPeriod(it) }
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(16.dp))

            when (step) {
                0 -> {
                    Text(
                        text = "Birkaç küçük soru 🌸",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Tahminlerin sana uygun olması için birkaç bilgi alalım. " +
                            "Hiçbiri zorunlu değil, sonradan değiştirebilirsin.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                    SoftCard {
                        Text(
                            "Sana nasıl sesleneyim? (isteğe bağlı)",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Örn. Aşkım") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                1 -> {
                    Text(
                        text = "Döngü bilgilerin",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(20.dp))
                    SoftCard {
                        Text(
                            "Son regl başlangıcın",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Hatırlamıyorsan boş bırakabilirsin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        DateField(
                            label = "Tarih seç",
                            date = lastStart,
                            onDateSelected = { lastStart = it },
                            maxDate = LocalDate.now()
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    SoftCard {
                        NumberStepper(
                            label = "Ortalama döngü uzunluğu",
                            value = cycleLength,
                            suffix = "gün",
                            range = 21..40,
                            onChange = { cycleLength = it }
                        )
                        Spacer(Modifier.height(16.dp))
                        NumberStepper(
                            label = "Ortalama regl süresi",
                            value = periodLength,
                            suffix = "gün",
                            range = 2..10,
                            onChange = { periodLength = it }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    InfoNote(
                        "Bunlar yalnızca başlangıç varsayımıdır. Sen kayıt ekledikçe " +
                            "uygulama senin gerçek döngünü öğrenir ve tahminleri günceller."
                    )
                }

                else -> {
                    Text(
                        text = "Hatırlatmalar",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(20.dp))
                    SoftCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Bildirimler açık olsun",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Switch(
                                checked = notificationsOn,
                                onCheckedChange = { notificationsOn = it }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Regline 3, 2, 1 gün kala ve tahmini başlangıç günü küçük " +
                                "birer hatırlatma gelir.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        TimeField(
                            label = "Bildirim saati",
                            hour = hour,
                            minute = minute,
                            onTimeSelected = { h, m -> hour = h; minute = m }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    InfoNote(RomanticMessages.FERTILITY_DISCLAIMER)
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 0) {
                    TextButton(onClick = { step-- }) { Text("Geri") }
                } else {
                    Spacer(Modifier.size(1.dp))
                }
                Button(
                    onClick = { if (step < 2) step++ else finish() },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (step < 2) "Devam" else "Hazırım ❤️")
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Adım ${step + 1} / 3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** – / + tuşlarıyla sayı seçimi. */
@Composable
fun NumberStepper(
    label: String,
    value: Int,
    suffix: String,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { if (value > range.first) onChange(value - 1) }
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Azalt")
            }
            Text(
                text = "$value $suffix",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            FilledTonalIconButton(
                onClick = { if (value < range.last) onChange(value + 1) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Artır")
            }
        }
    }
}
