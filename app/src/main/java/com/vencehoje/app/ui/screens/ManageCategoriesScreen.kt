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
fun ManageCategoriesScreen(
    repository: BillRepository,
    profileId: Int,
    onBack: () -> Unit,
    onProfileChange: (Int) -> Unit // <--- NOVO PARÂMETRO
) {
    val categories by remember(profileId) { repository.getCategoriesByProfile(profileId) }
        .collectAsState(initial = emptyList())

    val allProfiles by repository.allProfiles.collectAsState(initial = emptyList())
    val currentProfile = allProfiles.find { it.id == profileId }

    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    // Estado do Menu Dropdown
    var isProfileMenuExpanded by remember { mutableStateOf(false) }

    if (categoryToEdit != null) {
        CategoryDialog(
            category = categoryToEdit,
            onDismiss = { categoryToEdit = null },
            onSave = { updatedCat ->
                scope.launch {
                    repository.insertCategory(updatedCat.copy(profileId = profileId))
                }
                categoryToEdit = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Categorias", fontWeight = FontWeight.Black)

                        Spacer(modifier = Modifier.width(12.dp))

                        // Chip Visual INTERATIVO
                        Surface(
                            modifier = Modifier.clickable { isProfileMenuExpanded = true },
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            try { Color(android.graphics.Color.parseColor(currentProfile?.colorHex ?: "#FFFFFF")) }
                                            catch (e: Exception) { Color.White },
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentProfile?.name ?: "...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }

                            // Menu Dropdown
                            DropdownMenu(
                                expanded = isProfileMenuExpanded,
                                onDismissRequest = { isProfileMenuExpanded = false }
                            ) {
                                allProfiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = { Text(profile.name, fontWeight = if(profile.id == profileId) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            onProfileChange(profile.id)
                                            isProfileMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(
                                                        try { Color(android.graphics.Color.parseColor(profile.colorHex)) }
                                                        catch (e: Exception) { Color.Gray },
                                                        CircleShape
                                                    )
                                            )
                                        },
                                        trailingIcon = {
                                            if (profile.id == profileId) {
                                                Icon(Icons.Default.Check, null, tint = Color(0xFF1B5E20))
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
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
        // ... (O resto da LazyColumn mantém igual)
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
                        if (!category.isBuiltIn && category.id != 7) {
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
                scope.launch {
                    repository.insertCategory(newCat.copy(profileId = profileId))
                }
                showAddDialog = false
            }
        )
    }
}
// ... (CategoryDisplay, CategoryItem, CategoryDialog mantidos iguais)
@Composable
fun CategoryDisplay(iconName: String, color: Color, modifier: Modifier = Modifier, size: Int = 24) {
    val isSystemIcon = iconName.length > 3 && !iconName.any { Character.isSurrogate(it) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isSystemIcon) {
            Icon(painter = getIconPainterFromName(iconName), contentDescription = null, tint = color, modifier = Modifier.size(size.dp))
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
            val catColor = try { Color(android.graphics.Color.parseColor(category.colorHex)) } catch (e: Exception) { Color.Gray }
            Box(
                modifier = Modifier.size(40.dp).background(catColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CategoryDisplay(iconName = category.iconName, color = catColor)
            }
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                Text(text = category.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (category.isBuiltIn || category.id == 7) {
                    Text(text = "Sistema (Fixa)", fontSize = 10.sp, color = Color.Gray)
                }
            }
            if (!category.isBuiltIn && category.id != 7) {
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryDialog(category: Category? = null, onDismiss: () -> Unit, onSave: (Category) -> Unit) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var selectedColor by remember { mutableStateOf(category?.colorHex ?: "#1976D2") }
    var selectedIcon by remember { mutableStateOf(category?.iconName ?: "label") }
    val scrollState = rememberScrollState()
    val colorPresets = listOf("#1976D2", "#388E3C", "#FBC02D", "#7B1FA2", "#D32F2F", "#00796B", "#FF5722", "#9E9E9E", "#000000")
    val iconPresets = listOf("home", "directions_car", "shopping_cart", "celebration", "medical_services", "restaurant", "school", "label")
    val isSystemIconActive = selectedIcon.length > 3 && !selectedIcon.any { Character.isSurrogate(it) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Nova Categoria" else "Editar Categoria", fontWeight = FontWeight.Black) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome da Categoria") }, modifier = Modifier.fillMaxWidth())
                Text("Cor da Categoria", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorPresets.forEach { colorHex ->
                        Box(modifier = Modifier.size(36.dp).background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape).clickable { selectedColor = colorHex }.padding(2.dp)) {
                            if (selectedColor == colorHex) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp))
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
                        val displayColor = try { Color(android.graphics.Color.parseColor(selectedColor)) } catch(e: Exception) { Color.Gray }
                        CategoryDisplay(iconName = selectedIcon, color = displayColor)
                    }
                )
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    iconPresets.forEach { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(modifier = Modifier.size(44.dp).background(if (isSelected) Color(0xFFC8E6C9) else Color(0xFFF5F5F5), RoundedCornerShape(8.dp)).clickable { selectedIcon = iconName }, contentAlignment = Alignment.Center) {
                            Icon(painter = getIconPainterFromName(iconName), contentDescription = null, tint = if (isSelected) Color(0xFF1B5E20) else Color.Gray, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(Category(id = category?.id ?: 0, name = name, colorHex = selectedColor, iconName = selectedIcon, isBuiltIn = category?.isBuiltIn ?: false)) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } }
    )
}