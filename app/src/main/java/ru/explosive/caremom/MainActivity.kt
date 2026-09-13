package ru.explosive.caremom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import ru.explosive.caremom.data.CareMomDatabase
import ru.explosive.caremom.data.CareMomRepository
import ru.explosive.caremom.navigation.Screen
import ru.explosive.caremom.ui.screens.*
import ru.explosive.caremom.ui.theme.*
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.ui.viewmodel.MainViewModelFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val database by lazy { CareMomDatabase.getDatabase(this) }
    private val repository by lazy { CareMomRepository(database.dao()) }
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(repository, application)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermissions()

        val prefs = getSharedPreferences("caremom_prefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)

        setContent {
            // "system", "light", "dark"
            val themeMode = remember { mutableStateOf(prefs.getString("theme_mode", "system") ?: "system") }
            
            val useDarkTheme = when (themeMode.value) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            CareMomTheme(darkTheme = useDarkTheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    CareMomApp(
                        viewModel = viewModel,
                        themeMode = themeMode.value,
                        onThemeChange = { newMode ->
                            themeMode.value = newMode
                            prefs.edit { putString("theme_mode", newMode) }
                        },
                        initiallyShowOnboarding = isFirstRun,
                        onOnboardingFinish = {
                            prefs.edit { putBoolean("is_first_run", false) }
                        }
                    )
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}

@Composable
fun CareMomApp(
    viewModel: MainViewModel,
    themeMode: String,
    onThemeChange: (String) -> Unit,
    initiallyShowOnboarding: Boolean,
    onOnboardingFinish: () -> Unit
) {
    val navController = rememberNavController()
    var showOnboarding by remember { mutableStateOf(initiallyShowOnboarding) }
    val childrenUiState by viewModel.childrenState.collectAsState()
    
    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Meds,
        Screen.Statistics,
        Screen.Gallery,
        Screen.More
    )

    if (showOnboarding) {
        OnboardingScreen(onFinish = { 
            showOnboarding = false 
            onOnboardingFinish()
        })
    } else if (!childrenUiState.isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (childrenUiState.list.isEmpty()) {
        FirstChildScreen(viewModel = viewModel)
    } else {
        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val isDetailScreen = currentDestination?.route?.let { route ->
                    route.startsWith("child_detail") ||
                    route.startsWith("document_viewer") ||
                    route.startsWith("entry_detail") ||
                    route.startsWith("full_screen_image") ||
                    route == Screen.Settings.route ||
                    route == Screen.Vaccinations.route ||
                    route == Screen.QuickTemp.route ||
                    route == Screen.Profiles.route
                } ?: false
                
                if (!isDetailScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        bottomNavItems.forEach { screen ->
                            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                            NavigationBarItem(
                                icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
                                label = { 
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Visible,
                                        softWrap = false
                                    ) 
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { 
                    HomeScreen(
                        viewModel = viewModel,
                        onEventClick = { eventId ->
                            navController.navigate("entry_detail/$eventId")
                        }
                    ) 
                }
                composable(Screen.Meds.route) { MedsScreen(viewModel) }
                
                composable(Screen.Statistics.route) { 
                    StatsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    ) 
                }

                composable(Screen.Gallery.route) { 
                    GalleryScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onImageClick = { path ->
                            val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                            navController.navigate("full_screen_image?imagePath=$encodedPath")
                        }
                    ) 
                }

                composable(Screen.More.route) { 
                    MoreScreen(
                        viewModel = viewModel,
                        onNavigateToProfiles = { navController.navigate(Screen.Profiles.route) },
                        onNavigateToVaccines = { navController.navigate(Screen.Vaccinations.route) },
                        onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                    )
                }
                
                composable(Screen.Profiles.route) { 
                    ProfilesScreen(
                        viewModel = viewModel,
                        onChildClick = { childId ->
                            navController.navigate("child_detail/$childId")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Vaccinations.route) { 
                    VaccinationsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    ) 
                }

                composable(Screen.Settings.route) { 
                    SettingsScreen(
                        viewModel = viewModel,
                        themeMode = themeMode,
                        onThemeChange = onThemeChange,
                        onBack = { navController.popBackStack() }
                    )
                }
                
                composable(Screen.QuickTemp.route) {
                    QuickTempScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = "child_detail/{childId}",
                    arguments = listOf(navArgument("childId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val childId = backStackEntry.arguments?.getLong("childId") ?: 0L
                    ChildDetailScreen(
                        childId = childId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onDocumentClick = { docId ->
                            navController.navigate("document_viewer/$childId/$docId")
                        }
                    )
                }

                composable(
                    route = "entry_detail/{eventId}",
                    arguments = listOf(navArgument("eventId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val eventId = backStackEntry.arguments?.getLong("eventId") ?: 0L
                    EntryDetailScreen(
                        eventId = eventId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onImageClick = { path ->
                            val encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                            navController.navigate("full_screen_image?imagePath=$encodedPath")
                        }
                    )
                }

                composable(
                    route = "document_viewer/{childId}/{documentId}",
                    arguments = listOf(
                        navArgument("childId") { type = NavType.LongType },
                        navArgument("documentId") { type = NavType.LongType }
                    )
                ) { backStackEntry ->
                    val childId = backStackEntry.arguments?.getLong("childId") ?: 0L
                    val docId = backStackEntry.arguments?.getLong("documentId") ?: 0L
                    DocumentViewerScreen(
                        childId = childId,
                        documentId = docId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.FullScreenImage.route,
                    arguments = listOf(navArgument("imagePath") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val imagePath = backStackEntry.arguments?.getString("imagePath") ?: ""
                    FullScreenImageScreen(
                        imagePath = imagePath,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
