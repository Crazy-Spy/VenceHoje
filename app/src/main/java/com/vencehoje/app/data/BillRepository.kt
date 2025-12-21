package com.vencehoje.app.data

import kotlinx.coroutines.flow.Flow

class BillRepository(private val billDao: BillDao) {
    val allBills: Flow<List<Bill>> = billDao.getAllBills()

    suspend fun insert(bill: Bill) {
        billDao.insertBill(bill)
    }

    suspend fun delete(bill: Bill) {
        billDao.deleteBill(bill)
    }

    suspend fun update(bill: Bill) {
        billDao.updateBill(bill)
    }

    suspend fun deleteAll() {
        billDao.deleteAll()
    }
}