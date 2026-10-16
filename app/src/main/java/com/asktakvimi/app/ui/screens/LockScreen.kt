package com.asktakvimi.app.ui.screens

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.asktakvimi.app.data.AppSettings
import com.asktakvimi.app.data.PinUtil

/**
 * PIN veya parmak izi ile kilit açma ekranı.
 */
@Composable
fun LockScreen(
    settings: AppSettings,
    activity: FragmentActivity,
    onUnlocked: () -> Unit
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submit(pin: String) {
        if (PinUtil.verify(pin, settings)) {
            error = false
            onUnlocked()
        } else {
            error = true
            entered = ""
        }
    }

    fun showBiometric() {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    onUnlocked()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Aşk Takvimi")
            .setSubtitle("Devam etmek için kimliğini doğrula")
            .setNegativeButtonText("PIN kullan")
            .build()
        runCatching { prompt.authenticate(info) }
    }

    // Biyometrik açıksa ekran açılır açılmaz bir kez sorulur.
    LaunchedEffect(Unit) {
        if (settings.biometricEnabled) showBiometric()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text(
            "Aşk Takvimi kilitli",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (error) "PIN yanlış, tekrar dene." else "Devam etmek için PIN'ini gir.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (error) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Girilen hane sayısı kadar nokta
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(8) { index ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (index < entered.length) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape
                        )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Tuş takımı
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("sil", "0", "tamam")
        )
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    KeypadKey(
                        key = key,
                        onClick = {
                            when (key) {
                                "sil" -> if (entered.isNotEmpty()) entered = entered.dropLast(1)
                                "tamam" -> if (entered.length >= 4) submit(entered)
                                else -> if (entered.length < 8) {
                                    entered += key
                                    error = false
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (settings.biometricEnabled) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showBiometric() }) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Parmak izi ile aç")
            }
        }
    }
}

@Composable
private fun KeypadKey(key: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(
                if (key == "tamam") MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "sil" -> Icon(
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            "tamam" -> Text(
                "Aç",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            else -> Text(
                key,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
