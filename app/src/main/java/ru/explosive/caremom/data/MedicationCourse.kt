package ru.explosive.caremom.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_courses",
    foreignKeys = [
        ForeignKey(
            entity = Child::class,
            parentColumns = ["id"],
            childColumns = ["childId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("childId")]
)
data class MedicationCourse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val medicineName: String,
    val dosage: String,
    val startDate: Long,
    val endDate: Long,
    val timesPerDay: Int,
    val isActive: Boolean = true,
    val reminderTimes: String? = null // Comma-separated HH:mm, e.g., "08:00,14:00,20:00"
)
