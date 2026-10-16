package com.asktakvimi.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.asktakvimi.app.data.AppRepository
import com.asktakvimi.app.notifications.Notifier
import com.asktakvimi.app.ui.MainScaffold
import com.asktakvimi.app.ui.screens.LockScreen
import com.asktakvimi.app.ui.screens.OnboardingScreen
import com.asktakvimi.app.ui.screens.SplashScreen
import com.asktakvimi.app.ui.theme.AskTakvimiTheme
import kotlinx.coroutines.delay

/**
 * Uygulamanın tek Activity'si.
 *
 * FragmentActivity'den türetilir çünkü parmak izi/yüz ile kilit açma
 * (BiometricPrompt) bunu gerektirir.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val data by AppRepository.data.collectAsState()
            AskTakvimiTheme(themeMode = data.settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(activity = this)
                }
            }
        }
    }
}

/**
 * Açılış → (kilit) → (ilk kurulum) → ana uygulama akışı.
 */
@Composable
private fun AppRoot(activity: FragmentActivity) {
    val data by AppRepository.data.collectAsState()
    val context = LocalContext.current

    var showSplash by remember { mutableStateOf(true) }
    var unlocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2200)
        showSplash = false
    }

    // Uygulama arka plana atıldığında kilit yeniden devreye girer.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) unlocked = false
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Sonuç ne olursa olsun uygulama çalışmaya devam eder. */ }

    val requestNotificationPermission: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Kurulum tamamlandıysa ve izin yoksa bir kez nazikçe iste.
    LaunchedEffect(data.settings.onboardingDone, data.settings.notificationsEnabled) {
        if (data.settings.onboardingDone &&
            data.settings.notificationsEnabled &&
            !Notifier.hasPermission(context)
        ) {
            requestNotificationPermission()
        }
    }

    when {
        showSplash -> SplashScreen()

        data.settings.lockEnabled && !unlocked -> LockScreen(
            settings = data.settings,
            activity = activity,
            onUnlocked = { unlocked = true }
        )

        !data.settings.onboardingDone -> OnboardingScreen(
            onFinished = { requestNotificationPermission() }
        )

        else -> MainScaffold(
            data = data,
            onRequestNotificationPermission = requestNotificationPermission
        )
    }
}
