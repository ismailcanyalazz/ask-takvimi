package com.asktakvimi.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.asktakvimi.app.data.AppData
import com.asktakvimi.app.ui.screens.BackupScreen
import com.asktakvimi.app.ui.screens.CalendarScreen
import com.asktakvimi.app.ui.screens.HistoryScreen
import com.asktakvimi.app.ui.screens.HomeScreen
import com.asktakvimi.app.ui.screens.MessagesScreen
import com.asktakvimi.app.ui.screens.NotificationSettingsScreen
import com.asktakvimi.app.ui.screens.PeriodRecordScreen
import com.asktakvimi.app.ui.screens.PrivacySettingsScreen
import com.asktakvimi.app.ui.screens.SettingsScreen
import com.asktakvimi.app.ui.screens.StatsScreen
import com.asktakvimi.app.ui.screens.SymptomScreen
import java.time.LocalDate

/** Alt menüdeki sekmeler. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "Ana Sayfa", Icons.Filled.Favorite),
    CALENDAR("calendar", "Takvim", Icons.Filled.CalendarMonth),
    HISTORY("history", "Geçmiş", Icons.Filled.History),
    SETTINGS("settings", "Ayarlar", Icons.Filled.Settings)
}

object Routes {
    const val PERIOD_RECORD = "period_record"
    const val SYMPTOM = "symptom"
    const val STATS = "stats"
    const val NOTIFICATIONS = "notification_settings"
    const val PRIVACY = "privacy_settings"
    const val BACKUP = "backup"
    const val MESSAGES = "messages"

    fun symptomFor(date: LocalDate) = "$SYMPTOM/$date"
}

/**
 * Uygulamanın ana iskeleti: alt menü + ekran geçişleri.
 */
@Composable
fun MainScaffold(
    data: AppData,
    onRequestNotificationPermission: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = Tab.entries.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { tab ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Tab.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Tab.HOME.route) {
                HomeScreen(
                    data = data,
                    onOpenCalendar = { navController.navigate(Tab.CALENDAR.route) },
                    onRecordPeriod = { navController.navigate(Routes.PERIOD_RECORD) },
                    onAddSymptom = { navController.navigate(Routes.symptomFor(LocalDate.now())) },
                    onOpenMessages = { navController.navigate(Routes.MESSAGES) }
                )
            }

            composable(Tab.CALENDAR.route) {
                CalendarScreen(
                    data = data,
                    onOpenDay = { date -> navController.navigate(Routes.symptomFor(date)) },
                    onRecordPeriod = { navController.navigate(Routes.PERIOD_RECORD) }
                )
            }

            composable(Tab.HISTORY.route) {
                HistoryScreen(
                    data = data,
                    onOpenStats = { navController.navigate(Routes.STATS) },
                    onEditPeriods = { navController.navigate(Routes.PERIOD_RECORD) },
                    onOpenDay = { date -> navController.navigate(Routes.symptomFor(date)) }
                )
            }

            composable(Tab.SETTINGS.route) {
                SettingsScreen(
                    data = data,
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                    onOpenMessages = { navController.navigate(Routes.MESSAGES) }
                )
            }

            composable(Routes.PERIOD_RECORD) {
                PeriodRecordScreen(data = data, onBack = { navController.popBackStack() })
            }

            composable("${Routes.SYMPTOM}/{date}") { entry ->
                val date = runCatching {
                    LocalDate.parse(entry.arguments?.getString("date"))
                }.getOrDefault(LocalDate.now())
                SymptomScreen(
                    data = data,
                    date = date,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.STATS) {
                StatsScreen(data = data, onBack = { navController.popBackStack() })
            }

            composable(Routes.NOTIFICATIONS) {
                NotificationSettingsScreen(
                    data = data,
                    onBack = { navController.popBackStack() },
                    onRequestPermission = onRequestNotificationPermission
                )
            }

            composable(Routes.PRIVACY) {
                PrivacySettingsScreen(data = data, onBack = { navController.popBackStack() })
            }

            composable(Routes.BACKUP) {
                BackupScreen(data = data, onBack = { navController.popBackStack() })
            }

            composable(Routes.MESSAGES) {
                MessagesScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
