package com.vencehoje.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val repository = BillRepository(database.billDao())
        setupInitialWorker(this)
        setContent {
            VenceHojeTheme {
                NotificationPermissionHandler()
                MainScreen(repository)
            }
        }
    }

    private fun setupInitialWorker(context: Context) {
        val request = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork("vencehoje_loop", ExistingWorkPolicy.KEEP, request)
    }
}

@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(repository: BillRepository) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var currentScreen by remember { mutableStateOf("home") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "VenceHoje",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color(0xFF1B5E20)
                )
                Divider()
                NavigationDrawerItem(
                    label = { Text("Minhas Contas") },
                    selected = currentScreen == "home",
                    onClick = {
                        if (currentScreen != "home") currentScreen = "home"
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Home, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = currentScreen == "charts",
                    onClick = { currentScreen = "charts"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Info, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Configurações") },
                    selected = currentScreen == "configs",
                    onClick = { currentScreen = "configs"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Settings, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Sobre o App") },
                    selected = currentScreen == "about",
                    onClick = { currentScreen = "about"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Favorite, null) }
                )
                NavigationDrawerItem(
                    label = { Text("Reportar Bug/Sugestão") },
                    selected = currentScreen == "report",
                    onClick = { currentScreen = "report"; scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Info, null) }
                )
            }
        }
    ) {
        when(currentScreen) {
            "home" -> BillsListScreen(repository, onMenuClick = { scope.launch { drawerState.open() } })
            "charts" -> DashboardScreen(repository, onBack = { currentScreen = "home" })
            "configs" -> SettingsScreen(repository = repository, onBack = { currentScreen = "home" })
            "about" -> SobreScreen(onBack = { currentScreen = "home" })
            "report" -> ReportarScreen(onBack = { currentScreen = "home" })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BillsListScreen(repository: BillRepository, onMenuClick: () -> Unit) {
    val bills by repository.allBills.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var billToEdit by remember { mutableStateOf<Bill?>(null) }
    var billToPayAtLate by remember { mutableStateOf<Bill?>(null) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Minhas Contas", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Color(0xFF1B5E20)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    ) { padding ->
        val filteredList = if (selectedTab == 0) bills.filter { !it.isPaid } else bills.filter { it.isPaid }
        val sortedList = filteredList.sortedBy { bill ->
            val dateStr = if (selectedTab == 0) bill.dueDate else bill.paymentDate ?: bill.dueDate
            LocalDate.parse(dateStr, formatter)
        }.let { if (selectedTab == 1) it.reversed() else it }

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
                        text = { Text(title, fontWeight = FontWeight.Bold, color = if(selectedTab == index) Color(0xFF1B5E20) else Color.Gray) }
                    )
                }
            }
            SummaryCard(bills = bills, isHistoryTab = selectedTab == 1)
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                groupedBills.forEach { (monthYear, monthBills) ->
                    stickyHeader {
                        Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(8.dp)) {
                            Text(text = monthYear, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                        }
                    }
                    items(monthBills) { bill ->
                        BillCard(
                            bill = bill,
                            onDelete = { scope.launch { repository.delete(bill) } },
                            onEdit = { billToEdit = bill },
                            onPay = {
                                if (getDaysRemaining(bill.dueDate) < 0) billToPayAtLate = bill
                                else scope.launch { processPayment(bill, repository, bill.value) }
                            }
                        )
                    }
                }
            }
        }
    }
    if (showAddDialog) AddEditBillDialog(onDismiss = { showAddDialog = false }, onSave = { scope.launch { repository.insert(it) }; showAddDialog = false })
    if (billToEdit != null) AddEditBillDialog(bill = billToEdit, onDismiss = { billToEdit = null }, onSave = { scope.launch { repository.update(it) }; billToEdit = null })
    if (billToPayAtLate != null) LatePaymentDialog(bill = billToPayAtLate!!, onDismiss = { billToPayAtLate = null }, onConfirm = { finalVal -> scope.launch { processPayment(billToPayAtLate!!, repository, finalVal); billToPayAtLate = null } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(repository: BillRepository, onBack: () -> Unit) {
    val bills by repository.allBills.collectAsState(initial = emptyList())
    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scrollState = rememberScrollState()

    fun parseCurrency(value: String): Double {
        return try {
            val cleanString = value.replace(Regex("[^0-9]"), "")
            cleanString.toDouble() / 100.0
        } catch (e: Exception) { 0.0 }
    }

    val paidTotals = remember(bills, selectedMonth, selectedYear) {
        val totals = mutableMapOf<String, Double>()
        var totalEncargos = 0.0
        bills.filter { bill ->
            if (!bill.isPaid) return@filter false
            val dateStr = bill.paymentDate ?: bill.dueDate
            try {
                val date = LocalDate.parse(dateStr, formatter)
                date.monthValue == selectedMonth && date.year == selectedYear
            } catch (e: Exception) { false }
        }.forEach { bill ->
            val valorBase = parseCurrency(bill.value)
            val valorPagoFinal = if (bill.paidValue != null) parseCurrency(bill.paidValue!!) else valorBase
            totals[bill.category] = (totals[bill.category] ?: 0.0) + valorBase
            if (valorPagoFinal > valorBase) { totalEncargos += (valorPagoFinal - valorBase) }
        }
        if (totalEncargos > 0.01) { totals["Encargos"] = (totals["Encargos"] ?: 0.0) + totalEncargos }
        totals
    }

    val provisionTotals = remember(bills, selectedMonth, selectedYear) {
        bills.filter { bill ->
            if (bill.isPaid) return@filter false
            try {
                val date = LocalDate.parse(bill.dueDate, formatter)
                date.monthValue == selectedMonth && date.year == selectedYear
            } catch (e: Exception) { false }
        }.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { parseCurrency(it.value) } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(scrollState), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                    .clickable { showMonthYearPicker = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = {
                    if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- } else selectedMonth--
                }) { Icon(Icons.Default.KeyboardArrowLeft, null, tint = Color(0xFF1B5E20)) }

                val monthLabel = java.time.Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("$monthLabel / $selectedYear", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B5E20))
                    Text("Clique para alterar", fontSize = 10.sp, color = Color.Gray)
                }

                IconButton(onClick = {
                    if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else selectedMonth++
                }) { Icon(Icons.Default.KeyboardArrowRight, null, tint = Color(0xFF1B5E20)) }
            }

            Text("Efetivo Pago (Categorias + Encargos)", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 8.dp))
            if (paidTotals.isEmpty()) {
                Text("Nenhum pagamento registrado.", modifier = Modifier.padding(32.dp), fontSize = 12.sp, color = Color.Gray)
            } else {
                PieChart(data = paidTotals)
                DashboardLegend(paidTotals)
            }
            Divider(modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp))
            Text("Provisão de Gastos (Pendentes)", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFB71C1C))
            if (provisionTotals.isEmpty()) {
                Text("Nada pendente para este mês! 🎉", modifier = Modifier.padding(32.dp), fontSize = 12.sp, color = Color.Gray)
            } else {
                PieChart(data = provisionTotals)
                DashboardLegend(provisionTotals)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentMonth = selectedMonth,
            currentYear = selectedYear,
            onDismiss = { showMonthYearPicker = false },
            onConfirm = { m, y ->
                selectedMonth = m
                selectedYear = y
                showMonthYearPicker = false
            }
        )
    }
}

@Composable
fun MonthYearPickerDialog(currentMonth: Int, currentYear: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    var tempMonth by remember { mutableIntStateOf(currentMonth) }
    var tempYear by remember { mutableIntStateOf(currentYear) }
    val months = (1..12).map { java.time.Month.of(it).getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() } }
    val years = (2020..2030).toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar Período", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Mês", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    LazyColumn {
                        itemsIndexed(months) { index, item ->
                            Text(
                                text = item,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { tempMonth = index + 1 }
                                    .background(if (tempMonth == index + 1) Color(0xFFC8E6C9) else Color.Transparent)
                                    .padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ano", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    LazyColumn {
                        items(years) { year ->
                            Text(
                                text = year.toString(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { tempYear = year }
                                    .background(if (tempYear == year) Color(0xFFC8E6C9) else Color.Transparent)
                                    .padding(8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(tempMonth, tempYear) }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun BillCard(bill: Bill, onDelete: () -> Unit, onPay: () -> Unit, onEdit: () -> Unit) {
    // 1. Cálculo dos dias restantes
    val daysRemaining = getDaysRemaining(bill.dueDate)
    val isExpired = !bill.isPaid && daysRemaining < 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(
            containerColor = if (bill.isPaid) Color(0xFFF1F8E9)
            else if (isExpired) Color(0xFFFFEBEE)
            else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = bill.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    // --- NOVA TAG DE VENCIMENTO ---
                    if (!bill.isPaid) {
                        val textTag = when {
                            daysRemaining < 0 -> "Atrasado há ${-daysRemaining} dias"
                            daysRemaining == 0L -> "Vence hoje"
                            daysRemaining == 1L -> "Vence amanhã"
                            else -> "Vence em $daysRemaining dias"
                        }
                        Text(
                            text = textTag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isExpired) Color.Red else Color(0xFF1B5E20),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    // ------------------------------

                    Text(text = bill.category, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bill.totalInstallments > 0) {
                        Surface(color = Color(0xFFE57373), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "${bill.currentInstallment}/${bill.totalInstallments}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (bill.isAutomatic) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(color = Color(0xFFFFECB3), shape = RoundedCornerShape(4.dp)) {
                            Text(
                                text = "AUTO",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    if (bill.isPaid) {
                        Text(text = "Venc: ${bill.dueDate}", fontSize = 10.sp, color = Color.Gray)
                        Text(
                            text = "Pago em: ${bill.paymentDate ?: "--/--/----"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        Text(
                            text = "Venc: ${bill.dueDate}",
                            fontSize = 12.sp,
                            color = if (isExpired) Color.Red else Color.DarkGray
                        )
                    }

                    if (bill.isPaid && bill.paidValue != null) {
                        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                        val originalNum = bill.value.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0
                        val paidNum = bill.paidValue.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0
                        val diff = (paidNum - originalNum) / 100

                        Text("Original: ${bill.value}", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            "Valor: ${bill.paidValue}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (diff > 0) Color.Red else Color(0xFF2E7D32)
                        )
                        if (diff > 0) {
                            Text(
                                "Encargos: ${currencyFormatter.format(diff)}",
                                fontSize = 11.sp,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        val isLate = try {
                            val due = LocalDate.parse(bill.dueDate, formatter)
                            val paid = LocalDate.parse(bill.paymentDate, formatter)
                            paid.isAfter(due)
                        } catch (e: Exception) { false }

                        if (isLate) {
                            Text("PAGO COM ATRASO", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Black)
                        }
                    } else {
                        Text(
                            text = bill.value,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = if (isExpired) Color.Red else Color(0xFF1B5E20)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = Color.LightGray)
                    }
                    if (!bill.isPaid && !bill.isAutomatic) {
                        Button(
                            onClick = onPay,
                            colors = ButtonDefaults.buttonColors(containerColor = if (isExpired) Color.Red else Color(0xFF1B5E20))
                        ) {
                            Text("PAGUEI", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun SummaryCard(bills: List<Bill>, isHistoryTab: Boolean) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val today = LocalDate.now()
    val monthName = today.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    var totalValue = 0.0
    var totalEncargos = 0.0
    bills.forEach { bill ->
        try {
            val dueDate = LocalDate.parse(bill.dueDate, formatter)
            if (isHistoryTab) {
                val paymentDate = if (bill.paymentDate != null) LocalDate.parse(bill.paymentDate, formatter) else null
                if (paymentDate?.monthValue == today.monthValue && paymentDate?.year == today.year) {
                    val original = bill.value.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0
                    val paid = bill.paidValue?.replace(Regex("[^0-9]"), "")?.toDoubleOrNull() ?: original
                    totalValue += paid / 100
                    if (paid > original) totalEncargos += (paid - original) / 100
                }
            } else {
                if (!bill.isPaid) {
                    val isDueThisMonth = dueDate.monthValue == today.monthValue && dueDate.year == today.year
                    val isPastDue = dueDate.isBefore(today)
                    if (isDueThisMonth || isPastDue) {
                        val original = bill.value.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0
                        totalValue += original / 100
                    }
                }
            }
        } catch (e: Exception) {}
    }
    Card(modifier = Modifier.fillMaxWidth().padding(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                val label = if (isHistoryTab) "Total pago em $monthName" else "Pendências e gastos de $monthName"
                Text(text = label, fontSize = 12.sp)
                Text(text = currencyFormatter.format(totalValue), fontSize = 22.sp, fontWeight = FontWeight.Black, color = if (isHistoryTab) Color(0xFF2E7D32) else Color(0xFFB71C1C))
            }
            if (isHistoryTab && totalEncargos > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Encargos no Mês", fontSize = 10.sp, color = Color.Red)
                    Text(currencyFormatter.format(totalEncargos), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(bill: Bill? = null, onDismiss: () -> Unit, onSave: (Bill) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var dueDate by remember { mutableStateOf(bill?.dueDate ?: "") }
    var rawValue by remember { mutableStateOf(bill?.value?.replace(Regex("[^0-9]"), "") ?: "") }
    var category by remember { mutableStateOf(bill?.category ?: "Outros") }
    var periodicity by remember { mutableStateOf(bill?.periodicity ?: "Mês") }
    var isAutomatic by remember { mutableStateOf(bill?.isAutomatic ?: false) }
    var customInterval by remember { mutableStateOf(bill?.customInterval?.toString() ?: "1") }
    var totalInstallments by remember { mutableStateOf(if(bill?.totalInstallments == 0) "" else bill?.totalInstallments?.toString() ?: "") }
    var expCat by remember { mutableStateOf(false) }
    var expPer by remember { mutableStateOf(false) }
    val cats = listOf("Moradia", "Alimentação", "Transporte", "Saúde", "Lazer", "Educação", "Outros")
    val formattedValue = remember(rawValue) { val parsed = rawValue.toDoubleOrNull() ?: 0.0; NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100) }
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> dueDate = String.format("%02d/%02d/%d", d, m + 1, y) }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if(bill == null) "Nova Conta" else "Editar Conta") }, text = {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded = expCat, onExpandedChange = { expCat = !expCat }) {
                    OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("Categoria") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expCat) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expCat, onDismissRequest = { expCat = false }) { cats.forEach { cat -> DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expCat = false }) } }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = dueDate, onValueChange = {}, label = { Text("Vencimento") }, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = { datePickerDialog.show() }) { Icon(Icons.Default.DateRange, null) } })
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Pago Automático?", modifier = Modifier.weight(1f)); Switch(checked = isAutomatic, onCheckedChange = { isAutomatic = it }) }
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(value = customInterval, onValueChange = { customInterval = it }, label = { Text("Cada") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Spacer(modifier = Modifier.width(8.dp))
                    ExposedDropdownMenuBox(expanded = expPer, onExpandedChange = { expPer = !expPer }, modifier = Modifier.weight(1.5f)) {
                        OutlinedTextField(value = periodicity, onValueChange = {}, readOnly = true, label = { Text("Período") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expPer) }, modifier = Modifier.menuAnchor())
                        ExposedDropdownMenu(expanded = expPer, onDismissRequest = { expPer = false }) { listOf("Dia", "Semana", "Mês", "Ano").forEach { DropdownMenuItem(text = { Text(it) }, onClick = { periodicity = it; expPer = false }) } }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = totalInstallments, onValueChange = { totalInstallments = it }, label = { Text("Parcelas (0 = s/ limite)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = formattedValue, onValueChange = { rawValue = it.filter { c -> c.isDigit() } }, label = { Text("Valor") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
    }, confirmButton = { Button(onClick = {
        val finalBill = bill?.copy(name=name, value=formattedValue, dueDate=dueDate, category=category, periodicity=periodicity, customInterval=customInterval.toIntOrNull()?:1, totalInstallments=totalInstallments.toIntOrNull()?:0, isAutomatic=isAutomatic) ?: Bill(name=name, value=formattedValue, dueDate=dueDate, category=category, periodicity=periodicity, customInterval=customInterval.toIntOrNull()?:1, totalInstallments=totalInstallments.toIntOrNull()?:0, currentInstallment=1, isPaid=false, isAutomatic=isAutomatic)
        onSave(finalBill)
    }, enabled = name.isNotBlank() && dueDate.isNotBlank()) { Text("Salvar") } })
}

@Composable
fun DashboardLegend(data: Map<String, Double>) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val totalGeral = data.values.sum()
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        data.toList().sortedByDescending { it.second }.forEach { (category, total) ->
            val percentagem = if (totalGeral > 0) (total / totalGeral) * 100 else 0.0
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Box(modifier = Modifier.size(12.dp).background(getCategoryColor(category), CircleShape))
                Text(text = String.format("%.1f%% %s", percentagem, category), modifier = Modifier.padding(start = 8.dp).weight(1f), fontSize = 14.sp)
                Text(text = currencyFormatter.format(total), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
        Divider(modifier = Modifier.padding(vertical = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("TOTAL NO PERÍODO", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(text = currencyFormatter.format(totalGeral), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1B5E20))
        }
    }
}

@Composable
fun PieChart(data: Map<String, Double>) {
    val totalSum = data.values.sum()
    if (totalSum <= 0) return
    var startAngle = -90f
    Box(modifier = Modifier.size(240.dp).padding(16.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            data.forEach { (category, value) ->
                val sweepAngle = (value / totalSum * 360).toFloat()
                drawArc(color = getCategoryColor(category), startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = 35.dp.toPx()))
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", fontSize = 12.sp, color = Color.Gray)
            Text(NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(totalSum), fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when(category) {
        "Moradia" -> Color(0xFF1976D2)
        "Alimentação" -> Color(0xFF388E3C)
        "Transporte" -> Color(0xFFFBC02D)
        "Saúde" -> Color(0xFF2FD3B2)
        "Lazer" -> Color(0xFF7B1FA2)
        "Educação" -> Color(0xFF00796B)
        "Encargos" -> Color(0xFFFF0000)
        else -> Color(0xFF9E9E9E)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: BillRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("configs", Context.MODE_PRIVATE)
    val bills by repository.allBills.collectAsState(initial = emptyList())
    var notifyTime by remember { mutableStateOf(prefs.getString("notify_time", "08:00") ?: "08:00") }
    var insistence by remember { mutableStateOf(prefs.getString("insistence", "Padrão") ?: "Padrão") }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { importFromCSV(context, repository, scope, it) } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { saveCsvToUri(context, it, bills) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                // BARRA VERDE PADRONIZADA
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Horário de Notificação", fontWeight = FontWeight.Bold)
            Button(onClick = {
                val time = notifyTime.split(":")
                TimePickerDialog(context, { _, h, m ->
                    val newTime = String.format("%02d:%02d", h, m)
                    notifyTime = newTime
                    prefs.edit().putString("notify_time", newTime).apply()
                }, time[0].toInt(), time[1].toInt(), true).show()
            }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("Avisar às $notifyTime") }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Nível de Alerta", fontWeight = FontWeight.Bold)
            listOf("Padrão", "Alto", "Crítico").forEach { option ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { insistence = option; prefs.edit().putString("insistence", option).apply() }.padding(vertical = 8.dp)) {
                    RadioButton(selected = insistence == option, onClick = { insistence = option; prefs.edit().putString("insistence", option).apply() })
                    Text(option, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Dados e Segurança", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { val fileName = "vencehoje_backup_${System.currentTimeMillis()}.csv"; exportLauncher.launch(fileName) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), enabled = bills.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))) { Icon(Icons.Default.Share, null); Spacer(modifier = Modifier.width(8.dp)); Text("Exportar Dados (CSV)") }
            OutlinedButton(onClick = { importLauncher.launch("text/*") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Icon(Icons.Default.Menu, null); Spacer(modifier = Modifier.width(8.dp)); Text("Importar Dados (CSV)") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatePaymentDialog(bill: Bill, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var rawValue by remember { mutableStateOf("") }
    val formatted = remember(rawValue) { val parsed = rawValue.toDoubleOrNull() ?: 0.0; NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Pago com atraso") }, text = { Column { Text("Valor original: ${bill.value}"); OutlinedTextField(value = formatted, onValueChange = { rawValue = it.filter { c -> c.isDigit() } }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), label = { Text("Valor Pago") }) } }, confirmButton = { Button(onClick = { onConfirm(formatted) }) { Text("Confirmar") } })
}

fun getDaysRemaining(dueDate: String): Long { val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy"); return try { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate, formatter)) } catch (e: Exception) { 0 } }

suspend fun processPayment(bill: Bill, repository: BillRepository, finalValue: String) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val today = LocalDate.now().format(formatter)
    repository.insert(bill.copy(id = 0, isPaid = true, paymentDate = today, paidValue = finalValue))
    if (bill.totalInstallments > 0 && bill.currentInstallment >= bill.totalInstallments) { repository.delete(bill) }
    else {
        val date = LocalDate.parse(bill.dueDate, formatter)
        val nextDate = when (bill.periodicity) { "Dia" -> date.plusDays(bill.customInterval.toLong()); "Semana" -> date.plusWeeks(bill.customInterval.toLong()); "Mês" -> date.plusMonths(bill.customInterval.toLong()); "Ano" -> date.plusYears(bill.customInterval.toLong()); else -> date.plusMonths(1) }
        repository.update(bill.copy(dueDate = nextDate.format(formatter), currentInstallment = bill.currentInstallment + 1, isAutomatic = bill.isAutomatic))
    }
}

@Composable
fun VenceHojeTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1B5E20), primaryContainer = Color(0xFFC8E6C9), secondaryContainer = Color(0xFFE8F5E9), surfaceVariant = Color(0xFFF5F5F5)), content = content) }

fun importFromCSV(context: Context, repository: BillRepository, scope: kotlinx.coroutines.CoroutineScope, uri: android.net.Uri) {
    scope.launch {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = inputStream?.bufferedReader()
            val lines = reader?.readLines() ?: emptyList()
            if (lines.size > 1) {
                repository.deleteAll()
                lines.drop(1).forEach { line ->
                    val parts = line.split(";")
                    if (parts.size >= 10) {
                        val bill = Bill(
                            name = parts[0],
                            value = parts[1],
                            dueDate = parts[2],
                            category = parts[3],
                            isPaid = parts[4] == "Pago",
                            paidValue = parts[5].ifBlank { null },
                            paymentDate = parts[6].ifBlank { null },
                            totalInstallments = parts[7].toIntOrNull() ?: 0,
                            currentInstallment = parts[8].toIntOrNull() ?: 1,
                            isAutomatic = parts[9] == "Sim"
                        )
                        repository.insert(bill)
                    }
                }
                Toast.makeText(context, "Restore concluído com sucesso!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) { Toast.makeText(context, "Erro no restore: ${e.message}", Toast.LENGTH_LONG).show() }
    }
}

fun saveCsvToUri(context: Context, uri: android.net.Uri, bills: List<Bill>) {
    val content = StringBuilder("Nome;Valor;Vencimento;Categoria;Status;Valor Pago;Data Pagamento;Total Parcelas;Parcela Atual;Automatico\n")
    bills.forEach { content.append("${it.name};${it.value};${it.dueDate};${it.category};${if(it.isPaid) "Pago" else "Pendente"};${it.paidValue ?: ""};${it.paymentDate ?: ""};${it.totalInstallments};${it.currentInstallment};${if(it.isAutomatic) "Sim" else "Não"}\n") }
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream -> outputStream.write(content.toString().toByteArray()) }
        Toast.makeText(context, "Backup gerado com sucesso!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) { Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show() }
}