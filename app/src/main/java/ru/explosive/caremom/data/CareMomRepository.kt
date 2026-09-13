package ru.explosive.caremom.data

import kotlinx.coroutines.flow.Flow

class CareMomRepository(private val dao: CareMomDao) {
    val allChildren: Flow<List<Child>> = dao.getAllChildren()
    val allEvents: Flow<List<HealthEvent>> = dao.getAllEvents()
    val allCourses: Flow<List<MedicationCourse>> = dao.getAllCourses()

    suspend fun insertChild(child: Child) = dao.insertChild(child)
    suspend fun updateChild(child: Child) = dao.updateChild(child)
    suspend fun deleteChild(child: Child) = dao.deleteChild(child)
    
    suspend fun insertEvent(event: HealthEvent) = dao.insertEvent(event)
    suspend fun updateEvent(event: HealthEvent) = dao.updateEvent(event)
    suspend fun deleteEvent(event: HealthEvent) = dao.deleteEvent(event)
    
    fun getEventsForChild(childId: Long): Flow<List<HealthEvent>> = 
        dao.getEventsForChild(childId)

    suspend fun getEventsForChildOnce(childId: Long): List<HealthEvent> = 
        dao.getEventsForChildOnce(childId)

    // Курсы лекарств
    fun getCoursesForChild(childId: Long): Flow<List<MedicationCourse>> = 
        dao.getCoursesForChild(childId)

    suspend fun getCoursesForChildOnce(childId: Long): List<MedicationCourse> = 
        dao.getCoursesForChildOnce(childId)

    suspend fun getAllActiveCoursesOnce(): List<MedicationCourse> = 
        dao.getAllActiveCoursesOnce()

    suspend fun insertCourse(course: MedicationCourse) = dao.insertCourse(course)
    suspend fun deleteCourse(course: MedicationCourse) = dao.deleteCourse(course)
    
    suspend fun updateCourseStatus(courseId: Long, active: Boolean) = 
        dao.updateCourseStatus(courseId, active)

    // Медицинские документы
    fun getDocumentsForChild(childId: Long): Flow<List<MedicalDocument>> = 
        dao.getDocumentsForChild(childId)

    suspend fun getDocumentsForChildOnce(childId: Long): List<MedicalDocument> = 
        dao.getDocumentsForChildOnce(childId)

    suspend fun insertDocument(document: MedicalDocument) = dao.insertDocument(document)
    
    suspend fun deleteDocument(document: MedicalDocument) = dao.deleteDocument(document)
}
