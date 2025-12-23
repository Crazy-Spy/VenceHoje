package com.vencehoje.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. Adicionamos Profile::class e subimos a versão para 3
@Database(entities = [Bill::class, Category::class, Profile::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
    abstract fun categoryDao(): CategoryDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // --- A MÁGICA DA MIGRAÇÃO ---
        // Transforma o banco v2 (sem perfis) no banco v3 (com perfis) sem perder nada.
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Cria a tabela de Perfis
                database.execSQL("CREATE TABLE IF NOT EXISTS `profiles` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isMain` INTEGER NOT NULL)")

                // 2. Cria automaticamente o "Perfil Principal" (ID 1) para você
                database.execSQL("INSERT INTO profiles (id, name, colorHex, iconName, isMain) VALUES (1, 'Principal', '#1976D2', 'person', 1)")

                // 3. Adiciona a coluna profileId nas Contas existentes e define que elas são do Perfil 1
                database.execSQL("ALTER TABLE bills ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")

                // 4. Adiciona a coluna profileId nas Categorias existentes e define que elas são do Perfil 1
                database.execSQL("ALTER TABLE categories ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vencehoje_database"
                )
                    .addMigrations(MIGRATION_2_3) // Aplica a regra acima
                    // .fallbackToDestructiveMigration() // REMOVIDO PARA SEGURANÇA DOS DADOS!
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}