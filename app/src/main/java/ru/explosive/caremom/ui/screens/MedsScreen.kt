package ru.explosive.caremom.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.explosive.caremom.data.Child
import ru.explosive.caremom.data.HealthEvent
import ru.explosive.caremom.data.HealthEventType
import ru.explosive.caremom.data.MedicationCourse
import ru.explosive.caremom.ui.theme.*
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.ui.components.ChildSelector
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedsScreen(viewModel: MainViewModel) {
    val activeCourses by viewModel.activeCourses.collectAsState()
    val archivedCourses by viewModel.archivedCourses.collectAsState()
    val medHistory by viewModel.medicationHistory.collectAsState()
    val children by viewModel.children.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()
    
    val selectedChild = remember(children, selectedChildId) { children.find { it.id == selectedChildId } }
    
    var showAddCourseSheet by remember { mutableStateOf(false) }
    var courseToDelete by remember { mutableStateOf<MedicationCourse?>(null) }
    var showArchive by remember { mutableStateOf(false) }
    
    val dateFormat = SimpleDateFormat("dd MMMM, HH:mm", Locale("ru"))

    // Авто-выбор ребенка при входе
    LaunchedEffect(children) {
        if (selectedChildId == null && children.isNotEmpty()) {
            viewModel.selectChild(children.first().id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedChildId != null) {
                FloatingActionButton(
                    onClick = { showAddCourseSheet = true },
                    containerColor = EntryMedication,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 14.dp, end = 16.dp)
                        .size(51.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить курс", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Заголовок как в Дневнике
            Text(
                text = "Лекарства",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            ChildSelector(
                children = children,
                selectedChild = selectedChild,
                onChildSelected = { viewModel.selectChild(it.id) }
            )

            Spacer(Modifier.height(8.dp))

            if (children.isEmpty()) {
                EmptyMedsState("Добавьте профиль ребенка для ведения аптечки", Icons.Default.Face)
            } else if (selectedChildId == null) {
                EmptyMedsState("Выберите ребенка", Icons.Default.TouchApp)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // Активные курсы выбранного ребенка
                    if (activeCourses.isNotEmpty()) {
                        item {
                            Text(
                                text = "Текущее лечение",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                items(items = activeCourses, key = { it.id }) { course ->
                                    ActiveCourseCard(
                                        course = course,
                                        child = selectedChild,
                                        onTakeDose = {
                                            viewModel.addEvent(
                                                HealthEvent(
                                                    childId = course.childId,
                                                    type = HealthEventType.MEDICINE,
                                                    timestamp = System.currentTimeMillis(),
                                                    title = "Прием: ${course.medicineName}",
                                                    description = "Доза: ${course.dosage}"
                                                )
                                            )
                                        },
                                        onFinishCourse = {
                                            viewModel.updateCourseStatus(course.id, false)
                                        },
                                        onDelete = {
                                            courseToDelete = course
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Переключатель история/архив
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showArchive) "Архив курсов" else "История приёмов",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = { showArchive = !showArchive }) {
                                Text(
                                    text = if (showArchive) "Архив" else "К приёмам",
                                    color = EntryMedication,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (showArchive) {
                        if (archivedCourses.isEmpty()) {
                            item { EmptyMedsState("Архив пуст", Icons.Default.Inventory) }
                        } else {
                            items(items = archivedCourses, key = { it.id }) { course ->
                                ArchivedCourseItem(course, selectedChild) {
                                    viewModel.updateCourseStatus(course.id, true)
                                }
                            }
                        }
                    } else {
                        if (medHistory.isEmpty()) {
                            item { EmptyMedsState("История пуста", Icons.Default.History) }
                        } else {
                            items(items = medHistory, key = { it.id }) { med ->
                                MedicationHistoryItem(
                                    event = med,
                                    time = dateFormat.format(Date(med.timestamp)),
                                    onDelete = { viewModel.deleteEvent(med) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCourseSheet && selectedChild != null) {
        ModalBottomSheet(
            onDismissRequest = { showAddCourseSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primaryContainer) }
        ) {
            AddCourseForm(
                child = selectedChild,
                onDismiss = { showAddCourseSheet = false },
                onConfirm = { course ->
                    viewModel.addCourse(course)
                    showAddCourseSheet = false
                }
            )
        }
    }

    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text("Удалить курс?", fontWeight = FontWeight.Bold) },
            text = { Text("Вся информация о лечении ${course.medicineName} будет удалена.") },
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCourse(course)
                    courseToDelete = null
                }) { Text("Удалить", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { courseToDelete = null }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun EmptyMedsState(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primaryContainer)
            Spacer(Modifier.height(8.dp))
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, lineHeight = 20.sp)
        }
    }
}

@Composable
fun ArchivedCourseItem(course: MedicationCourse, child: Child?, onRestart: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(course.medicineName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text("${child?.name ?: "Ребенок"} • Завершен", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRestart) {
                Icon(Icons.Default.Refresh, "Возобновить", tint = EntryMedication)
            }
        }
    }
}

@Composable
fun ActiveCourseCard(course: MedicationCourse, child: Child?, onTakeDose: () -> Unit, onFinishCourse: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.width(300.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(EntryMedication.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    if (child?.photoPath != null) {
                        AsyncImage(
                            model = child.photoPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Face, null, tint = EntryMedication, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(course.medicineName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    Text(child?.name ?: "Ребенок", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, "Удалить", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, null, tint = EntryMedication, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "${course.dosage} • ${course.timesPerDay} р/день", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (!course.reminderTimes.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Напоминания: ${course.reminderTimes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onTakeDose, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = EntryMedication)) { 
                Text("Отметить прием", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)) 
            }
            TextButton(onClick = onFinishCourse, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) { 
                Text("Завершить курс", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) 
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseForm(child: Child, onDismiss: () -> Unit, onConfirm: (MedicationCourse) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var times by remember { mutableStateOf("3") }
    var days by remember { mutableStateOf("5") }
    val reminderTimes = remember { mutableStateListOf<String>() }
    val context = LocalContext.current

    // Initialize reminder times when 'times' changes
    LaunchedEffect(times) {
        val count = times.toIntOrNull() ?: 0
        if (count > 0 && reminderTimes.size != count) {
            reminderTimes.clear()
            for (i in 0 until count) {
                reminderTimes.add(String.format(Locale.getDefault(), "%02d:00", 8 + (i * (14 / count.coerceAtLeast(1)))))
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = "Новое лечение для ${child.name}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
        
        OutlinedTextField(
            value = name, 
            onValueChange = { name = it }, 
            label = { Text("Название лекарства") }, 
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EntryMedication, focusedLabelColor = EntryMedication)
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = dosage, 
                onValueChange = { dosage = it }, 
                label = { Text("Дозировка") }, 
                modifier = Modifier.weight(1f), 
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EntryMedication, focusedLabelColor = EntryMedication)
            )
            OutlinedTextField(
                value = times, 
                onValueChange = { 
                    if (it.length <= 1) times = it 
                }, 
                label = { Text("Раз в день") }, 
                modifier = Modifier.weight(0.7f), 
                shape = RoundedCornerShape(16.dp), 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EntryMedication, focusedLabelColor = EntryMedication)
            )
            OutlinedTextField(
                value = days, 
                onValueChange = { days = it }, 
                label = { Text("Дни") }, 
                modifier = Modifier.weight(0.5f), 
                shape = RoundedCornerShape(16.dp), 
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EntryMedication, focusedLabelColor = EntryMedication)
            )
        }

        if (reminderTimes.isNotEmpty()) {
            Text("Время напоминаний:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reminderTimes.size) { index ->
                    AssistChip(
                        onClick = {
                            val currentTime = reminderTimes[index].split(":")
                            TimePickerDialog(context, { _, h, m ->
                                reminderTimes[index] = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                            }, currentTime[0].toInt(), currentTime[1].toInt(), true).show()
                        },
                        label = { Text(reminderTimes[index]) },
                        leadingIcon = { Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (name.isNotBlank()) {
                    val duration = (days.toLongOrNull() ?: 1L) * 24 * 60 * 60 * 1000L
                    onConfirm(MedicationCourse(
                        childId = child.id, 
                        medicineName = name, 
                        dosage = dosage, 
                        startDate = System.currentTimeMillis(), 
                        endDate = System.currentTimeMillis() + duration, 
                        timesPerDay = times.toIntOrNull() ?: 1,
                        reminderTimes = reminderTimes.joinToString(",")
                    ))
                    onDismiss()
                }
            }, 
            modifier = Modifier.fillMaxWidth().height(56.dp), 
            shape = RoundedCornerShape(16.dp), 
            colors = ButtonDefaults.buttonColors(containerColor = EntryMedication)
        ) {
            Text("Начать курс", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationHistoryItem(event: HealthEvent, time: String, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })
    SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = false, backgroundContent = {
        Box(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 6.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFEBEE)), contentAlignment = Alignment.CenterEnd) {
            Icon(Icons.Default.Delete, "Удалить", tint = Color(0xFFEF5350), modifier = Modifier.padding(end = 16.dp))
        }
    }) {
        Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(EntryMedication.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Check, null, tint = EntryMedication, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = event.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "${event.description ?: ""} • $time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
