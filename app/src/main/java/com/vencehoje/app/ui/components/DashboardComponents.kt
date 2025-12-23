package com.vencehoje.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

@Composable
fun PieChart(data: Map<String, Double>, categoryColors: Map<String, Color>) {
    val totalSum = data.values.sum()
    if (totalSum <= 0) return
    var startAngle = -90f

    Box(modifier = Modifier.size(240.dp).padding(16.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            data.forEach { (categoryName, value) ->
                val sweepAngle = (value / totalSum * 360).toFloat()
                // Pega a cor do mapa de cores ou usa cinza se não achar
                val color = categoryColors[categoryName] ?: Color.LightGray

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 35.dp.toPx())
                )
                startAngle += sweepAngle
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", fontSize = 12.sp, color = Color.Gray)
            Text(NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(totalSum), fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun DashboardLegend(data: Map<String, Double>, categoryColors: Map<String, Color>) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    val totalGeral = data.values.sum()

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        data.toList().sortedByDescending { it.second }.forEach { (categoryName, total) ->
            val percentagem = if (totalGeral > 0) (total / totalGeral) * 100 else 0.0
            val color = categoryColors[categoryName] ?: Color.LightGray

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                Text(
                    text = String.format("%.1f%% %s", percentagem, categoryName),
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                    fontSize = 14.sp
                )
                Text(text = currencyFormatter.format(total), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TOTAL EXIBIDO",
                fontWeight = FontWeight.Black,
                fontSize = 16.sp
            )
            Text(
                text = currencyFormatter.format(totalGeral),
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color(0xFF1B5E20)
            )
        }
    }
}