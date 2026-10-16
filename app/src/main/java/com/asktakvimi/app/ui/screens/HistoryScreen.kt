package com.asktakvimi.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.domain.CycleEngine
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.LabeledValue
import com.asktakvimi.app.ui.components.SoftCard
import com.asktakvimi.app.ui.components.formatLongDate
import java.time.LocalDate

/**
 * Geçmiş regl dönemleri ve günlük kayıtların listesi.
 */
@Composable
fun HistoryScreen(
    data: AppData,
    onOpenStats: () -> Unit,
    onEditPeriods: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val periods = remember(data) { data.periods.sortedByDescending { it.start } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        SoftCard {
            CardTitle("Özet", Icons.Filled.History)
            Spacer(Modifier.height(8.dp))
            LabeledValue("Kayıtlı regl dönemi", "${data.periods.size}")
            LabeledValue("Günlük kayıt", "${data.logs.size}")
            LabeledValue(
                "Ortalama döngü",
                "${CycleEngine.averageCycleLength(data.periods, data.settings.avgCycleLength)} gün"
            )
            LabeledValue(
                "Ortalama regl süresi",
                "${CycleEngine.averagePeriodLength(data.periods, data.settings.avgPeriodLength)} gün"
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenStats,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null)
                    Spacer(Modifier.height(4.dp))
                    Text("İstatistikler")
                }
                OutlinedButton(
                    onClick = onEditPeriods,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(Modifier.height(4.dp))
                    Text("Düzenle")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SoftCard {
            CardTitle("Geçmiş regl dönemleri")
            Spacer(Modifier.height(8.dp))
            if (periods.isEmpty()) {
                Text(
                    "Henüz kayıtlı dönem yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            periods.forEachIndexed { index, period ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = period.end?.let {
                            "${formatLongDate(period.start)} – ${formatLongDate(it)}"
                        } ?: "${formatLongDate(period.start)} – devam ediyor",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val details = buildString {
                        period.lengthInDays?.let { append("$it gün sürdü") }
                        val previous = periods.getOrNull(index + 1)
                        val gap = previous?.let {
                            (period.start.toEpochDay() - it.start.toEpochDay()).toInt()
                        }
                        if (gap != null) {
                            if (isNotEmpty()) append(" · ")
                            append("bir önceki dönemden $gap gün sonra")
                        }
                    }
                    if (details.isNotBlank()) {
                        Text(
                            details,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < periods.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        SoftCard {
            CardTitle("Günlük kayıtlar")
            Spacer(Modifier.height(8.dp))
            val logs = data.logs.values.sortedByDescending { it.date }.take(30)
            if (logs.isEmpty()) {
                Text(
                    "Henüz belirti kaydı yok.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            logs.forEach { log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDay(log.date) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            formatLongDate(log.date) +
                                if (log.date == today) " (bugün)" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            buildString {
                                append("Kanama: ${log.flow.label}")
                                log.mood?.let { append(" · ${it.emoji} ${it.label}") }
                                if (log.symptoms.isNotEmpty()) {
                                    append(" · ${log.symptoms.size} belirti")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Düzenle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}
