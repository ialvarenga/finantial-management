package com.example.organizadordefinancas.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.organizadordefinancas.data.dao.AccountDao
import com.example.organizadordefinancas.data.dao.BalanceDao
import com.example.organizadordefinancas.data.dao.BankDao
import com.example.organizadordefinancas.data.dao.BillDao
import com.example.organizadordefinancas.data.dao.CapturedNotificationDao
import com.example.organizadordefinancas.data.dao.CompromiseOccurrenceDao
import com.example.organizadordefinancas.data.dao.CreditCardDao
import com.example.organizadordefinancas.data.dao.CreditCardItemDao
import com.example.organizadordefinancas.data.dao.FinancialCompromiseDao
import com.example.organizadordefinancas.data.dao.IncomeDao
import com.example.organizadordefinancas.data.dao.TransactionDao
import com.example.organizadordefinancas.data.model.Account
import com.example.organizadordefinancas.data.model.Balance
import com.example.organizadordefinancas.data.model.Bank
import com.example.organizadordefinancas.data.model.Bill
import com.example.organizadordefinancas.data.model.CapturedNotification
import com.example.organizadordefinancas.data.model.CompromiseOccurrence
import com.example.organizadordefinancas.data.model.CreditCard
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.data.model.FinancialCompromise
import com.example.organizadordefinancas.data.model.Income
import com.example.organizadordefinancas.data.model.Transaction

@Database(
    entities = [
        // Legacy entities (kept for migration)
        CreditCard::class,
        CreditCardItem::class,
        Bank::class,
        FinancialCompromise::class,
        Income::class,
        CompromiseOccurrence::class,
        CapturedNotification::class,
        // New entities for the new data model
        Account::class,
        Balance::class,
        Bill::class,
        Transaction::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Legacy DAOs (kept for migration)
    abstract fun creditCardDao(): CreditCardDao
    abstract fun creditCardItemDao(): CreditCardItemDao
    abstract fun bankDao(): BankDao
    abstract fun financialCompromiseDao(): FinancialCompromiseDao
    abstract fun incomeDao(): IncomeDao
    abstract fun compromiseOccurrenceDao(): CompromiseOccurrenceDao
    abstract fun capturedNotificationDao(): CapturedNotificationDao

    // New DAOs for the new data model
    abstract fun accountDao(): AccountDao
    abstract fun balanceDao(): BalanceDao
    abstract fun billDao(): BillDao
    abstract fun transactionDao(): TransactionDao

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

        // Migration from version 9 to 10: Add new data model entities (Account, Balance, Bill, Transaction)
        // and update CreditCard with new fields
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create accounts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        bank_name TEXT NOT NULL,
                        account_number TEXT DEFAULT NULL,
                        account_type TEXT NOT NULL DEFAULT 'checking',
                        logo_url TEXT DEFAULT NULL,
                        color INTEGER NOT NULL DEFAULT ${0xFF03DAC5},
                        is_active INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}
                    )
                """.trimIndent())

                // 2. Create balances table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS balances (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        account_id INTEGER NOT NULL,
                        current_balance REAL NOT NULL DEFAULT 0.0,
                        balance_type TEXT NOT NULL DEFAULT 'account',
                        goal_amount REAL DEFAULT NULL,
                        currency TEXT NOT NULL DEFAULT 'BRL',
                        is_active INTEGER NOT NULL DEFAULT 1,
                        color INTEGER DEFAULT NULL,
                        icon TEXT DEFAULT NULL,
                        created_at INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        updated_at INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_balances_account_id ON balances(account_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_balances_balance_type ON balances(balance_type)")

                // 3. Create bills table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bills (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        credit_card_id INTEGER NOT NULL,
                        year INTEGER NOT NULL,
                        month INTEGER NOT NULL,
                        closing_date INTEGER NOT NULL,
                        due_date INTEGER NOT NULL,
                        total_amount REAL NOT NULL DEFAULT 0.0,
                        paid_amount REAL NOT NULL DEFAULT 0.0,
                        status TEXT NOT NULL DEFAULT 'open',
                        payment_transaction_id INTEGER DEFAULT NULL,
                        created_at INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        FOREIGN KEY (credit_card_id) REFERENCES credit_cards(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_credit_card_id ON bills(credit_card_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_year_month ON bills(year, month)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_status ON bills(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_bills_due_date ON bills(due_date)")

                // 4. Create transactions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        amount REAL NOT NULL,
                        date INTEGER NOT NULL,
                        end_date INTEGER DEFAULT NULL,
                        frequency TEXT NOT NULL DEFAULT 'once',
                        balance_id INTEGER DEFAULT NULL,
                        type TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'completed',
                        bill_id INTEGER DEFAULT NULL,
                        parent_transaction_id INTEGER DEFAULT NULL,
                        is_installment_parent INTEGER NOT NULL DEFAULT 0,
                        installment_number INTEGER DEFAULT NULL,
                        total_installments INTEGER DEFAULT NULL,
                        installment_amount REAL DEFAULT NULL,
                        category TEXT NOT NULL DEFAULT 'Outros',
                        subcategory TEXT DEFAULT NULL,
                        description TEXT DEFAULT NULL,
                        transfer_pair_id INTEGER DEFAULT NULL,
                        created_at INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        updated_at INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()},
                        FOREIGN KEY (balance_id) REFERENCES balances(id) ON DELETE SET NULL,
                        FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE SET NULL,
                        FOREIGN KEY (parent_transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_balance_id ON transactions(balance_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_bill_id ON transactions(bill_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_parent_transaction_id ON transactions(parent_transaction_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_date ON transactions(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_status ON transactions(status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_category ON transactions(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_is_installment_parent ON transactions(is_installment_parent)")

                // 5. Update credit_cards table with new columns
                db.execSQL("ALTER TABLE credit_cards ADD COLUMN auto_generate_bills INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE credit_cards ADD COLUMN is_active INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE credit_cards ADD COLUMN created_at INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")

                // 6. Migrate existing bank data to accounts and balances
                // Create accounts from existing banks
                db.execSQL("""
                    INSERT INTO accounts (id, name, bank_name, account_type, color, is_active, created_at)
                    SELECT id, name, name, 
                           CASE accountType 
                               WHEN 'CHECKING' THEN 'checking'
                               WHEN 'SAVINGS' THEN 'savings'
                               WHEN 'INVESTMENT' THEN 'investment'
                               WHEN 'WALLET' THEN 'wallet'
                               ELSE 'checking'
                           END,
                           color, 1, ${System.currentTimeMillis()}
                    FROM banks
                """.trimIndent())

                // Create main balances from existing banks
                db.execSQL("""
                    INSERT INTO balances (name, account_id, current_balance, balance_type, is_active, created_at, updated_at)
                    SELECT 'Principal', id, balance, 'account', 1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                    FROM banks
                """.trimIndent())

                // Create savings pool balances from existing banks (where savingsBalance > 0)
                db.execSQL("""
                    INSERT INTO balances (name, account_id, current_balance, balance_type, is_active, created_at, updated_at)
                    SELECT 'Reserva', id, savingsBalance, 'pool', 1, ${System.currentTimeMillis()}, ${System.currentTimeMillis()}
                    FROM banks
                    WHERE savingsBalance > 0
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
