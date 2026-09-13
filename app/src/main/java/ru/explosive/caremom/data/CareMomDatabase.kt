package ru.explosive.caremom.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): java.util.Date? {
        return value?.let { java.util.Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: java.util.Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromHealthEventType(value: HealthEventType): String {
        return value.name
    }

    @TypeConverter
    fun toHealthEventType(value: String): HealthEventType {
        try {
            return HealthEventType.valueOf(value)
        } catch (e: Exception) {
            return HealthEventType.NOTE
        }
    }
}

@Database(
    entities = [Child::class, HealthEvent::class, MedicationCourse::class, MedicalDocument::class], 
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CareMomDatabase : RoomDatabase() {
    abstract fun dao(): CareMomDao

    companion object {
        @Volatile
        private var INSTANCE: CareMomDatabase? = null

        fun getDatabase(context: Context): CareMomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CareMomDatabase::class.java,
                    "caremom_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
