package com.vencehoje.app.data

import kotlinx.coroutines.flow.Flow

class BillRepository(
    private val billDao: BillDao,
    private val categoryDao: CategoryDao
) {
    // --- LÓGICA DE CONTAS (BILLS) ---

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

    suspend fun deleteAllBills() {
        billDao.deleteAll()
    }

    // --- LÓGICA DE CATEGORIAS ---

    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insert(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }

    /**
     * Carga Inicial: Se o banco estiver vazio, insere as categorias padrão.
     * Útil para o primeiro acesso do Pedro e do Bruno.
     */
    suspend fun checkAndSeedCategories() {
        val currentCategories = categoryDao.getAllCategoriesSync()
        if (currentCategories.isEmpty()) {
            val defaultCategories = listOf(
                Category(name = "Moradia", colorHex = "#1976D2", iconName = "home", isBuiltIn = true),
                Category(name = "Transporte", colorHex = "#FBC02D", iconName = "directions_car", isBuiltIn = true),
                Category(name = "Saúde", colorHex = "#2FD3B2", iconName = "medical_services", isBuiltIn = true),
                Category(name = "Lazer", colorHex = "#7B1FA2", iconName = "celebration", isBuiltIn = true),
                Category(name = "Alimentação", colorHex = "#388E3C", iconName = "restaurant", isBuiltIn = true),
                Category(name = "Educação", colorHex = "#00796B", iconName = "school", isBuiltIn = true),
                Category(name = "Outros", colorHex = "#9E9E9E", iconName = "label", isBuiltIn = true)
            )
            defaultCategories.forEach { categoryDao.insert(it) }
        }
    }
}