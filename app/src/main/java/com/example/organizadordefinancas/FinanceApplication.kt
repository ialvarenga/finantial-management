package com.example.organizadordefinancas

import android.app.Application
import com.example.organizadordefinancas.data.database.AppDatabase
import com.example.organizadordefinancas.data.repository.AccountRepository
import com.example.organizadordefinancas.data.repository.AnalyticsRepository
import com.example.organizadordefinancas.data.repository.BalanceRepository
import com.example.organizadordefinancas.data.repository.BankRepository
import com.example.organizadordefinancas.data.repository.BillRepository
import com.example.organizadordefinancas.data.repository.CapturedNotificationRepository
import com.example.organizadordefinancas.data.repository.CompromiseOccurrenceRepository
import com.example.organizadordefinancas.data.repository.CreditCardRepository
import com.example.organizadordefinancas.data.repository.FinancialCompromiseRepository
import com.example.organizadordefinancas.data.repository.IncomeRepository
import com.example.organizadordefinancas.data.repository.TransactionRepository
import com.example.organizadordefinancas.service.business.BillGenerationService
import com.example.organizadordefinancas.service.business.ExpenseCalculationService
import com.example.organizadordefinancas.service.business.InstallmentService
import com.example.organizadordefinancas.service.business.TransferService
import com.example.organizadordefinancas.worker.WorkManagerScheduler

class FinanceApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()

        // Schedule background workers for bill generation and overdue checks
        WorkManagerScheduler.scheduleAllWork(this)
    }

    // ==================== New Data Model Repositories ====================

    val accountRepository by lazy {
        AccountRepository(database.accountDao(), database.balanceDao())
    }

    val balanceRepository by lazy {
        BalanceRepository(database.balanceDao(), database.transactionDao())
    }

    val billRepository by lazy {
        BillRepository(database.billDao(), database.creditCardDao(), database.transactionDao())
    }

    val transactionRepository by lazy {
        TransactionRepository(database.transactionDao(), database.balanceDao(), database.billDao())
    }

    // ==================== Business Logic Services ====================

    val installmentService by lazy {
        InstallmentService(transactionRepository, billRepository)
    }

    val billGenerationService by lazy {
        BillGenerationService(billRepository, database.creditCardDao())
    }

    val transferService by lazy {
        TransferService(balanceRepository)
    }

    val expenseCalculationService by lazy {
        ExpenseCalculationService(transactionRepository, balanceRepository)
    }

    // ==================== Legacy Repositories ====================

    val creditCardRepository by lazy {
        CreditCardRepository(database.creditCardDao(), database.creditCardItemDao())
    }

    val bankRepository by lazy {
        BankRepository(database.bankDao())
    }

    val financialCompromiseRepository by lazy {
        FinancialCompromiseRepository(database.financialCompromiseDao())
    }

    val compromiseOccurrenceRepository by lazy {
        CompromiseOccurrenceRepository(database.compromiseOccurrenceDao())
    }

    val incomeRepository by lazy {
        IncomeRepository(database.incomeDao())
    }

    val capturedNotificationRepository by lazy {
        CapturedNotificationRepository(database.capturedNotificationDao())
    }

    val analyticsRepository by lazy {
        AnalyticsRepository(database.creditCardItemDao())
    }
}


