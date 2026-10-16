package com.asktakvimi.app.ui.screens

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.data.PinUtil
import com.asktakvimi.app.ui.components.CardTitle
import com.asktakvimi.app.ui.components.InfoNote
import com.asktakvimi.app.ui.components.SoftCard

/**
 * Gizlilik ve kilit ayarları.
 */
@Composable
fun PrivacySettingsScreen(data: AppData, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = data.settings
    var showPinDialog by remember { mutableStateOf(false) }

    val biometricAvailable = remember {
        BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    ScreenScaffold(title = "Gizlilik ve kilit", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            SoftCard {
                CardTitle("Bildirimlerde gizlilik", Icons.Filled.VisibilityOff)
                Spacer(Modifier.height(10.dp))
                SwitchRow(
                    title = "Gizli bildirim metni",
                    subtitle = "Açıkken bildirimlerde regl bilgisi yazmaz, " +
                        "yalnızca nazik bir hatırlatma görünür.",
                    checked = settings.privacyMode,
                    onCheckedChange = { value ->
                        AppRepository.updateSettings { it.copy(privacyMode = value) }
                    }
                )
                Spacer(Modifier.height(12.dp))
                SoftCard(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        "Gizlilik modunda bildirim şöyle görünür:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sana küçük bir hatırlatma ❤️",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Bugün kendine biraz zaman ayırmayı unutma. Seni düşünen biri var. 💗",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SoftCard {
                CardTitle("Uygulama kilidi", Icons.Filled.Lock)
                Spacer(Modifier.height(10.dp))
                SwitchRow(
                    title = "PIN ile kilitle",
                    subtitle = "Uygulama her açıldığında PIN sorar.",
                    checked = settings.lockEnabled,
                    onCheckedChange = { value ->
                        if (value) {
                            showPinDialog = true
                        } else {
                            AppRepository.updateSettings {
                                it.copy(
                                    lockEnabled = false,
                                    biometricEnabled = false,
                                    pinHash = null,
                                    pinSalt = null
                                )
                            }
                        }
                    }
                )

                if (settings.lockEnabled) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showPinDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("PIN'i değiştir") }

                    Spacer(Modifier.height(16.dp))
                    SwitchRow(
                        title = "Parmak izi / yüz ile aç",
                        subtitle = if (biometricAvailable) {
                            "Kilidi biyometrik ile de açabilirsin."
                        } else {
                            "Bu telefonda kayıtlı parmak izi/yüz bulunamadı."
                        },
                        checked = settings.biometricEnabled && biometricAvailable,
                        onCheckedChange = { value ->
                            if (biometricAvailable) {
                                AppRepository.updateSettings { it.copy(biometricEnabled = value) }
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            InfoNote(
                "Regl bilgileri özel sağlık verisidir. Bu uygulama verileri yalnızca " +
                    "telefonunun kendi özel klasöründe tutar, buluta yedeklemez ve " +
                    "hiçbir sunucuya göndermez. PIN'in düz metin olarak değil, " +
                    "geri çevrilemez bir özet olarak saklanır."
            )
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                val salt = PinUtil.newSalt()
                AppRepository.updateSettings {
                    it.copy(
                        lockEnabled = true,
                        pinSalt = salt,
                        pinHash = PinUtil.hash(pin, salt)
                    )
                }
                showPinDialog = false
            }
        )
    }
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val valid = pin.length in 4..8 && pin == again

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PIN belirle") },
        text = {
            Column {
                Text(
                    "4–8 haneli bir PIN seç.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = again,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) again = it },
                    label = { Text("PIN (tekrar)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true
                )
                if (pin.isNotEmpty() && again.isNotEmpty() && pin != again) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "PIN'ler aynı değil.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valid) onConfirm(pin) }, enabled = valid) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
