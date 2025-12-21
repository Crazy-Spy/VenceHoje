package com.vencehoje.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY id DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills")
    suspend fun getAllBillsSync(): List<Bill> // Novo método para o Worker

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Update
    suspend fun updateBill(bill: Bill)

    @Query("DELETE FROM bills")
    suspend fun deleteAll()
}