package com.vencehoje.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val value: String = "",
    val paidValue: String? = null,
    val dueDate: String,
    val paymentDate: String? = null,
    val category: String = "Outros",
    val periodicity: String = "Mês",
    val customInterval: Int = 1,
    val totalInstallments: Int = 0,
    val currentInstallment: Int = 1,
    val isPaid: Boolean = false,
    val isAutomatic: Boolean = false
)