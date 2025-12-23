package com.vencehoje.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    // --- MÉTODOS FILTRADOS (Para a UI de gerenciar categorias) ---
    // Cada perfil tem suas próprias categorias (ex: Seu pai pode não ter "Games")
    @Query("SELECT * FROM categories WHERE profileId = :profileId ORDER BY name ASC")
    fun getCategoriesByProfile(profileId: Int): Flow<List<Category>>

    // --- MÉTODOS GLOBAIS (Para o NotificationWorker) ---
    // O Worker precisa saber o ícone/emoji de qualquer categoria, de qualquer perfil
    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesGlobalSync(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // Útil para quando você cria um perfil novo e quer copiar as categorias padrão pra ele
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)
}