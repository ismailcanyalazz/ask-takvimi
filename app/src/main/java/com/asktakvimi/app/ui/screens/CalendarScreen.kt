package com.asktakvimi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.domain.CycleEngine
import com.asktakvimi.app.domain.DayKind
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.SoftCard
import com.asktakvimi.app.ui.components.formatLongDate
import com.asktakvimi.app.ui.components.formatMonthTitle
import com.asktakvimi.app.ui.components.shortDayNames
import com.asktakvimi.app.ui.theme.FertileColor
import com.asktakvimi.app.ui.theme.OvulationColor
import com.asktakvimi.app.ui.theme.PeriodActual
import com.asktakvimi.app.ui.theme.PeriodPredicted
import java.time.LocalDate
import java.time.YearMonth

/**
 * Aylık regl takvimi.
 * Gerçek kayıtlar koyu pembe, tahminler açık pembe, yumurtlama lila gösterilir.
 */
@Composable
fun CalendarScreen(
    data: AppData,
    onOpenDay: (LocalDate) -> Unit,
    onRecordPeriod: () -> Unit
) {
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    val prediction = remember(data, today) {
        CycleEngine.predict(data.periods, data.settings, today)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Ay başlığı ve gezinme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Önceki ay")
            }
            Text(
                text = formatMonthTitle(month),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sonraki ay")
            }
        }

        if (month != YearMonth.from(today)) {
            TextButton(onClick = { month = YearMonth.from(today) }) {
                Text("Bu aya dön")
            }
        }

        Spacer(Modifier.height(8.dp))

        SoftCard {
            // Gün adları
            Row(modifier = Modifier.fillMaxWidth()) {
                shortDayNames().forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // Günler ızgarası (Pazartesi ile başlar)
            val firstDay = month.atDay(1)
            val leadingBlanks = firstDay.dayOfWeek.value - 1
            val daysInMonth = month.lengthOfMonth()
            val totalCells = leadingBlanks + daysInMonth
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - leadingBlanks + 1
                        if (dayNumber < 1 || dayNumber > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = month.atDay(dayNumber)
                            DayCell(
                                date = date,
                                kind = CycleEngine.dayKind(date, data.periods, prediction, today),
                                isToday = date == today,
                                isSelected = date == selected,
                                hasLog = data.logs.containsKey(date),
                                modifier = Modifier.weight(1f),
                                onClick = { selected = if (selected == date) null else date }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Legend()
        }

        Spacer(Modifier.height(16.dp))

        // Seçili günün kartı
        selected?.let { date ->
            DayDetailCard(
                date = date,
                data = data,
                today = today,
                onOpenDay = { onOpenDay(date) }
            )
            Spacer(Modifier.height(16.dp))
        }

        OutlinedButton(
            onClick = onRecordPeriod,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Regl başlangıcı / bitişi kaydet")
        }

        Spacer(Modifier.height(16.dp))
        InfoNote(
            "Açık renkli günler tahmindir. Sen kayıt ekledikçe tahminler senin " +
                "gerçek döngüne göre güncellenir."
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    kind: DayKind,
    isToday: Boolean,
    isSelected: Boolean,
    hasLog: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val background = when (kind) {
        DayKind.ACTUAL_PERIOD -> PeriodActual
        DayKind.PREDICTED_PERIOD -> PeriodPredicted
        DayKind.OVULATION -> OvulationColor
        DayKind.FERTILE -> FertileColor
        DayKind.NONE -> Color.Transparent
    }
    val textColor = when (kind) {
        DayKind.ACTUAL_PERIOD, DayKind.OVULATION -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .background(background, CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else if (isToday) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                } else Modifier
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (hasLog) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(textColor, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    Column {
        LegendRow(PeriodActual, "Kayıtlı regl günü")
        LegendRow(PeriodPredicted, "Tahmini regl günü")
        LegendRow(OvulationColor, "Tahmini yumurtlama günü")
        LegendRow(FertileColor, "Tahmini doğurganlık dönemi")
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayDetailCard(
    date: LocalDate,
    data: AppData,
    today: LocalDate,
    onOpenDay: () -> Unit
) {
    val log = data.logs[date]
    val periodOnDay = CycleEngine.periodOn(data.periods, date, today)

    SoftCard {
        CardTitle(formatLongDate(date))
        Spacer(Modifier.height(10.dp))

        if (log == null) {
            Text(
                "Bu güne ait kayıt yok.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                buildString {
                    append("Kanama: ${log.flow.label}")
                    log.mood?.let { append(" · Ruh hali: ${it.label} ${it.emoji}") }
                    if (log.symptoms.isNotEmpty()) {
                        append("\nBelirtiler: ")
                        append(log.symptoms.joinToString(", ") { it.label })
                    }
                    if (log.note.isNotBlank()) append("\nNot: ${log.note}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onOpenDay,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (log == null) "Kayıt ekle" else "Düzenle")
            }
            OutlinedButton(
                onClick = { AppRepository.togglePeriodDay(date) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (data.periods.any { it.start == date }) "Başlangıcı kaldır"
                    else "Regl başlangıcı"
                )
            }
        }

        if (periodOnDay != null && periodOnDay.end == null) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { AppRepository.setPeriodEnd(periodOnDay.id, date) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reglim bu gün bitti olarak işaretle")
            }
        }
    }
}
