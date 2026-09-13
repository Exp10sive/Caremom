package ru.explosive.caremom.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "children")
data class Child(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birthday: Long, // Timestamp
    val photoPath: String? = null,
    val bloodType: String? = null,
    val allergies: String? = null,
    val chronicDiseases: String? = null
)
