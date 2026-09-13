package ru.explosive.caremom.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.explosive.caremom.data.HealthEvent
import ru.explosive.caremom.data.HealthEventType
import ru.explosive.caremom.data.MedicationCourse
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.util.AudioPlayer
import ru.explosive.caremom.util.AudioRecorder
import ru.explosive.caremom.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onEventClick: (Long) -> Unit = {}
) {
    val groupedEvents by viewModel.groupedEvents.collectAsState()
    val children by viewModel.children.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()
    val context = LocalContext.current
    
    val recorder = remember { AudioRecorder(context) }
    val player = remember { AudioPlayer(context) }
    val listState = rememberLazyListState()

    var showReminderDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { 
            recorder.stop()
            player.stop() 
        }
    }
    
    LaunchedEffect(children) {
        if (selectedChildId == null && children.isNotEmpty()) {
            viewModel.selectChild(children.first().id)
        }
    }
    
    LaunchedEffect(selectedChildId) {
        if (groupedEvents.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    val selectedChild = children.find { it.id == selectedChildId }
    var showSheet by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf<HealthEventType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedChildId != null) {
                FloatingActionButton(
                    onClick = { 
                        selectedType = null
                        showSheet = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 14.dp, end = 16.dp)
                        .size(51.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить", modifier = Modifier.size(24.dp))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Дневник",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedChildId != null) {
                    Button(
                        onClick = { showReminderDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Напоминание", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            ChildSelector(
                children = children,
                selectedChild = selectedChild,
                onChildSelected = { viewModel.selectChild(it.id) }
            )

            Spacer(Modifier.height(8.dp))

            if (children.isEmpty()) {
                EmptyStateMessage("Сначала добавьте профиль ребенка в разделе 'Ещё' ❤️")
            } else if (groupedEvents.isEmpty()) {
                EmptyDiaryPlaceholder()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedEvents.forEach { (date, events) ->
                        item(key = date) {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                        items(items = events, key = { it.id }) { event ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { 
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteEvent(event)
                                        true 
                                    } else false 
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(Color(0xFFFFEBEE))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Default.Delete, "Удалить", tint = Color(0xFFEF5350))
                                    }
                                },
                                enableDismissFromStartToEnd = false
                            ) {
                                EventItem(
                                    event = event, 
                                    time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(event.timestamp)), 
                                    player = player,
                                    onClick = { onEventClick(event.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showReminderDialog && selectedChildId != null) {
            AddGenericReminderDialog(
                onDismiss = { showReminderDialog = false },
                onConfirm = { title, time ->
                    viewModel.addCourse(
                        MedicationCourse(
                            childId = selectedChildId!!,
                            medicineName = title,
                            dosage = "Напоминание",
                            startDate = System.currentTimeMillis(),
                            endDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 1 day
                            timesPerDay = 1,
                            reminderTimes = time,
                            isActive = true
                        )
                    )
                    showReminderDialog = false
                }
            )
        }

        if (showSheet && selectedChildId != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    recorder.stop()
                    showSheet = false 
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primaryContainer) },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                if (selectedType == null) {
                    EventTypeSelectionMenu(onTypeSelected = { selectedType = it })
                } else {
                    EventEntryForm(
                        type = selectedType!!,
                        recorder = recorder,
                        onDismiss = { showSheet = false },
                        onSave = { title, desc, valStr, voicePath, imageUri, isImportant, _, _ ->
                            viewModel.addEvent(
                                event = HealthEvent(
                                    childId = selectedChildId!!,
                                    type = selectedType!!,
                                    timestamp = System.currentTimeMillis(),
                                    title = title,
                                    description = desc,
                                    value = valStr,
                                    voicePath = voicePath,
                                    isImportant = isImportant
                                ),
                                imageUri = imageUri
                            )
                            showSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AddGenericReminderDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("12:00") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое напоминание", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Что напомнить?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                AssistChip(
                    onClick = {
                        val parts = time.split(":")
                        TimePickerDialog(context, { _, h, m ->
                            time = String.format(Locale.getDefault(), "%02d:%02d", h, m)
                        }, parts[0].toInt(), parts[1].toInt(), true).show()
                    },
                    label = { Text("Время: $time") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (title.isNotBlank()) onConfirm(title, time) }) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

@Composable
fun EventItem(
    event: HealthEvent, 
    time: String, 
    player: AudioPlayer,
    onClick: () -> Unit
) {
    val typeColor = getBgColorByType(event.type)
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getIconByType(event.type), 
                        contentDescription = null, 
                        modifier = Modifier.size(24.dp), 
                        tint = typeColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    val unit = when(event.type) {
                        HealthEventType.HEIGHT -> " см"
                        HealthEventType.WEIGHT -> " кг"
                        HealthEventType.SICKNESS -> " °C"
                        else -> ""
                    }
                    val titleText = if (!event.value.isNullOrEmpty()) {
                        "${event.title}: ${event.value}$unit"
                    } else event.title
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = titleText, 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (event.isImportant) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = time, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (!event.description.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = event.description, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }
            
            if (!event.voicePath.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .clickable { player.playFile(event.voicePath) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Голосовая заметка", 
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), 
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!event.imagePath.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = event.imagePath,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun EventTypeSelectionMenu(onTypeSelected: (HealthEventType) -> Unit) {
    val types = listOf(
        HealthEventType.SICKNESS to "Симптом",
        HealthEventType.MEDICINE to "Лекарство",
        HealthEventType.FOOD_STOOL to "Питание",
        HealthEventType.SLEEP to "Сон",
        HealthEventType.HEIGHT to "Рост",
        HealthEventType.WEIGHT to "Вес",
        HealthEventType.VACCINATION to "Прививка",
        HealthEventType.ANALYSIS to "Анализы",
        HealthEventType.DOCTOR to "Врач",
        HealthEventType.NOTE to "Заметка"
    )

    Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
        Text(
            text = "Выберите тип", 
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), 
            color = MaterialTheme.colorScheme.onSurface, 
            modifier = Modifier.padding(bottom = 20.dp)
        )
        types.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (type, label) ->
                    EventTypeCard(
                        type = type, 
                        label = label, 
                        onClick = { onTypeSelected(type) }, 
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EmptyDiaryPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = null, 
                    modifier = Modifier.size(56.dp), 
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Дневник пока пуст", 
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), 
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Добавьте первую запись о малыше", 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
