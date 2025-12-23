package com.vencehoje.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.BillRepository
import com.vencehoje.app.data.Profile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageProfilesScreen(
    repository: BillRepository,
    currentProfileId: Int, // Necessário para saber qual não podemos deletar agora
    onBack: () -> Unit
) {
    val profiles by repository.allProfiles.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Estados para os Diálogos
    var showEditDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var profileToEdit by remember { mutableStateOf<Profile?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Profile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Perfis", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    // BOTÃO ADICIONAR (+)
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Novo Perfil", tint = Color.White)
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
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(profiles) { profile ->
                ProfileItem(
                    profile = profile,
                    isCurrent = profile.id == currentProfileId,
                    isMain = profile.id == 1, // Regra de Ouro: ID 1 é o sistema
                    onEdit = {
                        profileToEdit = profile
                        showEditDialog = true
                    },
                    onDelete = { showDeleteConfirm = profile }
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                if (profiles.any { it.id == currentProfileId && it.id != 1 }) {
                    Text(
                        "Nota: Para deletar o perfil que você está usando agora, mude para outro perfil primeiro.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }

    // --- DIALOG DE ADICIONAR ---
    if (showAddDialog) {
        ProfileDialog(
            profile = Profile(name = "", colorHex = "#FBC02D"), // Padrão vazio
            title = "Novo Perfil",
            onDismiss = { showAddDialog = false },
            onSave = { newProfile ->
                scope.launch {
                    repository.insertProfile(newProfile)
                    showAddDialog = false
                }
            }
        )
    }

    // --- DIALOG DE EDIÇÃO ---
    if (showEditDialog && profileToEdit != null) {
        ProfileDialog(
            profile = profileToEdit!!,
            title = "Editar Perfil",
            onDismiss = {
                showEditDialog = false
                profileToEdit = null
            },
            onSave = { updatedProfile ->
                scope.launch {
                    repository.updateProfile(updatedProfile)
                    showEditDialog = false
                    profileToEdit = null
                }
            }
        )
    }

    // --- DIALOG DE CONFIRMAÇÃO DE EXCLUSÃO ---
    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Excluir Perfil?", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C)) },
            text = {
                Column {
                    Text("Você tem certeza? Isso apagará:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("• O perfil '${showDeleteConfirm?.name}'")
                    Text("• TODAS as contas e categorias dele")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Essa ação não pode ser desfeita.", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            showDeleteConfirm?.let {
                                // LIMPEZA TOTAL: Apaga contas e o perfil
                                repository.clearBillsByProfile(it.id)
                                repository.deleteProfile(it)
                            }
                            showDeleteConfirm = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) { Text("Excluir Tudo") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun ProfileItem(
    profile: Profile,
    isCurrent: Boolean,
    isMain: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bolinha da Cor
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        try { Color(android.graphics.Color.parseColor(profile.colorHex)) } catch (e: Exception) { Color.Gray }
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (isMain) {
                    Text(text = "Perfil Principal (Sistema)", fontSize = 11.sp, color = Color.Gray)
                }
                if (isCurrent) {
                    Text(text = "● Em uso agora", fontSize = 11.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                }
            }

            // Botão Editar
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, null, tint = Color.Gray)
            }

            // Botão Deletar
            // LÓGICA: Só aparece se NÃO for o Principal e NÃO for o atual
            if (!isMain && !isCurrent) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFB71C1C))
                }
            } else {
                // Espaço vazio (placeholder) para manter alinhamento
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun ProfileDialog(
    profile: Profile,
    title: String,
    onDismiss: () -> Unit,
    onSave: (Profile) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var selectedColor by remember { mutableStateOf(profile.colorHex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Perfil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cor do Perfil:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("#D32F2F", "#1976D2", "#388E3C", "#FBC02D", "#7B1FA2", "#FF5722", "#000000").forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .clickable { selectedColor = color }
                                .border(
                                    width = if(selectedColor == color) 3.dp else 0.dp,
                                    color = if(selectedColor == color) Color.Black else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(profile.copy(name = name, colorHex = selectedColor))
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