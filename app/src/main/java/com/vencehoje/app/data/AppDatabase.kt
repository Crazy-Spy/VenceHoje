package com.vencehoje.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.vencehoje.app.data.Bill
import com.vencehoje.app.data.BillDao

@Database(entities = [Bill::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vencehoje_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}