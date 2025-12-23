package com.vencehoje.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.BillRepository
import com.vencehoje.app.ui.components.DashboardLegend
import com.vencehoje.app.ui.components.PieChart
import com.vencehoje.app.ui.dialogs.MonthYearPickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(repository: BillRepository, onBack: () -> Unit) {
    // Coleta as contas e as categorias do banco
    val bills by repository.allBills.collectAsState(initial = emptyList())
    val categories by repository.allCategories.collectAsState(initial = emptyList())

    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var filterMode by remember { mutableStateOf("Pagos") }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scrollState = rememberScrollState()

    // 1. Criamos o mapa de Cores para o Gráfico (Nome -> Color)
    val categoryColors = remember(categories) {
        categories.associate {
            it.name to Color(android.graphics.Color.parseColor(it.colorHex))
        }.toMutableMap().apply {
            this["Encargos"] = Color.Red // Cor fixa para juros
        }
    }

    fun parseCurrency(value: String): Double {
        return try {
            val cleanString = value.replace(Regex("[^0-9]"), "")
            cleanString.toDouble() / 100.0
        } catch (e: Exception) { 0.0 }
    }

    // 2. Processamento dos dados para exibição
    val displayData = remember(bills, categories, selectedMonth, selectedYear, filterMode) {
        val totals = mutableMapOf<String, Double>()
        var totalEncargos = 0.0

        bills.filter { bill ->
            val dateStr = if (bill.isPaid) (bill.paymentDate ?: bill.dueDate) else bill.dueDate
            val date = try { LocalDate.parse(dateStr, formatter) } catch (e: Exception) { null }
            val matchesDate = date?.monthValue == selectedMonth && date?.year == selectedYear
            val matchesFilter = if (filterMode == "Pagos") bill.isPaid else !bill.isPaid
            matchesDate && matchesFilter
        }.forEach { bill ->
            // Buscamos o nome da categoria pelo ID
            val categoryName = categories.find { it.id == bill.categoryId }?.name ?: "Outros"
            val valorBase = parseCurrency(bill.value)
            val isVariable = valorBase < 0.01

            if (bill.isPaid) {
                val valorPagoFinal = if (bill.paidValue != null) parseCurrency(bill.paidValue!!) else valorBase
                if (isVariable) {
                    totals[categoryName] = (totals[categoryName] ?: 0.0) + valorPagoFinal
                } else {
                    totals[categoryName] = (totals[categoryName] ?: 0.0) + valorBase
                    if (valorPagoFinal > valorBase) {
                        totalEncargos += (valorPagoFinal - valorBase)
                    }
                }
            } else {
                totals[categoryName] = (totals[categoryName] ?: 0.0) + valorBase
            }
        }

        if (totalEncargos > 0.01) {
            totals["Encargos"] = (totals["Encargos"] ?: 0.0) + totalEncargos
        }
        totals
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Black) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1B5E20),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- SELETOR DE MÊS/ANO ---
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

                val monthLabel = java.time.Month.of(selectedMonth)
                    .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                    .replaceFirstChar { it.uppercase() }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("$monthLabel / $selectedYear", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B5E20))
                    Text("Clique para alterar", fontSize = 10.sp, color = Color.Gray)
                }

                IconButton(onClick = {
                    if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ } else selectedMonth++
                }) { Icon(Icons.Default.KeyboardArrowRight, null, tint = Color(0xFF1B5E20)) }
            }

            // --- FILTROS RÁPIDOS (CHIPS) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Pagos", "Pendentes").forEach { label ->
                    FilterChip(
                        selected = filterMode == label,
                        onClick = { filterMode = label },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFC8E6C9),
                            selectedLabelColor = Color(0xFF1B5E20)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val titleColor = if (filterMode == "Pendentes") Color(0xFFB71C1C) else Color(0xFF2E7D32)
            Text(
                text = "Resumo: $filterMode",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = titleColor
            )

            // --- GRÁFICO E LEGENDA ---
            if (displayData.isEmpty()) {
                Text(
                    text = "Nada para exibir neste filtro. 🎉",
                    modifier = Modifier.padding(32.dp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else {
                // CORREÇÃO: Passando o mapa de cores para os componentes
                PieChart(data = displayData, categoryColors = categoryColors)
                DashboardLegend(data = displayData, categoryColors = categoryColors)
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