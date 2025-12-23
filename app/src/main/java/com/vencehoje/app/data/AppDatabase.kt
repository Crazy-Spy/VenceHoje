package com.vencehoje.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.BillDao
import com.vencehoje.app.data.Category
import com.vencehoje.app.data.CategoryDao

// Atualizado para versão 2 e inclusão da entidade Category
@Database(entities = [Bill::class, Category::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vencehoje_database"
                )
                    // ATENÇÃO: Isso apagará os dados atuais para aplicar a nova estrutura do Bill.kt
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}