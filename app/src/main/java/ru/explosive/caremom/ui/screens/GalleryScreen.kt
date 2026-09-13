package ru.explosive.caremom.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ru.explosive.caremom.data.Child
import ru.explosive.caremom.ui.theme.*
import ru.explosive.caremom.ui.viewmodel.MainViewModel
import ru.explosive.caremom.ui.components.ChildSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val events by viewModel.allEvents.collectAsState()
    val children by viewModel.children.collectAsState()
    val selectedChildId by viewModel.selectedChildId.collectAsState()
    
    val selectedChild = remember(children, selectedChildId) { 
        children.find { it.id == selectedChildId } 
    }

    val allPhotos = remember(events, selectedChildId) {
        events.filter { 
            (selectedChildId == null || it.childId == selectedChildId) && !it.imagePath.isNullOrEmpty() 
        }.map { it.imagePath!! }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text(
                text = "Галерея",
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
                onChildSelected = { child -> 
                    // Если нажимаем на уже выбранного, сбрасываем фильтр (показываем всех)
                    if (selectedChildId == child.id) {
                        viewModel.selectChild(null)
                    } else {
                        viewModel.selectChild(child.id)
                    }
                }
            )

            if (allPhotos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 80.dp)) {
                        Icon(Icons.Default.PhotoLibrary, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primaryContainer)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (selectedChildId == null) "Нет фотографий" else "Нет фото для ${selectedChild?.name}", 
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allPhotos) { photoPath ->
                        Surface(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clickable { onImageClick(photoPath) },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp
                        ) {
                            AsyncImage(
                                model = photoPath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}
