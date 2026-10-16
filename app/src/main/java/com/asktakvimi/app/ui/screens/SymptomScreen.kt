package com.asktakvimi.app.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.data.Appetite
import com.asktakvimi.app.data.DayLog
import com.asktakvimi.app.data.Flow
import com.asktakvimi.app.data.Mood
import com.asktakvimi.app.data.SleepQuality
import com.asktakvimi.app.data.Symptom
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.SoftCard
import com.asktakvimi.app.ui.components.formatLongDate
import java.time.LocalDate

/**
 * Günlük belirti kayıt ekranı.
 * Her şey tek dokunuşla seçilir; hiçbir alan zorunlu değildir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomScreen(data: AppData, date: LocalDate, onBack: () -> Unit) {
    val existing = remember(data, date) { data.logs[date] ?: DayLog(date) }

    var flow by remember(existing) { mutableStateOf(existing.flow) }
    var symptoms by remember(existing) { mutableStateOf(existing.symptoms) }
    var mood by remember(existing) { mutableStateOf(existing.mood) }
    var sleep by remember(existing) { mutableStateOf(existing.sleep) }
    var appetite by remember(existing) { mutableStateOf(existing.appetite) }
    var note by remember(existing) { mutableStateOf(existing.note) }

    ScreenScaffold(title = formatLongDate(date), onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            SoftCard {
                CardTitle("Kanama")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Flow.entries.forEach { option ->
                        ChoiceChip(
                            label = option.label,
                            selected = flow == option,
                            onClick = { flow = option }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SoftCard {
                CardTitle("Belirtiler")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Symptom.entries.forEach { option ->
                        ChoiceChip(
                            label = option.label,
                            selected = option in symptoms,
                            onClick = {
                                symptoms = if (option in symptoms) symptoms - option
                                else symptoms + option
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SoftCard {
                CardTitle("Ruh hali")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Mood.entries.forEach { option ->
                        ChoiceChip(
                            label = "${option.emoji} ${option.label}",
                            selected = mood == option,
                            onClick = { mood = if (mood == option) null else option }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SoftCard {
                CardTitle("Uyku")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepQuality.entries.forEach { option ->
                        ChoiceChip(
                            label = option.label,
                            selected = sleep == option,
                            onClick = { sleep = if (sleep == option) null else option }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                CardTitle("İştah")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Appetite.entries.forEach { option ->
                        ChoiceChip(
                            label = option.label,
                            selected = appetite == option,
                            onClick = { appetite = if (appetite == option) null else option }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            SoftCard {
                CardTitle("Kişisel not")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Bugün nasıl hissettin?") },
                    shape = RoundedCornerShape(16.dp),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    AppRepository.saveLog(
                        DayLog(
                            date = date,
                            flow = flow,
                            symptoms = symptoms,
                            mood = mood,
                            sleep = sleep,
                            appetite = appetite,
                            note = note
                        )
                    )
                    onBack()
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("Kaydet")
            }

            if (data.logs.containsKey(date)) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        AppRepository.deleteLog(date)
                        onBack()
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Bu günün kaydını sil")
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

/** Seçilebilir küçük etiket. */
@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
        shape = RoundedCornerShape(14.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
