package com.example.organizadordefinancas.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.organizadordefinancas.data.dao.BankDao
import com.example.organizadordefinancas.data.dao.CapturedNotificationDao
import com.example.organizadordefinancas.data.dao.CompromiseOccurrenceDao
import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.dao.CreditCardItemDao
import com.example.organizadordefinancas.data.dao.FinancialCompromiseDao
import com.example.organizadordefinancas.data.dao.IncomeDao
import com.example.organizadordefinancas.data.model.Bank
import com.example.organizadordefinancas.data.model.CapturedNotification
import com.example.organizadordefinancas.data.model.CompromiseOccurrence
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.data.model.Income

@Database(
    entities = [
        CreditCard::class,
        CreditCardItem::class,
        Bank::class,
        FinancialCompromise::class,
        Income::class,
        CompromiseOccurrence::class,
        CapturedNotification::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun creditCardItemDao(): CreditCardItemDao
    abstract fun bankDao(): BankDao
    abstract fun financialCompromiseDao(): FinancialCompromiseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun compromiseOccurrenceDao(): CompromiseOccurrenceDao
    abstract fun capturedNotificationDao(): CapturedNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 5 to 6: Add savingsBalance column to banks table
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE banks ADD COLUMN savingsBalance REAL NOT NULL DEFAULT 0.0")
            }
        }

        // Migration from version 6 to 7: Add frequency fields to financial_compromises and create compromise_occurrences table
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to financial_compromises table
                db.execSQL("ALTER TABLE financial_compromises ADD COLUMN frequency TEXT NOT NULL DEFAULT 'MONTHLY'")
                db.execSQL("ALTER TABLE financial_compromises ADD COLUMN dayOfWeek INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE financial_compromises ADD COLUMN dayOfMonth INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE financial_compromises ADD COLUMN monthOfYear INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE financial_compromises ADD COLUMN startDate INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                db.execSQL("ALTER TABLE financial_compromises ADD COLUMN endDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE financial_compromises ADD COLUMN reminderDaysBefore INTEGER NOT NULL DEFAULT 3")

                // Create compromise_occurrences table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS compromise_occurrences (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        compromiseId INTEGER NOT NULL,
                        dueDate INTEGER NOT NULL,
                        expectedAmount REAL NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        paidDate INTEGER DEFAULT NULL,
                        paidAmount REAL DEFAULT NULL,
                        notes TEXT DEFAULT NULL,
                        FOREIGN KEY (compromiseId) REFERENCES financial_compromises(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create indices for compromise_occurrences
                db.execSQL("CREATE INDEX IF NOT EXISTS index_compromise_occurrences_compromiseId ON compromise_occurrences(compromiseId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_compromise_occurrences_dueDate ON compromise_occurrences(dueDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_compromise_occurrences_status ON compromise_occurrences(status)")
            }
        }

        // Migration from version 7 to 8: Add lastFourDigits to credit_cards and create captured_notifications table
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add lastFourDigits column to credit_cards table
                db.execSQL("ALTER TABLE credit_cards ADD COLUMN lastFourDigits TEXT DEFAULT NULL")

                // Create captured_notifications table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS captured_notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        packageName TEXT NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        capturedAt INTEGER NOT NULL,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        extractedAmount REAL DEFAULT NULL,
                        extractedMerchant TEXT DEFAULT NULL,
                        extractedCardLastFour TEXT DEFAULT NULL,
                        linkedItemId INTEGER DEFAULT NULL,
                        linkedCardId INTEGER DEFAULT NULL,
                        parsingConfidence REAL DEFAULT NULL,
                        rawNotificationExtras TEXT DEFAULT NULL,
                        FOREIGN KEY (linkedItemId) REFERENCES credit_card_items(id) ON DELETE SET NULL
                    )
                """.trimIndent())

                // Create indices for captured_notifications
                db.execSQL("CREATE INDEX IF NOT EXISTS index_captured_notifications_linkedItemId ON captured_notifications(linkedItemId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_captured_notifications_status ON captured_notifications(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_captured_notifications_capturedAt ON captured_notifications(capturedAt)")
            }
        }

        // Migration from version 8 to 9: Add transactionType and linkedBankId to captured_notifications
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE captured_notifications ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'PURCHASE'")
                db.execSQL("ALTER TABLE captured_notifications ADD COLUMN linkedBankId INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
