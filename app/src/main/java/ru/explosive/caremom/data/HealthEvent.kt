package ru.explosive.caremom.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class HealthEventType {
    SICKNESS,     // Болезнь / Симптомы
    MEDICINE,     // Лекарство
    FOOD_STOOL,   // Еда / Стул
    SLEEP,        // Сон
    VACCINATION,  // Вакцинация
    ANALYSIS,     // Анализы
    DOCTOR,       // Врач
    NOTE,         // Заметка
    HEIGHT,       // Рост
    WEIGHT        // Вес
}

@Entity(
    tableName = "health_events",
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
data class HealthEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val childId: Long,
    val type: HealthEventType,
    val timestamp: Long,
    val title: String,
    val description: String?,
    val value: String? = null,
    val isImportant: Boolean = false,
    val imagePath: String? = null,
    val voicePath: String? = null
)
