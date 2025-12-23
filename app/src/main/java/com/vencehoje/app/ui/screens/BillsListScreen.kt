package com.vencehoje.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.BillRepository
import com.vencehoje.app.data.Profile
import com.vencehoje.app.logic.*
import com.vencehoje.app.ui.components.BillCard
import com.vencehoje.app.ui.components.SummaryCard
import com.vencehoje.app.ui.components.AddEditBillDialog
import com.vencehoje.app.ui.components.LatePaymentDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillsListScreen(
    repository: BillRepository,
    profileId: Int,
    onMenuClick: () -> Unit,
    onProfileChange: (Int) -> Unit,
    onManageProfiles: () -> Unit
) {
    // 1. Dados das Contas (Filtrado pelo Perfil)
    val bills by remember(profileId) { repository.getBillsByProfile(profileId) }
        .collectAsState(initial = emptyList())
    val categories by remember(profileId) { repository.getCategoriesByProfile(profileId) }
        .collectAsState(initial = emptyList())

    // 2. Dados dos Perfis
    val allProfiles by repository.allProfiles.collectAsState(initial = emptyList())
    val currentProfile = allProfiles.find { it.id == profileId }

    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showNewProfileDialog by remember { mutableStateOf(false) }
    var isProfileMenuExpanded by remember { mutableStateOf(false) }

    var billToEdit by remember { mutableStateOf<Bill?>(null) }
    var billToPayAtLate by remember { mutableStateOf<Bill?>(null) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                // --- AQUI ESTÁ A MUDANÇA VISUAL ---
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Título
                        Text("Minhas Contas", fontWeight = FontWeight.Bold, fontSize = 20.sp)

                        // 2. Espaço
                        Spacer(modifier = Modifier.width(12.dp))

                        // 3. Chip clicável (Botão do Perfil)
                        Surface(
                            modifier = Modifier
                                .clickable { isProfileMenuExpanded = true },
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50) // Bem arredondado
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bolinha colorida
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
                                    text = currentProfile?.name ?: "Carregando...",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // O Menu Dropdown
                            DropdownMenu(
                                expanded = isProfileMenuExpanded,
                                onDismissRequest = { isProfileMenuExpanded = false }
                            ) {
                                Text("Trocar Perfil", modifier = Modifier.padding(8.dp), fontSize = 12.sp, color = Color.Gray)

                                allProfiles.forEach { profile ->
                                    DropdownMenuItem(
                                        text = { Text(profile.name, fontWeight = if(profile.id == profileId) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            onProfileChange(profile.id)
                                            isProfileMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            // Bolinha da cor do perfil no menu também
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
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("➕ Novo Perfil") },
                                    onClick = {
                                        isProfileMenuExpanded = false
                                        showNewProfileDialog = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⚙️ Gerenciar Perfis") },
                                    onClick = {
                                        isProfileMenuExpanded = false
                                        onManageProfiles()
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
    ) { padding ->
        // Lógica de listagem (igual ao anterior)
        val filteredList = if (selectedTab == 0) bills.filter { !it.isPaid } else bills.filter { it.isPaid }
        val sortedList = filteredList.sortedBy { bill ->
            val dateStr = if (selectedTab == 0) bill.dueDate else bill.paymentDate ?: bill.dueDate
            try { LocalDate.parse(dateStr, formatter) } catch(e: Exception) { LocalDate.now() }
        }.let { if (selectedTab == 1) it.reversed() else it }

        val groupedBills = sortedList.groupBy { bill ->
            val dateStr = if (selectedTab == 0) bill.dueDate else bill.paymentDate ?: bill.dueDate
            try {
                val date = LocalDate.parse(dateStr, formatter)
                val month = date.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                "$month / ${date.year}"
            } catch (e: Exception) { "Data Inválida" }
        }

        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color(0xFFC8E6C9)) {
                listOf("A PAGAR", "PAGAS").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                color = if(selectedTab == index) Color(0xFF1B5E20) else Color.Gray
                            )
                        }
                    )
                }
            }

            SummaryCard(bills = bills, isHistoryTab = selectedTab == 1)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groupedBills.forEach { (monthYear, monthBills) ->
                    stickyHeader {
                        Box(
                            modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(8.dp)
                        ) {
                            Text(monthYear, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                        }
                    }
                    items(monthBills) { bill ->
                        val categoryObj = categories.find { it.id == bill.categoryId }
                        BillCard(
                            bill = bill,
                            category = categoryObj,
                            onDelete = { scope.launch { repository.deleteBill(bill) } },
                            onEdit = { billToEdit = bill },
                            onPay = {
                                val numericValue = bill.value.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                                val isZeroValue = numericValue == 0L
                                val isLate = getDaysRemaining(bill.dueDate) < 0
                                if (isLate || isZeroValue) billToPayAtLate = bill
                                else scope.launch { processPayment(bill, repository, bill.value) }
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG PARA CRIAR NOVO PERFIL ---
    if (showNewProfileDialog) {
        var newProfileName by remember { mutableStateOf("") }
        var newProfileColor by remember { mutableStateOf("#FBC02D") }

        AlertDialog(
            onDismissRequest = { showNewProfileDialog = false },
            title = { Text("Criar Novo Perfil") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("Nome (ex: Casa do Pai)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Escolha uma cor:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("#D32F2F", "#1976D2", "#388E3C", "#FBC02D", "#7B1FA2").forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { newProfileColor = color }
                                    .border(
                                        width = if(newProfileColor == color) 3.dp else 0.dp,
                                        color = if(newProfileColor == color) Color.Black else Color.Transparent,
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
                        if (newProfileName.isNotBlank()) {
                            scope.launch {
                                val newProfile = Profile(name = newProfileName, colorHex = newProfileColor)
                                repository.insertProfile(newProfile)
                                showNewProfileDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) { Text("Criar") }
            },
            dismissButton = {
                TextButton(onClick = { showNewProfileDialog = false }) { Text("Cancelar", color = Color.Gray) }
            }
        )
    }

    // Outros diálogos
    if (showAddDialog) {
        AddEditBillDialog(
            repository = repository,
            profileId = profileId,
            onDismiss = { showAddDialog = false },
            onSave = { newBill ->
                scope.launch { repository.insertBill(newBill.copy(profileId = profileId)) }
                showAddDialog = false
            }
        )
    }

    if (billToEdit != null) {
        AddEditBillDialog(
            bill = billToEdit,
            repository = repository,
            profileId = profileId,
            onDismiss = { billToEdit = null },
            onSave = { updatedBill ->
                scope.launch { repository.updateBill(updatedBill) }
                billToEdit = null
            }
        )
    }

    if (billToPayAtLate != null) {
        LatePaymentDialog(
            bill = billToPayAtLate!!,
            onDismiss = { billToPayAtLate = null },
            onConfirm = { finalVal ->
                scope.launch { processPayment(billToPayAtLate!!, repository, finalVal); billToPayAtLate = null }
            }
        )
    }
}