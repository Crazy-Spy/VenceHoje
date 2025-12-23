package com.vencehoje.app.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vencehoje.app.*
import com.vencehoje.app.data.BillRepository
import com.vencehoje.app.logic.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: BillRepository,
    onBack: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("configs", android.content.Context.MODE_PRIVATE) }

    var notifyTime by remember { mutableStateOf(prefs.getString("notify_time", "08:00") ?: "08:00") }
    var insistence by remember { mutableStateOf(prefs.getString("insistence", "Padrão") ?: "Padrão") }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            val newTime = String.format("%02d:%02d", hour, minute)
            notifyTime = newTime
            prefs.edit().putString("notify_time", newTime).apply()
        },
        notifyTime.split(":")[0].toInt(),
        notifyTime.split(":")[1].toInt(),
        true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Notificações",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )

            ListItem(
                headlineContent = { Text("Horário do Alerta") },
                supportingContent = { Text(notifyTime) },
                leadingContent = { Icon(Icons.Default.Notifications, null, tint = Color(0xFF1B5E20)) },
                modifier = Modifier.clickable { timePickerDialog.show() }
            )

            var expandedInsistence by remember { mutableStateOf(false) }
            Column {
                Box {
                    ListItem(
                        headlineContent = { Text("Insistência") },
                        supportingContent = { Text(insistence) },
                        leadingContent = { Icon(Icons.Default.Warning, null, tint = Color(0xFF1B5E20)) },
                        modifier = Modifier.clickable { expandedInsistence = true }
                    )
                    DropdownMenu(expanded = expandedInsistence, onDismissRequest = { expandedInsistence = false }) {
                        listOf("Padrão", "Alto", "Crítico").forEach { level ->
                            DropdownMenuItem(
                                text = { Text(level) },
                                onClick = {
                                    insistence = level
                                    prefs.edit().putString("insistence", level).apply()
                                    expandedInsistence = false
                                }
                            )
                        }
                    }
                }

                // --- CARD DE EXPLICAÇÃO DA INSISTÊNCIA ---
                val descricao = when(insistence) {
                    "Padrão" -> "Notifica apenas uma vez, pontualmente às $notifyTime no dia do vencimento."

                    "Alto" -> "Primeiro alerta às $notifyTime. Depois, o app insiste mais 2 vezes ao dia até você pagar."

                    "Crítico" -> "Começa às $notifyTime e repete a cada 2 horas (até as 22h)."

                    else -> ""
                }

                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = descricao,
                            fontSize = 12.sp,
                            color = Color(0xFFE65100),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            ListItem(
                headlineContent = { Text("Testar Notificação") },
                supportingContent = { Text("Verificar se os alertas estão chegando") },
                leadingContent = { Icon(Icons.Default.Send, null, tint = Color(0xFF1976D2)) },
                modifier = Modifier.clickable {
                    NotificationWorker.sendTestNotification(context)
                    Toast.makeText(context, "Alerta de teste enviado! 🚀", Toast.LENGTH_SHORT).show()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Dados e Segurança",
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )

            ListItem(
                headlineContent = { Text("Exportar Backup") },
                supportingContent = { Text("Exportar dados para um arquivo") },
                leadingContent = { Icon(Icons.Default.Share, null, tint = Color(0xFF1B5E20)) },
                modifier = Modifier.clickable { onExport() }
            )

            ListItem(
                headlineContent = { Text("Importar Backup") },
                supportingContent = { Text("Restaurar dados de um arquivo anterior") },
                leadingContent = { Icon(Icons.Default.Refresh, null, tint = Color(0xFFD32F2F)) },
                modifier = Modifier.clickable { onImport() }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                "Versão 1.0.1 - VenceHoje",
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}