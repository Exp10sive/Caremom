package ru.explosive.caremom.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.explosive.caremom.data.CareMomDatabase
import ru.explosive.caremom.data.CareMomRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val database = CareMomDatabase.getDatabase(context)
            val repository = CareMomRepository(database.dao())
            val notificationHelper = NotificationHelper(context)

            CoroutineScope(Dispatchers.IO).launch {
                // Получаем все активные курсы всех детей
                val activeCourses = repository.getAllActiveCoursesOnce()
                activeCourses.forEach { course ->
                    notificationHelper.scheduleMedicationReminders(course)
                }
            }
        }
    }
}
