package com.vencehoje.app.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.BillRepository

// Função auxiliar usada em vários lugares
fun getDaysRemaining(dueDate: String): Long {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return try {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate, formatter))
    } catch (e: Exception) {
        0
    }
}

suspend fun processPayment(bill: Bill, repository: BillRepository, finalValue: String) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val today = LocalDate.now().format(formatter)

    // Limpeza para checar se o valor original era zero
    val numericOriginal = bill.value.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L

    // Se a conta era variável (zero), o valor base para o histórico será o próprio valor pago
    val baseValueForHistory = if (numericOriginal == 0L) finalValue else bill.value

    // 1. CRIA O HISTÓRICO (Cópia Paga)
    // O profileId é copiado automaticamente do objeto 'bill', então cai no perfil certo.
    // MUDANÇA: insert -> insertBill
    repository.insertBill(bill.copy(
        id = 0, // ID 0 força o Room a criar um novo registro
        isPaid = true,
        paymentDate = today,
        paidValue = finalValue,
        value = baseValueForHistory
    ))

    // 2. TRATA A CONTA ORIGINAL (A que fica em aberto)
    if (bill.totalInstallments > 0 && bill.currentInstallment >= bill.totalInstallments) {
        // Se acabou as parcelas, a gente mata a conta original para ela sumir da lista de "A Pagar"
        // MUDANÇA: delete -> deleteBill
        repository.deleteBill(bill)
    } else {
        // Se ainda tem chão, a gente empurra a data pra frente
        val date = try {
            LocalDate.parse(bill.dueDate, formatter)
        } catch (e: Exception) { LocalDate.now() }

        val nextDate = when (bill.periodicity) {
            "Dia" -> date.plusDays(bill.customInterval.toLong())
            "Semana" -> date.plusWeeks(bill.customInterval.toLong())
            "Mês" -> date.plusMonths(bill.customInterval.toLong())
            "Ano" -> date.plusYears(bill.customInterval.toLong())
            else -> date.plusMonths(1)
        }

        // MUDANÇA: update -> updateBill
        repository.updateBill(bill.copy(
            dueDate = nextDate.format(formatter),
            currentInstallment = bill.currentInstallment + 1,
            isAutomatic = bill.isAutomatic
        ))
    }
}