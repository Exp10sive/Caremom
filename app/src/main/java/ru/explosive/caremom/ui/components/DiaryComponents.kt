package ru.explosive.caremom.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.explosive.caremom.data.Child
import ru.explosive.caremom.data.HealthEvent
import ru.explosive.caremom.data.HealthEventType
import ru.explosive.caremom.ui.theme.*
import ru.explosive.caremom.util.AudioRecorder
import java.io.File
import java.util.*

@Composable
fun ChildSelector(
    children: List<Child>,
    selectedChild: Child?,
    onChildSelected: (Child) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(children) { child ->
            val isSelected = child.id == selectedChild?.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onChildSelected(child) }
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(if (isSelected) 3.dp else 0.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (child.photoPath != null) {
                        AsyncImage(
                            model = child.photoPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Face, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = child.name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EventEntryForm(
    type: HealthEventType,
    recorder: AudioRecorder,
    initialEvent: HealthEvent? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, value: String?, voicePath: String?, imageUri: String?, isImportant: Boolean, imageRemoved: Boolean, voiceRemoved: Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(initialEvent?.title ?: getTitleByType(type)) }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var value by remember { mutableStateOf(initialEvent?.value ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(initialEvent?.imagePath?.let { Uri.parse(it) }) }
    var isImportant by remember { mutableStateOf(initialEvent?.isImportant ?: false) }
    
    var isRecording by remember { mutableStateOf(false) }
    var recordedFile by remember { mutableStateOf<File?>(initialEvent?.voicePath?.let { File(it) }) }
    
    var imageRemoved by remember { mutableStateOf(false) }
    var voiceRemoved by remember { mutableStateOf(false) }
    
    val sessionFiles = remember { mutableStateListOf<File>() }
    var isSaved by remember { mutableStateOf(false) }

    val popularSymptoms = listOf("Кашель", "Насморк", "Болит горло", "Температура", "Слабость", "Сыпь")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> 
        if (uri != null) {
            selectedImageUri = uri
            imageRemoved = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!isSaved) {
                sessionFiles.forEach { file -> if (file.exists()) file.delete() }
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть")
            }
            Text(
                text = if (initialEvent == null) "Добавить запись" else "Изменить запись", 
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), 
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { isImportant = !isImportant },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isImportant) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            ) {
                Icon(
                    imageVector = if (isImportant) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Важное",
                    tint = if (isImportant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (type == HealthEventType.HEIGHT) {
            Text("Рост: ${value.ifBlank { "50" }} см", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Slider(
                value = if (value.isEmpty()) 50f else value.toFloatOrNull() ?: 50f,
                onValueChange = { value = it.toInt().toString() },
                valueRange = 30f..120f,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
        } else if (type == HealthEventType.WEIGHT) {
            Text("Вес: ${value.ifBlank { "5" }} кг", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Slider(
                value = if (value.isEmpty()) 5f else value.toFloatOrNull() ?: 5f,
                onValueChange = { value = String.format(Locale.US, "%.1f", it) },
                valueRange = 2f..25f,
                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
            )
        }

        if (type == HealthEventType.SICKNESS) {
            Text(text = "Популярные симптомы:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularSymptoms.forEach { symptom ->
                    FilterChip(
                        selected = title == symptom,
                        onClick = { title = symptom },
                        label = { Text(symptom) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (type == HealthEventType.SICKNESS) "Симптом" else "Заголовок") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
        )

        if (type == HealthEventType.SICKNESS) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Температура (°C) - если есть") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                trailingIcon = { Icon(Icons.Default.Thermostat, null, tint = MaterialTheme.colorScheme.primary) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(if (isRecording) "Идет запись..." else "Комментарий") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = { photoPickerLauncher.launch("image/*") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedImageUri != null) "Изменить" else "Фото", 
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            VoiceRecordButton(
                isRecording = isRecording,
                hasRecord = recordedFile != null,
                modifier = Modifier.weight(1f),
                onRecordToggle = {
                    if (isRecording) {
                        recorder.stop()
                        isRecording = false
                    } else {
                        sessionFiles.forEach { if(it.exists()) it.delete() }
                        sessionFiles.clear()
                        val file = File(context.filesDir, "voice_${System.currentTimeMillis()}.mp4")
                        recorder.start(file)
                        recordedFile = file
                        sessionFiles.add(file)
                        isRecording = true
                        voiceRemoved = false
                    }
                },
                onDeleteRecord = { 
                    if (recordedFile != null && sessionFiles.contains(recordedFile)) {
                        recordedFile?.delete()
                        sessionFiles.remove(recordedFile)
                    }
                    recordedFile = null 
                    voiceRemoved = true
                }
            )
        }
        
        if (selectedImageUri != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(Modifier.size(100.dp).clip(RoundedCornerShape(16.dp))) {
                AsyncImage(model = selectedImageUri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                IconButton(
                    onClick = { 
                        selectedImageUri = null
                        imageRemoved = true
                    }, 
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { 
                isSaved = true
                onSave(
                    title, description, value.takeIf { it.isNotBlank() },
                    recordedFile?.absolutePath, selectedImageUri?.toString(), 
                    isImportant, imageRemoved, voiceRemoved
                )
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Сохранить", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

@Composable
fun VoiceRecordButton(
    isRecording: Boolean,
    hasRecord: Boolean,
    modifier: Modifier = Modifier,
    onRecordToggle: () -> Unit,
    onDeleteRecord: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "scale"
    )

    Surface(
        onClick = onRecordToggle,
        modifier = modifier.scale(if (isRecording) scale else 1f),
        shape = RoundedCornerShape(16.dp),
        color = if (isRecording) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.background,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isRecording) Color.Red.copy(0.3f) else MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic, 
                null, 
                tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isRecording) "Стоп" else if (hasRecord) "Записано" else "Голос", 
                color = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (hasRecord && !isRecording) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.Close, 
                    null, 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.size(16.dp).clickable { onDeleteRecord() }
                )
            }
        }
    }
}

@Composable
fun EventTypeCard(
    type: HealthEventType,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = getBgColorByType(type)
    Surface(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(getIconByType(type), contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label, 
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), 
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingsClickableItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
}

fun getIconByType(type: HealthEventType): ImageVector = when(type) {
    HealthEventType.MEDICINE -> Icons.Default.MedicalServices
    HealthEventType.SICKNESS -> Icons.Default.Thermostat
    HealthEventType.FOOD_STOOL -> Icons.Default.Restaurant
    HealthEventType.SLEEP -> Icons.Default.Bedtime
    HealthEventType.HEIGHT -> Icons.Default.Straighten
    HealthEventType.WEIGHT -> Icons.Default.MonitorWeight
    HealthEventType.VACCINATION -> Icons.Default.Shield
    HealthEventType.ANALYSIS -> Icons.Default.Science
    HealthEventType.DOCTOR -> Icons.Default.Person
    HealthEventType.NOTE -> Icons.Default.Edit
}

fun getTitleByType(type: HealthEventType) = when(type) {
    HealthEventType.SICKNESS -> "Симптом"
    HealthEventType.MEDICINE -> "Лекарство"
    HealthEventType.FOOD_STOOL -> "Питание"
    HealthEventType.SLEEP -> "Сон"
    HealthEventType.HEIGHT -> "Рост"
    HealthEventType.WEIGHT -> "Вес"
    HealthEventType.VACCINATION -> "Прививка"
    HealthEventType.ANALYSIS -> "Анализы"
    HealthEventType.DOCTOR -> "Врач"
    HealthEventType.NOTE -> "Заметка"
}

fun getBgColorByType(type: HealthEventType) = when(type) {
    HealthEventType.SICKNESS -> EntryIllness
    HealthEventType.MEDICINE -> EntryMedication
    HealthEventType.FOOD_STOOL -> EntryFeeding
    HealthEventType.SLEEP -> EntrySleep
    HealthEventType.DOCTOR -> EntryDoctor
    HealthEventType.HEIGHT -> BoyBlue
    HealthEventType.WEIGHT -> BoyBlue
    else -> LavenderLight
}
