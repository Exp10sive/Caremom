package ru.explosive.caremom.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ru.explosive.caremom.data.MedicationCourse
import java.util.*

class NotificationHelper(private val context: Context) {

    fun scheduleMedicationReminders(course: MedicationCourse) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val reminderTimes = course.reminderTimes?.split(",")?.filter { it.isNotBlank() }
        
        val startDate = Calendar.getInstance().apply {
            timeInMillis = course.startDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val totalDays = ((course.endDate - course.startDate) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)

        for (day in 0 until totalDays) {
            val dayStartMillis = startDate + (day.toLong() * 24 * 60 * 60 * 1000)
            
            if (!reminderTimes.isNullOrEmpty()) {
                // Use specific times
                reminderTimes.forEachIndexed { index, timeStr ->
                    val parts = timeStr.split(":")
                    if (parts.size == 2) {
                        val hour = parts[0].toIntOrNull() ?: 0
                        val minute = parts[1].toIntOrNull() ?: 0
                        
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = dayStartMillis
                            set(Calendar.HOUR_OF_DAY, hour)
                            set(Calendar.MINUTE, minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        
                        val reminderTime = calendar.timeInMillis
                        if (reminderTime > System.currentTimeMillis() && reminderTime <= course.endDate) {
                            scheduleAlarm(course, reminderTime, day * 100 + index)
                        }
                    }
                }
            } else {
                // Fallback to distributed times if no specific times provided
                val startHour = 8
                val wakeWindowMillis = 15L * 60 * 60 * 1000 // 15 hours
                val intervalMillis = if (course.timesPerDay > 1) {
                    wakeWindowMillis / (course.timesPerDay - 1)
                } else 0L

                for (i in 0 until course.timesPerDay) {
                    val reminderTime = dayStartMillis + (startHour.toLong() * 60 * 60 * 1000) + (i.toLong() * intervalMillis)
                    
                    if (reminderTime > System.currentTimeMillis() && reminderTime <= course.endDate) {
                        scheduleAlarm(course, reminderTime, day * 100 + i)
                    }
                }
            }
        }
    }

    private fun scheduleAlarm(course: MedicationCourse, reminderTime: Long, uniqueId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("TITLE", "Пора принять лекарство")
            putExtra("MESSAGE", "${course.medicineName}, дозировка: ${course.dosage}")
        }

        val requestCode = (course.id.toInt() * 10000) + uniqueId
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent)
        }
    }

    fun cancelReminders(courseId: Long, timesPerDay: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Cancel for a reasonable range
        for (day in 0 until 30) {
            for (i in 0 until (if (timesPerDay > 0) timesPerDay else 10)) {
                val intent = Intent(context, ReminderReceiver::class.java)
                val requestCode = (courseId.toInt() * 10000) + (day * 100) + i
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent?.let { alarmManager.cancel(it) }
            }
        }
    }
}
