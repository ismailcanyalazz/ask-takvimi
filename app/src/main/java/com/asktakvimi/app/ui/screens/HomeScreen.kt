package com.asktakvimi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.domain.CycleEngine
import com.asktakvimi.app.domain.RomanticMessages
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.LabeledValue
import com.asktakvimi.app.ui.components.SoftCard
import com.asktakvimi.app.ui.components.formatLongDate
import java.time.LocalDate

/**
 * Ana sayfa. Tüm bilgiler kullanıcının kendi kayıtlarından hesaplanır;
 * hiçbir gün sayısı sabit yazılmaz.
 */
@Composable
fun HomeScreen(
    data: AppData,
    onOpenCalendar: () -> Unit,
    onRecordPeriod: () -> Unit,
    onAddSymptom: () -> Unit,
    onOpenMessages: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val prediction = remember(data, today) {
        CycleEngine.predict(data.periods, data.settings, today)
    }
    val name = data.settings.partnerName.ifBlank { "aşkım" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        // ---------------- Karşılama kartı ----------------
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Merhaba $name ❤️",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = headlineFor(prediction, today),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                if (data.settings.romanticMessagesEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = RomanticMessages.homeMessage(
                            daysUntil = prediction.daysUntilNextPeriod,
                            isOnPeriod = prediction.isOnPeriodToday
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- Hızlı işlemler ----------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onRecordPeriod,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Regl kaydet")
            }
            OutlinedButton(
                onClick = onAddSymptom,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Icon(Icons.Filled.Spa, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Belirti ekle")
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onOpenCalendar,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text("Regl takvimini aç")
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- Döngü özeti ----------------
        SoftCard {
            CardTitle("Döngü özetin", Icons.Filled.Favorite)
            Spacer(Modifier.height(10.dp))

            if (!prediction.hasData) {
                Text(
                    "Henüz kayıt yok. İlk regl başlangıcını eklediğinde burada " +
                        "tahminlerin görünmeye başlayacak. 🌸",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LabeledValue(
                    "Son regl başlangıcı",
                    prediction.lastPeriodStart?.let { formatLongDate(it) } ?: "—"
                )
                LabeledValue(
                    "Tahmini regl bitişi",
                    prediction.currentPeriodEnd?.let { formatLongDate(it) }
                        ?: prediction.predictedNextEnd?.let { formatLongDate(it) }
                        ?: "—"
                )
                LabeledValue(
                    "Bugün döngünün",
                    prediction.cycleDay?.let { "$it. günü" } ?: "—"
                )
                LabeledValue(
                    "Tahmini yumurtlama",
                    prediction.ovulationDay?.let { formatLongDate(it) } ?: "—"
                )
                LabeledValue(
                    "Tahmini doğurganlık dönemi",
                    if (prediction.fertileStart != null && prediction.fertileEnd != null) {
                        "${formatLongDate(prediction.fertileStart)} – " +
                            formatLongDate(prediction.fertileEnd)
                    } else "—"
                )
                LabeledValue("Kullanılan döngü uzunluğu", "${prediction.cycleLengthUsed} gün")
            }
        }

        Spacer(Modifier.height(16.dp))

        // ---------------- Yaklaşan önemli tarihler ----------------
        if (prediction.hasData) {
            SoftCard {
                CardTitle("Yaklaşan önemli tarihler", Icons.Filled.CalendarMonth)
                Spacer(Modifier.height(10.dp))
                upcomingDates(prediction, today).forEach { (label, date) ->
                    LabeledValue(label, formatLongDate(date))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------------- Günlük mesaj ----------------
        if (data.settings.dailyMotivationEnabled && data.settings.romanticMessagesEnabled) {
            SoftCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                CardTitle("Bugünün mesajı", Icons.Filled.Favorite)
                Spacer(Modifier.height(8.dp))
                Text(
                    RomanticMessages.dailyMotivation(today),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onOpenMessages) { Text("Diğer mesajlar") }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---------------- Uyarılar ----------------
        InfoNote(
            buildString {
                append(prediction.confidence.explanation)
                if (prediction.isIrregular) {
                    append(" Kayıtlarında döngü uzunluğu belirgin şekilde değişiyor, ")
                    append("bu yüzden tahminleri kesin bilgi gibi görme.")
                }
            }
        )

        Spacer(Modifier.height(12.dp))
        InfoNote(RomanticMessages.FERTILITY_DISCLAIMER)
        Spacer(Modifier.height(28.dp))
    }
}

/** Kartın büyük başlığı: kaç gün kaldığını anlatır. */
private fun headlineFor(
    prediction: com.asktakvimi.app.domain.CyclePrediction,
    today: LocalDate
): String {
    if (!prediction.hasData) return "Hadi ilk kaydını ekleyelim 🌷"
    if (prediction.isOnPeriodToday) {
        val day = prediction.lastPeriodStart?.let {
            (today.toEpochDay() - it.toEpochDay()).toInt() + 1
        }
        return if (day != null) "Regl döneminin $day. günündesin" else "Regl dönemindesin"
    }
    val days = prediction.daysUntilNextPeriod ?: return "—"
    return when {
        days == 0 -> "Reglin bugün başlayabilir"
        days > 0 -> "Regline $days gün kaldı"
        else -> "Tahmini tarih ${-days} gün geçti"
    }
}

/** Yaklaşan tarihleri sıralı biçimde verir. */
private fun upcomingDates(
    prediction: com.asktakvimi.app.domain.CyclePrediction,
    today: LocalDate
): List<Pair<String, LocalDate>> {
    val items = mutableListOf<Pair<String, LocalDate>>()
    prediction.ovulationDay?.let {
        if (!it.isBefore(today)) items.add("Tahmini yumurtlama günü" to it)
    }
    prediction.fertileStart?.let {
        if (!it.isBefore(today)) items.add("Doğurganlık dönemi başlangıcı" to it)
    }
    prediction.predictedNextStart?.let { items.add("Tahmini regl başlangıcı" to it) }
    prediction.predictedNextEnd?.let { items.add("Tahmini regl bitişi" to it) }
    return items.sortedBy { it.second }
}
