package ru.explosive.caremom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.explosive.caremom.data.Child
import ru.explosive.caremom.data.HealthEvent
import ru.explosive.caremom.data.HealthEventType
import ru.explosive.caremom.ui.theme.*
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.ui.components.ChildSelector
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTempScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val children by viewModel.children.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()
    val selectedChild = remember(children, selectedChildId) { children.find { it.id == selectedChildId } }
    
    var temperature by remember { mutableStateOf(36.6f) }

    // Если ребенок не выбран, выбираем первого доступного
    LaunchedEffect(children) {
        if (selectedChildId == null && children.isNotEmpty()) {
            viewModel.selectChild(children.first().id)
        }
    }

    val tempColor = when {
        temperature < 37.2f -> EntryDoctor
        temperature < 38.5f -> EntryFeeding
        else -> EntryIllness
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Градусник", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Измерение",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onBackground
            )

            ChildSelector(
                children = children,
                selectedChild = selectedChild,
                onChildSelected = { viewModel.selectChild(it.id) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(tempColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Thermostat, null, tint = tempColor, modifier = Modifier.size(32.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = String.format(Locale.US, "%.1f°C", temperature),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2).sp
                        ),
                        color = tempColor
                    )
                    
                    val statusText = when {
                        temperature < 37.2f -> "Нормальная"
                        temperature < 38.5f -> "Повышенная"
                        else -> "Высокая температура!"
                    }
                    
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = tempColor.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))

                    Slider(
                        value = temperature,
                        onValueChange = { temperature = it },
                        valueRange = 35f..41f,
                        steps = 59,
                        colors = SliderDefaults.colors(
                            thumbColor = tempColor,
                            activeTrackColor = tempColor,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("35.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("41.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    selectedChildId?.let { id ->
                        viewModel.addEvent(
                            event = HealthEvent(
                                childId = id,
                                type = HealthEventType.SICKNESS,
                                timestamp = System.currentTimeMillis(),
                                title = "Температура",
                                description = "Быстрое измерение",
                                value = String.format(Locale.US, "%.1f", temperature)
                            )
                        )
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = selectedChildId != null
            ) {
                Text("Сохранить результат", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
        }
    }
}
