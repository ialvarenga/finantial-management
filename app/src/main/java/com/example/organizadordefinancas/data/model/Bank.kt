package com.example.organizadordefinancas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    CHECKING,   // Conta Corrente
    SAVINGS,    // Poupança
    INVESTMENT, // Investimento
    WALLET      // Carteira
}

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val balance: Double,
    val savingsBalance: Double = 0.0, // Emergency savings - not touched
    val accountType: AccountType = AccountType.CHECKING,
    val color: Long = 0xFF03DAC5 // Default teal color
)

