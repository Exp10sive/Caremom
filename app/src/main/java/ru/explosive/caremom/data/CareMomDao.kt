package ru.explosive.caremom.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CareMomDao {
    @Query("SELECT * FROM children")
    fun getAllChildren(): Flow<List<Child>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: Child)

    @Update
    suspend fun updateChild(child: Child)

    @Delete
    suspend fun deleteChild(child: Child)

    @Query("SELECT * FROM health_events WHERE childId = :childId ORDER BY timestamp DESC")
    fun getEventsForChild(childId: Long): Flow<List<HealthEvent>>

    @Query("SELECT * FROM health_events WHERE childId = :childId")
    suspend fun getEventsForChildOnce(childId: Long): List<HealthEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: HealthEvent)

    @Update
    suspend fun updateEvent(event: HealthEvent)

    @Delete
    suspend fun deleteEvent(event: HealthEvent)

    @Query("SELECT * FROM health_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<HealthEvent>>

    @Query("SELECT * FROM medication_courses WHERE childId = :childId")
    fun getCoursesForChild(childId: Long): Flow<List<MedicationCourse>>

    @Query("SELECT * FROM medication_courses WHERE childId = :childId")
    suspend fun getCoursesForChildOnce(childId: Long): List<MedicationCourse>

    @Query("SELECT * FROM medication_courses WHERE isActive = 1")
    suspend fun getAllActiveCoursesOnce(): List<MedicationCourse>

    @Query("SELECT * FROM medication_courses")
    fun getAllCourses(): Flow<List<MedicationCourse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: MedicationCourse): Long

    @Query("UPDATE medication_courses SET isActive = :active WHERE id = :courseId")
    suspend fun updateCourseStatus(courseId: Long, active: Boolean)

    @Delete
    suspend fun deleteCourse(course: MedicationCourse)

    @Query("SELECT * FROM medical_documents WHERE childId = :childId ORDER BY date DESC")
    fun getDocumentsForChild(childId: Long): Flow<List<MedicalDocument>>

    @Query("SELECT * FROM medical_documents WHERE childId = :childId")
    suspend fun getDocumentsForChildOnce(childId: Long): List<MedicalDocument>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: MedicalDocument)

    @Delete
    suspend fun deleteDocument(document: MedicalDocument)
}
