package ru.explosive.caremom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.explosive.caremom.data.HealthEvent
import ru.explosive.caremom.data.HealthEventType
import ru.explosive.caremom.ui.theme.*
import ru.explosive.caremom.ui.components.SettingsClickableItem
import ru.explosive.caremom.ui.components.EventEntryForm
import ru.explosive.caremom.ui.components.ChildSelector
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.util.AudioRecorder
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    onNavigateToProfiles: () -> Unit,
    onNavigateToVaccines: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }
    val selectedChildId by viewModel.selectedChildId.collectAsState()
    val children by viewModel.children.collectAsState()
    val selectedChild = children.find { it.id == selectedChildId }

    var showQuickAddSheet by remember { mutableStateOf(false) }
    var quickAddType by remember { mutableStateOf<HealthEventType?>(null) }

    LaunchedEffect(children) {
        if (selectedChildId == null && children.isNotEmpty()) {
            viewModel.selectChild(children.first().id)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "Еще",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp)
            )

            ChildSelector(
                children = children,
                selectedChild = selectedChild,
                onChildSelected = { viewModel.selectChild(it.id) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Секция семьи
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Семья",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        MoreMenuCard(
                            title = "Профили детей",
                            icon = Icons.Default.Face,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onNavigateToProfiles
                        )
                    }
                }

                // Быстрые действия
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Быстрые действия",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MoreMenuCard(
                                title = "Добавить рост",
                                icon = Icons.Default.Straighten,
                                color = BoyBlue,
                                modifier = Modifier.weight(1f),
                                onClick = { 
                                    if (selectedChildId != null) {
                                        quickAddType = HealthEventType.HEIGHT
                                        showQuickAddSheet = true
                                    }
                                }
                            )
                            MoreMenuCard(
                                title = "Добавить вес",
                                icon = Icons.Default.MonitorWeight,
                                color = EntryFeeding,
                                modifier = Modifier.weight(1f),
                                onClick = { 
                                    if (selectedChildId != null) {
                                        quickAddType = HealthEventType.WEIGHT
                                        showQuickAddSheet = true
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Приложение",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SettingsClickableItem(
                                title = "Прививки",
                                subtitle = "Календарь вакцинации",
                                icon = Icons.Default.Shield,
                                onClick = onNavigateToVaccines
                            )
                            SettingsClickableItem(
                                title = "Настройки",
                                subtitle = "Тема и уведомления",
                                icon = Icons.Default.Settings,
                                onClick = onNavigateToSettings
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQuickAddSheet && quickAddType != null && selectedChildId != null) {
        ModalBottomSheet(
            onDismissRequest = { showQuickAddSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primaryContainer) }
        ) {
            EventEntryForm(
                type = quickAddType!!,
                recorder = recorder,
                onDismiss = { showQuickAddSheet = false },
                onSave = { title, desc, valStr, voicePath, imageUri, isImportant, _, _ ->
                    viewModel.addEvent(
                        event = HealthEvent(
                            childId = selectedChildId!!,
                            type = quickAddType!!,
                            timestamp = System.currentTimeMillis(),
                            title = title,
                            description = desc,
                            value = valStr,
                            voicePath = voicePath,
                            isImportant = isImportant
                        ),
                        imageUri = imageUri
                    )
                    showQuickAddSheet = false
                }
            )
        }
    }
}

@Composable
fun MoreMenuCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
