package com.vencehoje.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.Category
import com.vencehoje.app.data.BillRepository
import com.vencehoje.app.ui.components.getIconPainterFromName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(repository: BillRepository, onBack: () -> Unit) {
    val categories by repository.allCategories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    if (categoryToEdit != null) {
        CategoryDialog(
            category = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onSave = { updatedCat ->
                scope.launch { repository.insertCategory(updatedCat) }
                categoryToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Categorias", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nova Categoria", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                CategoryItem(
                    category = category,
                    onClick = { categoryToEdit = category },
                    onDelete = {
                        if (category.id != 7) {
                            scope.launch { repository.deleteCategory(category) }
                        }
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newCat ->
                scope.launch { repository.insertCategory(newCat) }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CategoryDisplay(iconName: String, color: Color, modifier: Modifier = Modifier, size: Int = 24) {
    val isSystemIcon = iconName.length > 3 && !iconName.any { Character.isSurrogate(it) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isSystemIcon) {
            Icon(
                painter = getIconPainterFromName(iconName),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size.dp)
            )
        } else {
            Text(text = iconName, fontSize = (size - 4).sp)
        }
    }
}

@Composable
fun CategoryItem(category: Category, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val catColor = Color(android.graphics.Color.parseColor(category.colorHex))

            Box(
                modifier = Modifier.size(40.dp).background(catColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryDisplay(iconName = category.iconName, color = catColor)
            }

            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(text = category.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (category.id == 7) {
                    Text(text = "Sistema (Fixa)", fontSize = 10.sp, color = Color.Gray)
                }
            }

            if (category.id != 7) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color.LightGray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(
    category: Category? = null,
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: "#1976D2") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "label") }

    val scrollState = rememberScrollState()

    val colorPresets = listOf(
        "#1976D2", "#388E3C", "#FBC02D", "#7B1FA2",
        "#D32F2F", "#00796B", "#FF5722", "#9E9E9E", "#000000"
    )

    val iconPresets = listOf(
        "home", "directions_car", "shopping_cart",
        "celebration", "medical_services", "restaurant", "school", "label"
    )

    val isSystemIconActive = selectedIcon.length > 3 && !selectedIcon.any { Character.isSurrogate(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Nova Categoria" else "Editar Categoria", fontWeight = FontWeight.Black) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da Categoria") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Cor da Categoria", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorPresets.forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
                                .clickable { selectedColor = colorHex }
                                .padding(2.dp)
                        ) {
                            if (selectedColor == colorHex) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                Text("Ícone ou Emoji", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = if (isSystemIconActive) "" else selectedIcon,
                    onValueChange = { if (it.isNotEmpty()) selectedIcon = it },
                    label = { Text("Emoji ou selecione abaixo") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Cole um emoji aqui...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    trailingIcon = {
                        CategoryDisplay(
                            iconName = selectedIcon,
                            color = Color(android.graphics.Color.parseColor(selectedColor))
                        )
                    }
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    iconPresets.forEach { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isSelected) Color(0xFFC8E6C9) else Color(0xFFF5F5F5),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = getIconPainterFromName(iconName),
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF1B5E20) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(Category(
                            id = category?.id ?: 0,
                            name = name,
                            colorHex = selectedColor,
                            iconName = selectedIcon,
                            isBuiltIn = category?.isBuiltIn ?: false
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        }
    )
}