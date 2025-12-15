package com.example.organizadordefinancas

import android.app.Application
import com.example.organizadordefinancas.data.database.AppDatabase
import com.example.organizadordefinancas.data.repository.BankRepository
import com.example.organizadordefinancas.data.repository.CreditCardRepository
import com.example.organizadordefinancas.data.repository.FinancialCompromiseRepository
import com.example.organizadordefinancas.data.repository.IncomeRepository

class FinanceApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    val creditCardRepository by lazy {
        CreditCardRepository(database.creditCardDao(), database.creditCardItemDao())
    }

    val bankRepository by lazy {
        BankRepository(database.bankDao())
    }

    val financialCompromiseRepository by lazy {
        FinancialCompromiseRepository(database.financialCompromiseDao())
    }

    val incomeRepository by lazy {
        IncomeRepository(database.incomeDao())
    }
}


