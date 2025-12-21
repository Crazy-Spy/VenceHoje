package com.vencehoje.app.logic

import android.content.Context
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.BillRepository


// Lógica de Importação extraída do MainActivity.kt
fun importFromCSV(context: Context, repository: BillRepository, scope: CoroutineScope, uri: Uri) {
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
        } catch (e: Exception) {
            Toast.makeText(context, "Erro no restore: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

// Lógica de Exportação extraída do MainActivity.kt
fun saveCsvToUri(context: Context, uri: Uri, bills: List<Bill>) {
    val content = StringBuilder("Nome;Valor;Vencimento;Categoria;Status;Valor Pago;Data Pagamento;Total Parcelas;Parcela Atual;Automatico\n")
    bills.forEach { content.append("${it.name};${it.value};${it.dueDate};${it.category};${if(it.isPaid) "Pago" else "Pendente"};${it.paidValue ?: ""};${it.paymentDate ?: ""};${it.totalInstallments};${it.currentInstallment};${if(it.isAutomatic) "Sim" else "Não"}\n") }
    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(content.toString().toByteArray())
        }
        Toast.makeText(context, "Backup gerado com sucesso!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Erro ao salvar: ${e.message}", Toast.LENGTH_LONG).show()
    }
}