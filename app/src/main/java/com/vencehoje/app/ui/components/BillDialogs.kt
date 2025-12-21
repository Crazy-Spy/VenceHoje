package com.vencehoje.app.ui.dialogs

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import com.vencehoje.app.logic.getDaysRemaining
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.*

// Mova para cá: AddEditBillDialog, LatePaymentDialog e MonthYearPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(bill: Bill? = null, onDismiss: () -> Unit, onSave: (Bill) -> Unit) {
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var category by remember { mutableStateOf(bill?.category ?: "Outros") }
    var dueDate by remember { mutableStateOf(bill?.dueDate ?: "") }
    var periodicity by remember { mutableStateOf(bill?.periodicity ?: "Mês") }
    var isAutomatic by remember { mutableStateOf(bill?.isAutomatic ?: false) }
    var customInterval by remember { mutableStateOf(bill?.customInterval?.toString() ?: "1") }
    var totalInstallments by remember { mutableStateOf(bill?.totalInstallments?.toString() ?: "0") }
    var rawValue by remember { mutableStateOf(bill?.value?.replace(Regex("[^0-9]"), "") ?: "") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Formatação de Moeda em tempo real
    val formattedValue = remember(rawValue) {
        val parsed = rawValue.toDoubleOrNull() ?: 0.0
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100)
    }

    // Lógica da Frase de Periodicidade
    val periodicidadeTexto = remember(customInterval, periodicity, dueDate) {
        if (dueDate.isBlank()) "Informe o vencimento para ver a recorrência."
        else {
            val dia = dueDate.split("/").firstOrNull() ?: ""
            val intervalo = customInterval.toIntOrNull() ?: 1
            val plural = if (intervalo > 1) "s" else ""

            when (periodicity) {
                "Dia" -> "Repete a cada $intervalo dia$plural."
                "Semana" -> "Repete a cada $intervalo semana$plural."
                "Mês" -> if (intervalo == 1) "Repetida mensalmente todo dia $dia."
                else "Repetida a cada $intervalo meses todo dia $dia."
                "Ano" -> "Repetida anualmente todo dia $dia."
                else -> ""
            }
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
        title = { Text(if (bill == null) "Nova Conta" else "Editar Conta", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    // NOME DA CONTA
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome da conta") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // CATEGORIA E VENCIMENTO (LADO A LADO)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        var expandedCat by remember { mutableStateOf(false) }
                        val categories = listOf("Moradia", "Transporte", "Saúde", "Lazer", "Alimentação", "Educação", "Outros")

                        ExposedDropdownMenuBox(
                            expanded = expandedCat,
                            onExpandedChange = { expandedCat = !expandedCat },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoria") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expandedCat = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = {},
                            label = { Text("Vencimento") },
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.DateRange, null, tint = Color(0xFF1B5E20))
                                }
                            }
                        )
                    }

                    // PAGO AUTOMÁTICO (SWITCH)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pagamento Automático?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Marcar como pago no dia", fontSize = 11.sp, color = Color.Gray)
                        }
                        Switch(checked = isAutomatic, onCheckedChange = { isAutomatic = it })
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)

                    // PERIODICIDADE E PARCELAS (TRIO)
                    Text("Recorrência e Parcelas", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customInterval,
                            onValueChange = { customInterval = it.filter { c -> c.isDigit() } },
                            label = { Text("Cada") },
                            modifier = Modifier.weight(0.6f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        var expandedPer by remember { mutableStateOf(false) }
                        val periods = listOf("Dia", "Semana", "Mês", "Ano")
                        ExposedDropdownMenuBox(
                            expanded = expandedPer,
                            onExpandedChange = { expandedPer = !expandedPer },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = periodicity,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Período") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPer) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = expandedPer, onDismissRequest = { expandedPer = false }) {
                                periods.forEach { p ->
                                    DropdownMenuItem(text = { Text(p) }, onClick = { periodicity = p; expandedPer = false })
                                }
                            }
                        }

                        OutlinedTextField(
                            value = totalInstallments,
                            onValueChange = { totalInstallments = it.filter { c -> c.isDigit() } },
                            label = { Text("Parc.") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(0.7f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // FRASE DINÂMICA
                    Text(
                        text = periodicidadeTexto,
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // VALOR (CENTRALIZADO E DESTAQUE)
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        OutlinedTextField(
                            value = formattedValue,
                            onValueChange = { rawValue = it.filter { c -> c.isDigit() } },
                            label = { Text("Valor") },
                            modifier = Modifier.fillMaxWidth(0.8f),
                            textStyle = LocalTextStyle.current.copy(
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color(0xFF1B5E20)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Text("0,00 = Valor Variável", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && dueDate.isNotBlank()) {
                        onSave(Bill(
                            id = bill?.id ?: 0,
                            name = name,
                            category = category,
                            value = formattedValue,
                            dueDate = dueDate,
                            isPaid = bill?.isPaid ?: false,
                            periodicity = periodicity,
                            isAutomatic = isAutomatic,
                            customInterval = customInterval.toIntOrNull() ?: 1,
                            totalInstallments = totalInstallments.toIntOrNull() ?: 0,
                            currentInstallment = bill?.currentInstallment ?: 1
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatePaymentDialog(bill: Bill, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var rawValue by remember { mutableStateOf("") }
    val formatted = remember(rawValue) {
        val parsed = rawValue.toDoubleOrNull() ?: 0.0
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(parsed / 100)
    }

    // Título dinâmico
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


