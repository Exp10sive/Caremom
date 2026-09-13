package ru.explosive.caremom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
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

data class VaccinationInfo(
    val name: String,
    val age: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val children by viewModel.children.collectAsState()
    val doneVaccines by viewModel.vaccinationDoneStatus.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()
    
    val selectedChild = remember(children, selectedChildId) {
        children.find { it.id == selectedChildId }
    }

    val vaccinationList = remember {
        listOf(
            VaccinationInfo("Гепатит В", "0-24 часа", "Первая вакцинация"),
            VaccinationInfo("Туберкулез (БЦЖ)", "3-7 дней", "Вакцинация в роддоме"),
            VaccinationInfo("Пневмококк", "2 месяца", "Первая вакцинация"),
            VaccinationInfo("АКДС", "3 месяца", "Первая вакцинация (коклюш, дифтерия, столбняк)"),
            VaccinationInfo("Полиомиелит", "3 месяца", "Первая вакцинация"),
            VaccinationInfo("Ротавирус", "3 месяца", "Первая вакцинация"),
            VaccinationInfo("АКДС (2)", "4.5 месяца", "Вторая вакцинация"),
            VaccinationInfo("Корь, Краснуха, Паротит", "12 месяцев", "Вакцинация")
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Прививки", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (children.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 80.dp)) {
                        Icon(Icons.Default.Shield, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.height(16.dp))
                        Text("Добавьте профиль ребенка для графика прививок", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                ChildSelector(
                    children = children,
                    selectedChild = selectedChild,
                    onChildSelected = { viewModel.selectChild(it.id) }
                )

                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = EntryDoctor.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EntryDoctor.copy(alpha = 0.1f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, null, tint = EntryDoctor)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Национальный календарь профилактических прививок",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EntryDoctor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    items(vaccinationList) { vac ->
                        val isDone = doneVaccines.any { it.contains(vac.name.lowercase()) }
                        
                        VaccinationCard(
                            vac = vac,
                            isDone = isDone,
                            onMarkDone = {
                                selectedChild?.let { child ->
                                    viewModel.addEvent(
                                        HealthEvent(
                                            childId = child.id,
                                            type = HealthEventType.VACCINATION,
                                            timestamp = System.currentTimeMillis(),
                                            title = "Прививка: ${vac.name}",
                                            description = vac.description
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaccinationCard(
    vac: VaccinationInfo,
    isDone: Boolean,
    onMarkDone: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (isDone) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surface,
        shadowElevation = if (isDone) 0.dp else 2.dp,
        border = if (isDone) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isDone) EntryDoctor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.Check else Icons.Default.Shield,
                    contentDescription = null,
                    tint = if (isDone) EntryDoctor else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vac.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = vac.age,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = vac.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (!isDone) {
                IconButton(
                    onClick = onMarkDone,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Отметить", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Text(
                    text = "Готово",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = EntryDoctor,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
