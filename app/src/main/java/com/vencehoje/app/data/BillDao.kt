package com.vencehoje.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    // --- MÉTODOS FILTRADOS (Para a UI do Dashboard) ---
    // Só traz as contas do perfil que está selecionado na tela
    @Query("SELECT * FROM bills WHERE profileId = :profileId ORDER BY CASE periodicity WHEN 'U' THEN 1 ELSE 0 END, dueDate ASC")
        fun getBillsByProfile(profileId: Int): Flow<List<Bill>>

        // Para somatórios e relatórios de um perfil específico
        @Query("SELECT * FROM bills WHERE profileId = :profileId")
        suspend fun getBillsByProfileSync(profileId: Int): List<Bill>

        // --- MÉTODOS GLOBAIS (Para o NotificationWorker e Backup) ---
        // O Worker precisa ver TUDO para avisar se a conta do seu pai ou a sua vai vencer
        @Query("SELECT * FROM bills")
        suspend fun getAllBillsGlobalSync(): List<Bill>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertBill(bill: Bill): Long

        @Update
        suspend fun updateBill(bill: Bill)

        @Delete
        suspend fun deleteBill(bill: Bill)

        // Deleta todas as contas de um perfil (cuidado!)
        @Query("DELETE FROM bills WHERE profileId = :profileId")
        suspend fun deleteBillsByProfile(profileId: Int)
}