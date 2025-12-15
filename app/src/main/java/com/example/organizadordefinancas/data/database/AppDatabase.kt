package com.example.organizadordefinancas.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.organizadordefinancas.data.dao.BankDao
import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.dao.CreditCardItemDao
import com.example.organizadordefinancas.data.dao.FinancialCompromiseDao
import com.example.organizadordefinancas.data.model.Bank
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.data.model.FinancialCompromise

@Database(
    entities = [
        CreditCard::class,
        CreditCardItem::class,
        Bank::class,
        FinancialCompromise::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun creditCardItemDao(): CreditCardItemDao
    abstract fun bankDao(): BankDao
    abstract fun financialCompromiseDao(): FinancialCompromiseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
