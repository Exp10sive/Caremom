package ru.explosive.caremom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ru.explosive.caremom.data.Child
import ru.explosive.caremom.ui.components.ChildEntryDialog
import ru.explosive.caremom.ui.components.InfoBadge
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.util.calculateAgeRussian

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: MainViewModel,
    onChildClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val children by viewModel.children.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var childToEdit by remember { mutableStateOf<Child?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Дети", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
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
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (children.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Face, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.height(16.dp))
                        Text("Добавьте профиль вашего ребенка", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = children, key = { it.id }) { child ->
                        ChildProfileCard(
                            child = child,
                            onClick = { onChildClick(child.id) },
                            onEdit = { childToEdit = child }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ChildEntryDialog(
            title = "Новый профиль",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, birthday, photoUri, blood, allergies, chronic, _ ->
                viewModel.addChild(name, birthday, photoUri, blood, allergies, chronic)
                showAddDialog = false
            }
        )
    }

    childToEdit?.let { child ->
        var showDeleteConfirm by remember { mutableStateOf(false) }
        
        ChildEntryDialog(
            title = "Редактирование",
            initialChild = child,
            onDismiss = { childToEdit = null },
            onConfirm = { name, birthday, photoUri, blood, allergies, chronic, isRemoved ->
                viewModel.updateChild(
                    child = child.copy(
                        name = name,
                        birthday = birthday,
                        bloodType = blood,
                        allergies = allergies,
                        chronicDiseases = chronic
                    ),
                    newPhotoUri = if (photoUri != child.photoPath) photoUri else null,
                    photoRemoved = isRemoved
                )
                childToEdit = null
            },
            extraContent = {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color(0xFFEF5350)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = null
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Удалить профиль")
                }
            }
        )

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Удалить профиль?", fontWeight = FontWeight.Bold) },
                text = { Text("Все записи и данные о здоровье ${child.name} будут удалены безвозвратно.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteChild(child)
                        showDeleteConfirm = false
                        childToEdit = null
                    }) { Text("Удалить", color = Color(0xFFEF5350), fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun ChildProfileCard(child: Child, onClick: () -> Unit, onEdit: () -> Unit) {
    val age = remember(child.birthday) { calculateAgeRussian(child.birthday) }
    
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
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
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
                        Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = child.name, 
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), 
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = age, 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            
            if (!child.bloodType.isNullOrBlank() || !child.allergies.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!child.bloodType.isNullOrBlank()) {
                        InfoBadge(label = "Группа: ${child.bloodType}", color = MaterialTheme.colorScheme.primary)
                    }
                    if (!child.allergies.isNullOrBlank()) {
                        InfoBadge(label = "Аллергия", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}
