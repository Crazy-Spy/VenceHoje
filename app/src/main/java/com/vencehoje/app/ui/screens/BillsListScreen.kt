package com.vencehoje.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.BillRepository
import com.vencehoje.app.logic.*
import com.vencehoje.app.ui.components.BillCard
import com.vencehoje.app.ui.components.SummaryCard
import com.vencehoje.app.ui.dialogs.AddEditBillDialog
import com.vencehoje.app.ui.dialogs.LatePaymentDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillsListScreen(repository: BillRepository, onMenuClick: () -> Unit) {
    // Coleta as contas e as categorias de forma reativa
    val bills by repository.allBills.collectAsState(initial = emptyList())
    val categories by repository.allCategories.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<Bill?>(null) }
    var billToPayAtLate by remember { mutableStateOf<Bill?>(null) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas Contas", fontWeight = FontWeight.Black) },
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
        val filteredList = if (selectedTab == 0) bills.filter { !it.isPaid } else bills.filter { it.isPaid }

        // Ordenação lógica por data
        val sortedList = filteredList.sortedBy { bill ->
            val dateStr = if (selectedTab == 0) bill.dueDate else bill.paymentDate ?: bill.dueDate
            LocalDate.parse(dateStr, formatter)
        }.let { if (selectedTab == 1) it.reversed() else it }

        // Agrupamento por Mês/Ano para o StickyHeader
        val groupedBills = sortedList.groupBy { bill ->
            val dateStr = if (selectedTab == 0) bill.dueDate else bill.paymentDate ?: bill.dueDate
            val date = LocalDate.parse(dateStr, formatter)
            val month = date.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
            "$month / ${date.year}"
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = monthYear,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }

                    items(monthBills) { bill ->
                        // MATCH: Encontra a categoria correspondente para o card
                        val categoryObj = categories.find { it.id == bill.categoryId }

                        BillCard(
                            bill = bill,
                            category = categoryObj,
                            onDelete = { scope.launch { repository.delete(bill) } },
                            onEdit = { billToEdit = bill },
                            onPay = {
                                val numericValue = bill.value.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
                                val isZeroValue = numericValue == 0L
                                val isLate = getDaysRemaining(bill.dueDate) < 0

                                if (isLate || isZeroValue) {
                                    billToPayAtLate = bill
                                } else {
                                    scope.launch { processPayment(bill, repository, bill.value) }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // DIÁLOGOS
    if (showAddDialog) {
        AddEditBillDialog(
            repository = repository, // Novo parâmetro necessário
            onDismiss = { showAddDialog = false },
            onSave = { scope.launch { repository.insert(it) }; showAddDialog = false }
        )
    }

    if (billToEdit != null) {
        AddEditBillDialog(
            bill = billToEdit,
            repository = repository, // Novo parâmetro necessário
            onDismiss = { billToEdit = null },
            onSave = { scope.launch { repository.update(it) }; billToEdit = null }
        )
    }

    if (billToPayAtLate != null) {
        LatePaymentDialog(
            bill = billToPayAtLate!!,
            onDismiss = { billToPayAtLate = null },
            onConfirm = { finalVal ->
                scope.launch {
                    processPayment(billToPayAtLate!!, repository, finalVal)
                    billToPayAtLate = null
                }
            }
        )
    }
}