package com.vencehoje.app.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.BillRepository
import com.vencehoje.app.logic.getDaysRemaining
import com.vencehoje.app.ui.components.getIconPainterFromName
import com.vencehoje.app.ui.screens.CategoryDisplay
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(
    bill: Bill? = null,
    repository: BillRepository,
    onDismiss: () -> Unit,
    onSave: (Bill) -> Unit
) {
    val categories by repository.allCategories.collectAsState(initial = emptyList())
    var showInfoDialog by remember { mutableStateOf(false) }

    // Estados do Formulário
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var selectedCategoryId by remember { mutableIntStateOf(bill?.categoryId ?: 7) }
    var dueDate by remember { mutableStateOf(bill?.dueDate ?: "") }
    var periodicity by remember { mutableStateOf(bill?.periodicity ?: "Mês") }
    var isAutomatic by remember { mutableStateOf(bill?.isAutomatic ?: false) }
    var customInterval by remember { mutableStateOf(bill?.customInterval?.toString() ?: "1") }
    var totalInstallments by remember { mutableStateOf(bill?.totalInstallments?.toString() ?: "0") }
    var rawValue by remember { mutableStateOf(bill?.value?.replace(Regex("[^0-9]"), "") ?: "") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    // Formatação de Moeda
    val formattedValue = remember(rawValue) {
        val parsed = rawValue.toDoubleOrNull() ?: 0.0
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100)
    }

    // --- LÓGICA DE RECORRÊNCIA INTELIGENTE ---
    val intervalInt = customInterval.toIntOrNull() ?: 1
    val totalInstallmentsInt = totalInstallments.toIntOrNull() ?: 0

    val periodicityLabel = when (periodicity) {
        "Dia" -> if (intervalInt == 1) "dia" else "dias"
        "Semana" -> if (intervalInt == 1) "semana" else "semanas"
        "Mês" -> if (intervalInt == 1) "mês" else "meses"
        "Ano" -> if (intervalInt == 1) "ano" else "anos"
        else -> "mês"
    }

    val finalDateText = remember(dueDate, intervalInt, periodicity, totalInstallmentsInt) {
        if (totalInstallmentsInt <= 1 || dueDate.isBlank()) ""
        else {
            try {
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val startDate = LocalDate.parse(dueDate, formatter)
                val finalDate = when (periodicity) {
                    "Dia" -> startDate.plusDays((intervalInt * (totalInstallmentsInt - 1)).toLong())
                    "Semana" -> startDate.plusWeeks((intervalInt * (totalInstallmentsInt - 1)).toLong())
                    "Mês" -> startDate.plusMonths((intervalInt * (totalInstallmentsInt - 1)).toLong())
                    "Ano" -> startDate.plusYears((intervalInt * (totalInstallmentsInt - 1)).toLong())
                    else -> startDate
                }
                " | Última em ${finalDate.format(formatter)}"
            } catch (e: Exception) { "" }
        }
    }

    val repetitionText = remember(intervalInt, periodicityLabel, totalInstallmentsInt, finalDateText) {
        val prefix = if (intervalInt == 1) "Repete a cada $periodicityLabel" else "Repete a cada $intervalInt $periodicityLabel"
        when {
            totalInstallmentsInt == 0 -> "$prefix indefinidamente."
            totalInstallmentsInt == 1 -> "Parcela única (não repete)."
            else -> "$prefix por $totalInstallmentsInt vezes$finalDateText."
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            dueDate = String.format("%02d/%02d/%d", dayOfMonth, month + 1, year)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dueDate.isNotBlank()) {
                        onSave(Bill(
                            id = bill?.id ?: 0,
                            name = name,
                            value = formattedValue,
                            dueDate = dueDate,
                            categoryId = selectedCategoryId,
                            isPaid = bill?.isPaid ?: false,
                            periodicity = periodicity,
                            isAutomatic = isAutomatic,
                            customInterval = intervalInt,
                            totalInstallments = totalInstallmentsInt,
                            currentInstallment = bill?.currentInstallment ?: 1
                        ))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
        title = { Text(if (bill == null) "Nova Conta" else "Editar Conta", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome da conta") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = {},
                        label = { Text("Data de Vencimento") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.DateRange, null, tint = Color(0xFF1B5E20))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    var expandedCat by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCat,
                        onExpandedChange = { expandedCat = !expandedCat },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.name ?: "Outros",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoria") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            leadingIcon = {
                                selectedCategory?.let { cat ->
                                    val catColor = try {
                                        Color(android.graphics.Color.parseColor(cat.colorHex))
                                    } catch (e: Exception) {
                                        Color.Gray
                                    }

                                    // USA O COMPONENTE NOVO AQUI:
                                    CategoryDisplay(
                                        iconName = cat.iconName,
                                        color = catColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        )
                        ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val catColor = try {
                                                Color(android.graphics.Color.parseColor(cat.colorHex))
                                            } catch (e: Exception) {
                                                Color.Gray
                                            }

                                            // AQUI ESTÁ A MÁGICA: TROCAMOS O ICON PELO CATEGORYDISPLAY
                                            CategoryDisplay(
                                                iconName = cat.iconName,
                                                color = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Text(
                                                text = cat.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        expandedCat = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Pagamento Automático?", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Marcar como pago no dia", fontSize = 11.sp, color = Color.Gray)
                            }
                            Switch(checked = isAutomatic, onCheckedChange = { isAutomatic = it })
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Recorrência e Parcelas", color = Color(0xFF1B5E20), fontWeight = FontWeight.Black, fontSize = 13.sp)
                        IconButton(onClick = { showInfoDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customInterval,
                            onValueChange = { customInterval = it.filter { c -> c.isDigit() } },
                            label = { Text("Cada") },
                            modifier = Modifier.weight(0.6f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        var expandedPeriod by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedPeriod,
                            onExpandedChange = { expandedPeriod = !expandedPeriod },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = periodicity,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Período") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriod) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expandedPeriod, onDismissRequest = { expandedPeriod = false }) {
                                listOf("Dia", "Semana", "Mês", "Ano").forEach {
                                    DropdownMenuItem(text = { Text(it) }, onClick = { periodicity = it; expandedPeriod = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = totalInstallments,
                            onValueChange = { totalInstallments = it.filter { c -> c.isDigit() } },
                            label = { Text("Parc.") },
                            modifier = Modifier.weight(0.6f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Text(
                        text = repetitionText,
                        color = Color(0xFF2E7D32),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = formattedValue,
                        onValueChange = { rawValue = it.replace(Regex("[^0-9]"), "") },
                        label = { Text("Valor") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            textAlign = TextAlign.Center,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF1B5E20)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text(
                        text = "0,00 = Valor Variável",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    )

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Entendi") } },
            title = { Text("Dica de Recorrência", fontWeight = FontWeight.Bold) },
            text = { Text("Use 0 parcelas para contas que não terminam (ex: Aluguel). O app gerará as contas automaticamente por tempo indeterminado.") }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatePaymentDialog(bill: Bill, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var rawValue by remember { mutableStateOf("") }
    val formatted = remember(rawValue) {
        val parsed = rawValue.toDoubleOrNull() ?: 0.0
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100)
    }

    val isLate = getDaysRemaining(bill.dueDate) < 0
    val titleText = if (isLate) "Pago com atraso" else "Confirmar Valor"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Original: ${if(bill.value.contains("0,00")) "Variável" else bill.value}")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = formatted,
                    onValueChange = { rawValue = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Valor Pago Real") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(formatted) }, enabled = rawValue.isNotEmpty()) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
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