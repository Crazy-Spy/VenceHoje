package com.vencehoje.app.logic

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.BillRepository


// Extraído do final do MainActivity.kt
fun getDaysRemaining(dueDate: String): Long {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return try {
        ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate, formatter))
    } catch (e: Exception) {
        0
    }
}

// Extraído do final do MainActivity.kt
suspend fun processPayment(bill: Bill, repository: BillRepository, finalValue: String) {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val today = LocalDate.now().format(formatter)

    // Limpeza para checar se o valor original era zero
    val numericOriginal = bill.value.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L

    // Se a conta era variável (zero), o valor base para o histórico será o próprio valor pago
    // Isso impede que o Dashboard calcule juros sobre zero.
    val baseValueForHistory = if (numericOriginal == 0L) finalValue else bill.value

    repository.insert(bill.copy(
        id = 0,
        isPaid = true,
        paymentDate = today,
        paidValue = finalValue,
        value = baseValueForHistory // Ajuste crucial aqui!
    ))

    if (bill.totalInstallments > 0 && bill.currentInstallment >= bill.totalInstallments) {
        repository.delete(bill)
    } else {
        val date = LocalDate.parse(bill.dueDate, formatter)
        val nextDate = when (bill.periodicity) {
            "Dia" -> date.plusDays(bill.customInterval.toLong())
            "Semana" -> date.plusWeeks(bill.customInterval.toLong())
            "Mês" -> date.plusMonths(bill.customInterval.toLong())
            "Ano" -> date.plusYears(bill.customInterval.toLong())
            else -> date.plusMonths(1)
        }
        repository.update(bill.copy(
            dueDate = nextDate.format(formatter),
            currentInstallment = bill.currentInstallment + 1,
            isAutomatic = bill.isAutomatic
        ))
    }
}