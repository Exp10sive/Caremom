package ru.explosive.caremom.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.explosive.caremom.data.HealthEvent
import ru.explosive.caremom.data.HealthEventType
import ru.explosive.caremom.ui.theme.*
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.ui.components.ChildSelector
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val stats by viewModel.statsData.collectAsState()
    val children by viewModel.children.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()
    val selectedChild = remember(children, selectedChildId) { children.find { it.id == selectedChildId } }
    
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Заголовок в стиле Дневника
            Text(
                text = "Статистика",
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Переключатель типов графиков
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 1.dp
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            divider = {}
                        ) {
                            val tabs = listOf("Рост", "Вес", "Темп.")
                            tabs.forEachIndexed { index, title ->
                                Tab(selected = selectedTab == index, onClick = { selectedTab = index }) {
                                    Text(title, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Основной график
                item {
                    val currentData = when(selectedTab) {
                        0 -> stats.heightData
                        1 -> stats.weightData
                        else -> stats.tempData
                    }
                    val chartColor = when(selectedTab) {
                        0 -> BoyBlue
                        1 -> LavenderLight
                        else -> EntryIllness
                    }
                    val unit = when(selectedTab) {
                        0 -> "см"
                        1 -> "кг"
                        else -> "°C"
                    }

                    Column(Modifier.padding(horizontal = 20.dp)) {
                        if (currentData.size >= 2) {
                            LineChartCard(currentData, chartColor, unit)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MiniStatCard("Мин", "${currentData.min()} $unit", modifier = Modifier.weight(1f))
                                MiniStatCard("Макс", "${currentData.max()} $unit", modifier = Modifier.weight(1f))
                                MiniStatCard("Посл.", "${currentData.last()} $unit", modifier = Modifier.weight(1f))
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(28.dp),
                                shadowElevation = 1.dp
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(24.dp)) {
                                    Text(
                                        text = if (selectedChild == null) "Выберите ребенка для просмотра графиков" else "Недостаточно данных для графика ${selectedChild.name}", 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Календарь здоровья
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = "Календарь здоровья", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(12.dp))
                        HealthCalendar(stats.sicknessDates)
                    }
                }

                // Популярные симптомы
                if (stats.topSymptoms.isNotEmpty()) {
                    item {
                        Column(Modifier.padding(horizontal = 20.dp)) {
                            Text(
                                text = "Частые симптомы", 
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    stats.topSymptoms.forEachIndexed { index, (name, count) ->
                                        SymptomRow(name.replaceFirstChar { it.uppercase() }, count)
                                        if (index < stats.topSymptoms.size - 1) {
                                            HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineChartCard(data: List<Float>, color: Color, unit: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(220.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val minVal = data.min() * 0.98f
                val maxVal = data.max() * 1.02f
                val range = maxVal - minVal
                
                val points = data.mapIndexed { i, value ->
                    val x = if (data.size > 1) i * (width / (data.size - 1)) else width / 2
                    val normalizedValue = (value - minVal) / range
                    val y = height - (normalizedValue * height)
                    Offset(x, y)
                }
                
                // Draw Area Gradient
                if (points.isNotEmpty()) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, height)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
                }
                
                // Draw Line
                val path = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(path, color, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                
                // Draw dots
                points.forEach { 
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = it)
                    drawCircle(color, radius = 4.dp.toPx(), center = it, style = Stroke(width = 2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
fun MiniStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun SymptomRow(name: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = CircleShape
        ) {
            Text(
                text = "$count раз",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HealthCalendar(sicknessDates: Set<Pair<Int, Int>>) {
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("ru"))
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year = calendar.get(Calendar.YEAR)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "$currentMonth $year",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            val chunks = (1..daysInMonth).chunked(7)
            chunks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    week.forEach { day ->
                        val isSick = sicknessDates.contains(getDayOfYear(day, calendar) to year)
                        DayItem(day, isSick)
                    }
                    repeat(7 - week.size) { Spacer(modifier = Modifier.size(40.dp)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun getDayOfYear(day: Int, calendar: Calendar): Int {
    val tempCal = calendar.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, day)
    return tempCal.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun DayItem(day: Int, isSick: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSick) EntryIllness.copy(alpha = 0.15f) else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSick) EntryIllness else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSick) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
