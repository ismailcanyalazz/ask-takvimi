package com.asktakvimi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.domain.CycleEngine
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.LabeledValue
import com.asktakvimi.app.ui.components.SoftCard
import com.asktakvimi.app.ui.components.formatShortDate

/**
 * İstatistikler: ortalamalar, döngü geçmişindeki değişimler ve
 * en sık görülen belirtiler.
 */
@Composable
fun StatsScreen(data: AppData, onBack: () -> Unit) {
    val lengths = remember(data) { CycleEngine.cycleLengths(data.periods) }
    val avgCycle = remember(data) {
        CycleEngine.averageCycleLength(data.periods, data.settings.avgCycleLength)
    }
    val avgPeriod = remember(data) {
        CycleEngine.averagePeriodLength(data.periods, data.settings.avgPeriodLength)
    }
    val variation = remember(data) { CycleEngine.cycleVariation(data.periods) }
    val irregular = remember(data) { CycleEngine.isIrregular(data.periods) }

    ScreenScaffold(title = "İstatistikler", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            SoftCard {
                CardTitle("Genel", Icons.Filled.BarChart)
                Spacer(Modifier.height(8.dp))
                LabeledValue("Ortalama döngü uzunluğu", "$avgCycle gün")
                LabeledValue("Ortalama regl süresi", "$avgPeriod gün")
                LabeledValue("En kısa döngü", lengths.minOrNull()?.let { "$it gün" } ?: "—")
                LabeledValue("En uzun döngü", lengths.maxOrNull()?.let { "$it gün" } ?: "—")
                LabeledValue("Oynama payı", variation?.let { "$it gün" } ?: "—")
                LabeledValue(
                    "Döngü düzeni",
                    when {
                        lengths.size < 3 -> "Değerlendirmek için az kayıt"
                        irregular -> "Değişken"
                        else -> "Düzenliye yakın"
                    }
                )
            }

            Spacer(Modifier.height(16.dp))

            SoftCard {
                CardTitle("Döngü geçmişindeki değişim")
                Spacer(Modifier.height(10.dp))
                if (lengths.isEmpty()) {
                    Text(
                        "En az iki regl kaydı olduğunda burada döngülerinin " +
                            "nasıl değiştiğini görebileceksin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val periods = data.periods.sortedBy { it.start }
                    val maxLen = (lengths.maxOrNull() ?: 28).coerceAtLeast(1)
                    lengths.takeLast(8).forEachIndexed { index, length ->
                        // Bu döngü hangi dönemle bitiyor?
                        val label = periods.getOrNull(
                            periods.size - lengths.takeLast(8).size + index
                        )?.start?.let { formatShortDate(it) } ?: "—"
                        BarRow(label = label, value = length, maxValue = maxLen)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SoftCard {
                CardTitle("En sık yaşadığın belirtiler")
                Spacer(Modifier.height(10.dp))
                val counts = data.logs.values
                    .flatMap { it.symptoms }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(5)

                if (counts.isEmpty()) {
                    Text(
                        "Henüz belirti kaydı yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val max = counts.first().value
                    counts.forEach { (symptom, count) ->
                        BarRow(label = symptom.label, value = count, maxValue = max, unit = "kez")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SoftCard {
                CardTitle("Ruh hali dağılımı")
                Spacer(Modifier.height(10.dp))
                val moods = data.logs.values.mapNotNull { it.mood }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }

                if (moods.isEmpty()) {
                    Text(
                        "Ruh hali kaydı yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val max = moods.first().value
                    moods.forEach { (mood, count) ->
                        BarRow(
                            label = "${mood.emoji} ${mood.label}",
                            value = count,
                            maxValue = max,
                            unit = "gün"
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            InfoNote(
                "İstatistikler yalnızca senin kaydettiğin verilerden hesaplanır " +
                    "ve cihazından hiçbir yere gönderilmez."
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Basit yatay çubuk. */
@Composable
private fun BarRow(label: String, value: Int, maxValue: Int, unit: String = "gün") {
    val fraction = if (maxValue <= 0) 0f else (value.toFloat() / maxValue).coerceIn(0.05f, 1f)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(110.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(8.dp)
                        )
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
