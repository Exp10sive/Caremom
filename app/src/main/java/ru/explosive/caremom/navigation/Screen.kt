package ru.explosive.caremom.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Дневник", Icons.Default.Home)
    object Meds : Screen("meds", "Аптечка", Icons.Default.MedicalServices)
    object Statistics : Screen("stats", "Статистика", Icons.Default.BarChart)
    object Gallery : Screen("gallery", "Галерея", Icons.Default.PhotoLibrary)
    object More : Screen("more", "Еще", Icons.Default.MoreHoriz)
    
    // Второстепенные экраны
    object Profiles : Screen("profiles", "Дети", Icons.Default.Face)
    object Vaccinations : Screen("vaccinations", "Прививки", Icons.Default.Shield)
    object Settings : Screen("settings", "Настройки", Icons.Default.Settings)
    object QuickTemp : Screen("quick_temp", "Температура", Icons.Default.Thermostat)

    object Onboarding : Screen("onboarding", "Приветствие")
    object ChildDetail : Screen("child_detail/{childId}", "Профиль ребенка")
    object EntryDetail : Screen("entry_detail/{eventId}", "Детали записи")
    object FullScreenImage : Screen("full_screen_image?imagePath={imagePath}", "Просмотр фото")
}
