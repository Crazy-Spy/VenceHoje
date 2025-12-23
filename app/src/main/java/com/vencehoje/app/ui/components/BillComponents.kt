package com.vencehoje.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.Category
import com.vencehoje.app.logic.getDaysRemaining
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import com.vencehoje.app.R // O seu pacote real
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@Composable
fun BillCard(
    bill: Bill,
    category: Category?, // Agora usamos este objeto
    onDelete: () -> Unit,
    onPay: () -> Unit,
    onEdit: () -> Unit
) {
    val daysRemaining = getDaysRemaining(bill.dueDate)
    val isExpired = !bill.isPaid && daysRemaining < 0
    val numericValue = bill.value.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L

    // Resolvemos a cor e o ícone aqui fora para limpar o código abaixo
    val catColor = if (category != null) {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } else {
        Color(0xFF9E9E9E)
    }

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
            // LINHA 1: ÍCONE + NOME + TAGS
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ÍCONE DINÂMICO
                Icon(
                    painter = getIconPainterFromName(category?.iconName ?: "label"),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = catColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = bill.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (bill.isAutomatic) {
                        StatusTag(text = "AUTO", containerColor = Color(0xFFFFEB3B), contentColor = Color.Black)
                    }
                    if (bill.totalInstallments > 0) {
                        StatusTag(text = "${bill.currentInstallment}/${bill.totalInstallments}", containerColor = Color.Red, contentColor = Color.White)
                    }
                }
            }

            // LINHA 2: VENCIMENTO / STATUS E VALOR
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val statusText = when {
                        bill.isPaid -> "Pago em ${bill.paymentDate}"
                        daysRemaining < 0 -> "Atrasado há ${-daysRemaining} dias"
                        daysRemaining == 0L -> "Vence hoje"
                        else -> "Venc: ${bill.dueDate}"
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isExpired) Color.Red else if (bill.isPaid) Color(0xFF2E7D32) else Color.DarkGray
                    )
                    // CORREÇÃO: Usando o nome da categoria que veio do objeto
                    Text(
                        text = category?.name ?: "Outros",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }

                val isZero = numericValue == 0L
                val displayValue = if (bill.isPaid && bill.paidValue != null) bill.paidValue!! else if (isZero) "Variável" else bill.value

                Text(
                    text = displayValue,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = if (isZero) Color(0xFF1976D2) else if (isExpired) Color.Red else Color(0xFF1B5E20)
                )
            }

            // LINHA 3: AÇÕES
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.LightGray.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                }

                if (!bill.isPaid && !bill.isAutomatic) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onPay,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isExpired) Color.Red else Color(0xFF1B5E20))
                    ) {
                        Text("PAGUEI", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

// Mapeamento de String para Icon
@Composable
fun getIconPainterFromName(iconName: String): Painter {
    return when (iconName) {
        "home" -> rememberVectorPainter(Icons.Default.Home)
        "shopping_cart" -> rememberVectorPainter(Icons.Default.ShoppingCart)
        "directions_car" -> painterResource(id = R.drawable.ic_directions_car_vencehoje)
        "celebration" -> painterResource(id = R.drawable.ic_celebration_vencehoje)
        "medical_services" -> painterResource(id = R.drawable.ic_medical_vencehoje)
        "restaurant" -> painterResource(id = R.drawable.ic_restaurant_vencehoje)
        "school" -> painterResource(id = R.drawable.ic_school_vencehoje)
        else -> painterResource(id = R.drawable.ic_label_vencehoje)
    }
}

@Composable
fun CategoryDisplay(iconName: String, colorHex: String, modifier: Modifier = Modifier) {
    val catColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color.Gray
    }

    // Se tiver mais de 3 caracteres, assumimos que é um nome de ícone do sistema (ex: "shopping")
    // Se for curto ou tiver caracteres especiais, é emoji.
    val isSystemIcon = iconName.length > 3 && !iconName.any { Character.isSurrogate(it) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isSystemIcon) {
            Icon(
                painter = getIconPainterFromName(iconName),
                contentDescription = null,
                tint = catColor,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = iconName,
                fontSize = 20.sp // Emojis ficam bem nesse tamanho
            )
        }
    }
}

@Composable
fun StatusTag(text: String, containerColor: Color, contentColor: Color) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = contentColor
        )
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