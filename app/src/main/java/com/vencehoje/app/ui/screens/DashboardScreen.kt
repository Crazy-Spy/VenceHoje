package com.vencehoje.app.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.BillRepository
import com.vencehoje.app.ui.components.DashboardLegend
import com.vencehoje.app.ui.components.PieChart
import com.vencehoje.app.ui.components.MonthYearPickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: BillRepository,
    profileId: Int,
    onBack: () -> Unit,
    onProfileChange: (Int) -> Unit // <--- NOVO PARÂMETRO
) {
    val bills by remember(profileId) { repository.getBillsByProfile(profileId) }
        .collectAsState(initial = emptyList())

    val categories by remember(profileId) { repository.getCategoriesByProfile(profileId) }
        .collectAsState(initial = emptyList())

    val allProfiles by repository.allProfiles.collectAsState(initial = emptyList())
    val currentProfile = allProfiles.find { it.id == profileId }

    var selectedMonth by remember { mutableIntStateOf(LocalDate.now().monthValue) }
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var filterMode by remember { mutableStateOf("Pagos") }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    // Estado do Menu Dropdown
    var isProfileMenuExpanded by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val scrollState = rememberScrollState()

    val categoryColors = remember(categories) {
        categories.associate {
            it.name to try {
                Color(android.graphics.Color.parseColor(it.colorHex))
            } catch (e: Exception) { Color.Gray }
        }.toMutableMap().apply {
            this["Encargos"] = Color.Red
        }
    }

    fun parseCurrency(value: String): Double {
        return try {
            val cleanString = value.replace(Regex("[^0-9]"), "")
            cleanString.toDouble() / 100.0
        } catch (e: Exception) { 0.0 }
    }

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
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Dashboard", fontWeight = FontWeight.Black)

                        Spacer(modifier = Modifier.width(12.dp))

                        // Chip Visual INTERATIVO
                        Surface(
                            modifier = Modifier.clickable { isProfileMenuExpanded = true }, // Agora clica!
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
                                // Setinha para indicar que clica
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
            // ... (Resto do conteúdo mantido igual)
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

            if (displayData.isEmpty()) {
                Text(
                    text = "Nada para exibir neste filtro. 🎉",
                    modifier = Modifier.padding(32.dp),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            } else {
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