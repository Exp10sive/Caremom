package ru.explosive.caremom.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import ru.explosive.caremom.data.HealthEventType
import ru.explosive.caremom.data.MedicalDocument
import ru.explosive.caremom.ui.theme.*
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.util.PdfExporter
import ru.explosive.caremom.util.calculateAgeRussian
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDetailScreen(
    childId: Long,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onDocumentClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val children by viewModel.children.collectAsState()
    val child = remember(children, childId) { children.find { it.id == childId } }
    
    val documents by remember(childId) { viewModel.getDocumentsForChild(childId) }.collectAsState(initial = emptyList())
    val events by remember(childId) { viewModel.getEventsForChild(childId) }.collectAsState(initial = emptyList())
    
    val pdfExporter = remember { PdfExporter(context) }

    val lastWeight = remember(events) { 
        events.filter { it.type == HealthEventType.WEIGHT }.sortedByDescending { it.timestamp }.firstOrNull()?.value 
    }
    val lastHeight = remember(events) { 
        events.filter { it.type == HealthEventType.HEIGHT }.sortedByDescending { it.timestamp }.firstOrNull()?.value 
    }

    var pendingDocUri by remember { mutableStateOf<Uri?>(null) }
    var showNameDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingDocUri = uri
            showNameDialog = true
        }
    }

    if (child == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ребенок не найден", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val file = pdfExporter.exportChildHistory(child, events)
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Открыть отчет"))
                            }
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Экспорт", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = { photoPickerLauncher.launch("image/*") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Добавить фото", modifier = Modifier.size(32.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header Section
            Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                if (child.photoPath != null) {
                    AsyncImage(
                        model = child.photoPath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Face, null, Modifier.size(120.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    }
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.8f), MaterialTheme.colorScheme.background),
                                startY = 400f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                ) {
                    Text(
                        text = child.name,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 36.sp,
                            letterSpacing = (-1).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = calculateAgeRussian(child.birthday),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Info Cards
            Row(
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GrowthIndicatorCard(
                    label = "Вес",
                    value = if (lastWeight != null) "$lastWeight кг" else "--",
                    icon = Icons.Default.MonitorWeight,
                    color = BoyBlue,
                    modifier = Modifier.weight(1f)
                )
                GrowthIndicatorCard(
                    label = "Рост",
                    value = if (lastHeight != null) "$lastHeight см" else "--",
                    icon = Icons.Default.Straighten,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Medical Data Card
            Surface(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        "Медицинская карта",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    InfoDetailRow("Группа крови", child.bloodType ?: "Не указана", icon = Icons.Default.WaterDrop, iconColor = Color.Red)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    InfoDetailRow("Аллергии", child.allergies ?: "Нет", icon = Icons.Default.Warning, iconColor = WarmCoralLight, isAlert = !child.allergies.isNullOrBlank())
                    HorizontalDivider(Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    InfoDetailRow("Особенности", child.chronicDiseases ?: "Нет", icon = Icons.Default.HealthAndSafety, iconColor = EntryDoctor)
                }
            }

            // Documents Gallery
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    "Документы и анализы",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (documents.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Нет загруженных документов", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    documents.chunked(2).forEach { rowDocs ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowDocs.forEach { doc ->
                                Box(Modifier.weight(1f)) {
                                    DocumentThumbnail(
                                        document = doc,
                                        onClick = { onDocumentClick(doc.id) }
                                    )
                                }
                            }
                            if (rowDocs.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
        }
    }

    if (showNameDialog && pendingDocUri != null) {
        var docName by remember { mutableStateOf("Анализ от ${SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date())}") }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Название документа", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = docName,
                    onValueChange = { docName = it },
                    label = { Text("Введите название") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addDocument(childId, docName, pendingDocUri!!)
                    showNameDialog = false
                    pendingDocUri = null
                }) { Text("Сохранить", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNameDialog = false
                    pendingDocUri = null
                }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp)
        )
    }
}

@Composable
fun GrowthIndicatorCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, modifier = Modifier.size(22.dp), tint = color)
            }
            Spacer(Modifier.height(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value, 
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), 
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun InfoDetailRow(label: String, value: String, icon: ImageVector, iconColor: Color, isAlert: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = iconColor)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value, 
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), 
                color = if (isAlert) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun DocumentThumbnail(document: MedicalDocument, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(0.85f),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column {
            AsyncImage(
                model = document.imageUri,
                contentDescription = document.title,
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = document.title,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
