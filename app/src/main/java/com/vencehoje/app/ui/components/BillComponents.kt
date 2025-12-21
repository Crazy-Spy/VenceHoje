package com.vencehoje.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.Bill
import com.vencehoje.app.logic.getDaysRemaining
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import kotlin.compareTo
import kotlin.unaryMinus

@Composable
fun BillCard(bill: Bill, onDelete: () -> Unit, onPay: () -> Unit, onEdit: () -> Unit) {
    val daysRemaining = getDaysRemaining(bill.dueDate)
    val isExpired = !bill.isPaid && daysRemaining < 0
    // Extrai apenas os números para checar se é zero real
    val numericValue = bill.value.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L

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
                    Text(text = bill.category, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                // ... (Manter lógica de parcelas e AUTO que você já tem)
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
                        // Exibição do valor pago (com juros se houver)
                        if (bill.paidValue != null) {
                            Text("Valor: ${bill.paidValue}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Venc: ${bill.dueDate}",
                            fontSize = 12.sp,
                            color = if (isExpired) Color.Red else Color.DarkGray
                        )

                        // --- LÓGICA DE EXIBIÇÃO DO VALOR ---
                        val isZero = numericValue == 0L
                        val displayValue = if (isZero) "Valor Variável" else bill.value

                        Text(
                            text = displayValue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = if (isZero) 14.sp else 18.sp,
                            color = if (isZero) Color(0xFF1976D2) else if (isExpired) Color.Red else Color(0xFF1B5E20)
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
