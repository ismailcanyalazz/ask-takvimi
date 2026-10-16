package com.asktakvimi.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.LabeledValue
import com.asktakvimi.app.ui.components.SoftCard
import java.time.LocalDate

/**
 * Verileri dışa aktarma, geri yükleme ve kalıcı silme ekranı.
 */
@Composable
fun BackupScreen(data: AppData, onBack: () -> Unit) {
    val context = LocalContext.current
    var confirmWipe by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(AppRepository.exportJson().toByteArray(Charsets.UTF_8))
            }
        }.isSuccess
        Toast.makeText(
            context,
            if (ok) "Yedek kaydedildi." else "Yedek kaydedilemedi.",
            Toast.LENGTH_SHORT
        ).show()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        val ok = text != null && AppRepository.importJson(text)
        Toast.makeText(
            context,
            if (ok) "Yedek geri yüklendi." else "Dosya okunamadı veya biçim uygun değil.",
            Toast.LENGTH_LONG
        ).show()
    }

    ScreenScaffold(title = "Yedekleme ve veriler", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            SoftCard {
                CardTitle("Verilerin", Icons.Filled.Backup)
                Spacer(Modifier.height(8.dp))
                LabeledValue("Regl dönemi kaydı", "${data.periods.size}")
                LabeledValue("Günlük kayıt", "${data.logs.size}")
            }

            Spacer(Modifier.height(16.dp))

            SoftCard {
                CardTitle("Dışa aktar")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tüm kayıtlarını tek bir dosyaya kaydeder. Telefon değiştirirsen " +
                        "bu dosyayla geri yükleyebilirsin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        exportLauncher.launch("ask-takvimi-yedek-${LocalDate.now()}.json")
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Yedek dosyası oluştur") }

                Spacer(Modifier.height(16.dp))
                CardTitle("Geri yükle")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Daha önce aldığın bir yedeği geri yükler. Mevcut kayıtların " +
                        "yedekteki kayıtlarla değiştirilir.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Yedekten geri yükle") }
            }

            Spacer(Modifier.height(16.dp))

            InfoNote(
                "Yedek dosyası şifrelenmez; içinde sağlık verilerin bulunur. " +
                    "Dosyayı telefonunun kendi hafızasında veya güvendiğin bir yerde " +
                    "sakla, herkese açık paylaşımlardan kaçın."
            )

            Spacer(Modifier.height(24.dp))

            SoftCard(containerColor = MaterialTheme.colorScheme.errorContainer) {
                CardTitle("Tüm verileri sil", Icons.Filled.DeleteForever)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Bütün regl kayıtların, belirtilerin ve ayarların kalıcı olarak " +
                        "silinir. Bu işlem geri alınamaz.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { confirmWipe = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Tüm verilerimi sil") }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text("Emin misin?") },
            text = {
                Text(
                    "Tüm kayıtların kalıcı olarak silinecek ve uygulama ilk " +
                        "kurulum ekranına dönecek."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    AppRepository.wipeAll()
                    confirmWipe = false
                    Toast.makeText(context, "Tüm veriler silindi.", Toast.LENGTH_SHORT).show()
                }) { Text("Evet, sil") }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) { Text("Vazgeç") }
            }
        )
    }
}
