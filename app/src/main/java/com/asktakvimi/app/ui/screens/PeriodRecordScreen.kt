package com.asktakvimi.app.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.DateField
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.SoftCard
import com.asktakvimi.app.ui.components.formatLongDate
import java.time.LocalDate

/**
 * Regl başlangıcı ve bitişinin kaydedildiği, geçmiş kayıtların
 * düzenlenip silinebildiği ekran.
 */
@Composable
fun PeriodRecordScreen(data: AppData, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    var customStart by remember { mutableStateOf<LocalDate?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    val openPeriod = data.periods.sortedBy { it.start }.lastOrNull { it.end == null }

    ScreenScaffold(title = "Regl kaydı", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            SoftCard {
                CardTitle("Yeni regl başlangıcı", Icons.Filled.WaterDrop)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { AppRepository.startPeriod(today) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Text("Reglim bugün başladı")
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Başka bir gün başladıysa tarihi seçebilirsin:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                DateField(
                    label = "Başlangıç tarihi seç",
                    date = customStart,
                    onDateSelected = {
                        customStart = it
                        AppRepository.startPeriod(it)
                    },
                    maxDate = today
                )
            }

            Spacer(Modifier.height(16.dp))

            if (openPeriod != null) {
                SoftCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    CardTitle("Devam eden dönem")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${formatLongDate(openPeriod.start)} tarihinde başladı, " +
                            "bitiş günü henüz işaretlenmedi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { AppRepository.setPeriodEnd(openPeriod.id, today) },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reglim bugün bitti")
                    }
                    Spacer(Modifier.height(10.dp))
                    DateField(
                        label = "Başka bir bitiş tarihi seç",
                        date = null,
                        onDateSelected = { AppRepository.setPeriodEnd(openPeriod.id, it) },
                        minDate = openPeriod.start,
                        maxDate = today
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            SoftCard {
                CardTitle("Kayıtlı dönemler")
                Spacer(Modifier.height(8.dp))

                if (data.periods.isEmpty()) {
                    Text(
                        "Henüz kayıt yok.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                data.periods.sortedByDescending { it.start }.forEach { period ->
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    formatLongDate(period.start),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    period.end?.let {
                                        "Bitiş: ${formatLongDate(it)} · " +
                                            "${period.lengthInDays} gün"
                                    } ?: "Bitiş işaretlenmedi",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = {
                                editingId = if (editingId == period.id) null else period.id
                            }) {
                                Text(if (editingId == period.id) "Kapat" else "Düzenle")
                            }
                        }

                        if (editingId == period.id) {
                            Spacer(Modifier.height(8.dp))
                            DateField(
                                label = "Başlangıç",
                                date = period.start,
                                onDateSelected = {
                                    AppRepository.updatePeriod(period.id, it, period.end)
                                },
                                maxDate = today
                            )
                            Spacer(Modifier.height(8.dp))
                            DateField(
                                label = "Bitiş",
                                date = period.end,
                                onDateSelected = {
                                    AppRepository.updatePeriod(period.id, period.start, it)
                                },
                                minDate = period.start,
                                maxDate = today
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { deleteId = period.id },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(Modifier.height(4.dp))
                                Text("Bu dönemi sil")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            InfoNote(
                "Yeni bir başlangıç kaydettiğinde tahminler ve bildirimler " +
                    "otomatik olarak güncellenir; eski bildirimler iptal edilir."
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text("Dönem silinsin mi?") },
            text = { Text("Bu kayıt kalıcı olarak silinecek.") },
            confirmButton = {
                TextButton(onClick = {
                    AppRepository.deletePeriod(id)
                    deleteId = null
                    editingId = null
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { deleteId = null }) { Text("Vazgeç") }
            }
        )
    }
}
