package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.DaynexaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as DaynexaApplication
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(app.repository, app.backupService)
            )
            val settingsState by settingsViewModel.uiState.collectAsState()

            val isDark = when (settingsState.theme) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            DaynexaTheme(darkTheme = isDark) {
                if (settingsState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    MainContent(
                        app = app,
                        settingsViewModel = settingsViewModel,
                        isOnboardingCompleted = settingsState.isOnboardingCompleted
                    ) {
                        settingsViewModel.completeOnboarding()
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(
    app: DaynexaApplication,
    settingsViewModel: SettingsViewModel,
    isOnboardingCompleted: Boolean,
    onOnboardingComplete: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val startDestination = if (isOnboardingCompleted) Screen.Today else Screen.Onboarding

    Scaffold(
        bottomBar = {
            if (isOnboardingCompleted && currentDestination?.route != Screen.Onboarding::class.qualifiedName && currentDestination?.route?.contains("TaskDetail") != true) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    val isToday = currentDestination?.route == Screen.Today::class.qualifiedName
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isToday) Icons.Filled.Today else Icons.Outlined.Today,
                                contentDescription = "Today"
                            )
                        },
                        label = { Text("Today", fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal) },
                        selected = isToday,
                        onClick = {
                            navController.navigate(Screen.Today) {
                                popUpTo(Screen.Today) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )

                    val isCalendar = currentDestination?.route == Screen.Calendar::class.qualifiedName
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isCalendar) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                contentDescription = "Calendar"
                            )
                        },
                        label = { Text("Calendar", fontWeight = if (isCalendar) FontWeight.Bold else FontWeight.Normal) },
                        selected = isCalendar,
                        onClick = {
                            navController.navigate(Screen.Calendar) {
                                popUpTo(Screen.Today) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )

                    val isStats = currentDestination?.route == Screen.Statistics::class.qualifiedName
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isStats) Icons.Filled.Insights else Icons.Outlined.Insights,
                                contentDescription = "Analytics"
                            )
                        },
                        label = { Text("Stats", fontWeight = if (isStats) FontWeight.Bold else FontWeight.Normal) },
                        selected = isStats,
                        onClick = {
                            navController.navigate(Screen.Statistics) {
                                popUpTo(Screen.Today) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )

                    val isCategories = currentDestination?.route == Screen.Categories::class.qualifiedName
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isCategories) Icons.Filled.Category else Icons.Outlined.Category,
                                contentDescription = "Categories"
                            )
                        },
                        label = { Text("Categories", fontWeight = if (isCategories) FontWeight.Bold else FontWeight.Normal) },
                        selected = isCategories,
                        onClick = {
                            navController.navigate(Screen.Categories) {
                                popUpTo(Screen.Today) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )

                    val isSettings = currentDestination?.route == Screen.Settings::class.qualifiedName
                    NavigationBarItem(
                        icon = {
                            Icon(
                                if (isSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings", fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Normal) },
                        selected = isSettings,
                        onClick = {
                            navController.navigate(Screen.Settings) {
                                popUpTo(Screen.Today) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Screen.Onboarding> {
                OnboardingScreen(onComplete = {
                    onOnboardingComplete()
                    navController.navigate(Screen.Today) {
                        popUpTo(Screen.Onboarding) { inclusive = true }
                    }
                })
            }
            composable<Screen.Today> {
                val todayViewModel: TodayViewModel = viewModel(
                    factory = TodayViewModel.Factory(app.repository, app.schedulingService)
                )
                TodayScreen(
                    viewModel = todayViewModel,
                    onAddTask = { navController.navigate(Screen.TaskDetail()) },
                    onEditTask = { taskId -> navController.navigate(Screen.TaskDetail(taskId = taskId)) }
                )
            }
            composable<Screen.Calendar> {
                val calendarViewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.Factory(app.repository, app.schedulingService)
                )
                CalendarScreen(
                    viewModel = calendarViewModel,
                    onEditTask = { taskId -> navController.navigate(Screen.TaskDetail(taskId = taskId)) }
                )
            }
            composable<Screen.Statistics> {
                val statisticsViewModel: StatisticsViewModel = viewModel(
                    factory = StatisticsViewModel.Factory(app.repository, app.schedulingService)
                )
                StatisticsScreen(viewModel = statisticsViewModel)
            }
            composable<Screen.Categories> {
                val categoriesViewModel: CategoriesViewModel = viewModel(
                    factory = CategoriesViewModel.Factory(app.repository)
                )
                CategoriesScreen(viewModel = categoriesViewModel)
            }
            composable<Screen.Settings> {
                SettingsScreen(viewModel = settingsViewModel)
            }
            composable<Screen.TaskDetail> { backStackEntry ->
                val route: Screen.TaskDetail = backStackEntry.toRoute()
                val detailViewModel: TaskDetailViewModel = viewModel(
                    factory = TaskDetailViewModel.Factory(app.repository, route.taskId)
                )
                TaskDetailScreen(
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
