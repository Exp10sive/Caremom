package ru.explosive.caremom.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.explosive.caremom.data.CareMomRepository
import ru.explosive.caremom.data.Child
import ru.explosive.caremom.data.HealthEvent
import ru.explosive.caremom.data.HealthEventType
import ru.explosive.caremom.data.MedicationCourse
import ru.explosive.caremom.data.MedicalDocument
import ru.explosive.caremom.notifications.NotificationHelper
import ru.explosive.caremom.util.FileUtil
import ru.explosive.caremom.util.formatHeaderDate
import java.io.File
import java.util.*

data class ChildrenUiState(
    val list: List<Child> = emptyList(),
    val isInitialized: Boolean = false
)

class MainViewModel(
    private val repository: CareMomRepository,
    private val application: Application
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("caremom_prefs", Context.MODE_PRIVATE)
    private val notificationHelper = NotificationHelper(application)

    // Используем SharingStarted.Eagerly, чтобы загрузка началась мгновенно
    val childrenState: StateFlow<ChildrenUiState> = repository.allChildren
        .map { ChildrenUiState(list = it, isInitialized = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ChildrenUiState(isInitialized = false)
        )

    val children = childrenState.map { it.list }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allEvents: StateFlow<List<HealthEvent>> = repository.allEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedChildId = MutableStateFlow<Long?>(null)
    val selectedChildId = _selectedChildId.asStateFlow()

    init {
        val savedId = prefs.getLong("selected_child_id", -1L)
        if (savedId != -1L) {
            _selectedChildId.value = savedId
        }
    }

    val filteredEvents = combine(allEvents, _selectedChildId) { events, childId ->
        if (childId == null) emptyList()
        else events.filter { it.childId == childId }.sortedByDescending { it.timestamp }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedEvents = filteredEvents.map { events ->
        events.groupBy { formatHeaderDate(it.timestamp) }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val galleryPhotos = combine(allEvents, _selectedChildId) { events, childId ->
        events.filter { (childId == null || it.childId == childId) && !it.imagePath.isNullOrEmpty() }
            .map { it.imagePath!! }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statsData = combine(allEvents, _selectedChildId) { events, childId ->
        val childEvents = if (childId == null) events else events.filter { it.childId == childId }
        
        val tempData = childEvents.filter { it.type == HealthEventType.SICKNESS && it.value?.toFloatOrNull() != null }
            .sortedBy { it.timestamp }
            .map { it.value!!.toFloat() }

        val heightData = childEvents.filter { it.type == HealthEventType.HEIGHT && it.value?.toFloatOrNull() != null }
            .sortedBy { it.timestamp }
            .map { it.value!!.toFloat() }

        val weightData = childEvents.filter { it.type == HealthEventType.WEIGHT && it.value?.toFloatOrNull() != null }
            .sortedBy { it.timestamp }
            .map { it.value!!.toFloat() }

        val sicknessDates = childEvents.filter { it.type == HealthEventType.SICKNESS }
            .map { 
                val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                cal.get(Calendar.DAY_OF_YEAR) to cal.get(Calendar.YEAR)
            }.toSet()

        val topSymptoms = childEvents.filter { it.type == HealthEventType.SICKNESS }
            .groupBy { it.title.lowercase().trim() }
            .map { it.key to it.value.size }
            .sortedByDescending { it.second }
            .take(4)

        StatsResult(tempData, heightData, weightData, sicknessDates, topSymptoms)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsResult())

    val allCourses: StateFlow<List<MedicationCourse>> = repository.allCourses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeCourses = combine(allCourses, _selectedChildId) { courses, childId ->
        if (childId == null) emptyList()
        else courses.filter { it.isActive && it.childId == childId }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedCourses = combine(allCourses, _selectedChildId) { courses, childId ->
        if (childId == null) emptyList()
        else courses.filter { !it.isActive && it.childId == childId }.sortedByDescending { it.endDate }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicationHistory = combine(allEvents, _selectedChildId) { events, childId ->
        if (childId == null) emptyList()
        else events.filter { it.type == HealthEventType.MEDICINE && it.childId == childId }.sortedByDescending { it.timestamp }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaccinationDoneStatus = combine(allEvents, _selectedChildId) { events, childId ->
        if (childId == null) emptySet<String>()
        else events.filter { it.childId == childId && it.type == HealthEventType.VACCINATION }
            .map { it.title.lowercase() }
            .toSet()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun selectChild(id: Long?) {
        _selectedChildId.value = id
        viewModelScope.launch(Dispatchers.IO) {
            if (id != null) prefs.edit().putLong("selected_child_id", id).apply()
            else prefs.edit().remove("selected_child_id").apply()
        }
    }

    fun addChild(name: String, birthday: Long, photoUri: String? = null, bloodType: String? = null, allergies: String? = null, chronicDiseases: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedPath = photoUri?.let { uriStr ->
                if (!uriStr.startsWith("/")) {
                    FileUtil.saveImageToInternalStorage(application, Uri.parse(uriStr))
                } else uriStr
            }
            repository.insertChild(Child(name = name, birthday = birthday, photoPath = savedPath, bloodType = bloodType, allergies = allergies, chronicDiseases = chronicDiseases))
        }
    }

    fun updateChild(child: Child, newPhotoUri: String? = null, photoRemoved: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalPath = child.photoPath
            if (photoRemoved) {
                child.photoPath?.let { File(it).delete() }
                finalPath = null
            } else if (newPhotoUri != null && !newPhotoUri.startsWith("/")) {
                child.photoPath?.let { File(it).delete() }
                finalPath = FileUtil.saveImageToInternalStorage(application, Uri.parse(newPhotoUri))
            }
            repository.updateChild(child.copy(photoPath = finalPath))
        }
    }

    fun deleteChild(child: Child) {
        viewModelScope.launch(Dispatchers.IO) {
            child.photoPath?.let { File(it).delete() }
            val childEvents = repository.getEventsForChildOnce(child.id)
            childEvents.forEach { event ->
                event.imagePath?.let { File(it).delete() }
                event.voicePath?.let { File(it).delete() }
            }
            val childDocs = repository.getDocumentsForChildOnce(child.id)
            childDocs.forEach { doc ->
                File(doc.imageUri).delete()
            }
            val courses = repository.getCoursesForChildOnce(child.id)
            courses.forEach { notificationHelper.cancelReminders(it.id, it.timesPerDay) }
            repository.deleteChild(child)
            if (_selectedChildId.value == child.id) {
                selectChild(null)
            }
        }
    }

    fun addEvent(event: HealthEvent, imageUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedImagePath = imageUri?.let { uriStr ->
                if (uriStr.isNotEmpty() && !uriStr.startsWith("/")) {
                    FileUtil.saveImageToInternalStorage(application, Uri.parse(uriStr))
                } else uriStr
            }
            repository.insertEvent(event.copy(imagePath = savedImagePath))
        }
    }

    fun updateEvent(event: HealthEvent, newImageUri: String? = null, imageRemoved: Boolean = false, newVoicePath: String? = null, voiceRemoved: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            var finalImagePath = event.imagePath
            var finalVoicePath = event.voicePath
            
            if (imageRemoved) {
                event.imagePath?.let { File(it).delete() }
                finalImagePath = null
            } else if (newImageUri != null && !newImageUri.startsWith("/")) {
                event.imagePath?.let { File(it).delete() }
                finalImagePath = FileUtil.saveImageToInternalStorage(application, Uri.parse(newImageUri))
            }
            
            if (voiceRemoved) {
                event.voicePath?.let { File(it).delete() }
                finalVoicePath = null
            } else if (newVoicePath != null && newVoicePath != event.voicePath) {
                event.voicePath?.let { File(it).delete() }
                finalVoicePath = newVoicePath
            }
            
            repository.updateEvent(event.copy(imagePath = finalImagePath, voicePath = finalVoicePath))
        }
    }

    fun deleteEvent(event: HealthEvent) {
        viewModelScope.launch(Dispatchers.IO) {
            event.imagePath?.let { File(it).delete() }
            event.voicePath?.let { File(it).delete() }
            repository.deleteEvent(event)
        }
    }

    fun addCourse(course: MedicationCourse) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertCourse(course)
            notificationHelper.scheduleMedicationReminders(course.copy(id = id))
        }
    }

    fun deleteCourse(course: MedicationCourse) {
        viewModelScope.launch(Dispatchers.IO) {
            notificationHelper.cancelReminders(course.id, course.timesPerDay)
            repository.deleteCourse(course)
        }
    }

    fun updateCourseStatus(courseId: Long, active: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCourseStatus(courseId, active)
            val course = allCourses.value.find { it.id == courseId }
            if (course != null) {
                if (active) notificationHelper.scheduleMedicationReminders(course)
                else notificationHelper.cancelReminders(courseId, course.timesPerDay)
            }
        }
    }

    fun getEventsForChild(childId: Long) = repository.getEventsForChild(childId)
    fun getDocumentsForChild(childId: Long) = repository.getDocumentsForChild(childId)

    fun addDocument(childId: Long, title: String, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val savedPath = FileUtil.saveImageToInternalStorage(application, uri)
            if (savedPath != null) {
                repository.insertDocument(MedicalDocument(childId = childId, title = title, date = System.currentTimeMillis(), imageUri = savedPath, category = "Анализ"))
            }
        }
    }

    fun deleteDocument(document: MedicalDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            File(document.imageUri).delete()
            repository.deleteDocument(document)
        }
    }

    suspend fun getChildrenWithEvents(): List<Pair<Child, List<HealthEvent>>> {
        val allChildrenList = children.value
        val allEventsList = allEvents.value
        return allChildrenList.map { child -> child to allEventsList.filter { it.childId == child.id } }
    }
}

data class StatsResult(
    val tempData: List<Float> = emptyList(),
    val heightData: List<Float> = emptyList(),
    val weightData: List<Float> = emptyList(),
    val sicknessDates: Set<Pair<Int, Int>> = emptySet(),
    val topSymptoms: List<Pair<String, Int>> = emptyList()
)

class MainViewModelFactory(
    private val repository: CareMomRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
