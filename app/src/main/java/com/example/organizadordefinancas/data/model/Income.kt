package com.example.organizadordefinancas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IncomeCategory {
    SALARY,         // Salário
    FREELANCE,      // Freelance
    INVESTMENT,     // Investimento
    BONUS,          // Bônus
    GIFT,           // Presente
    RENTAL,         // Aluguel
    SALE,           // Venda
    REFUND,         // Reembolso
    OTHER           // Outros
}

enum class IncomeType {
    RECURRENT,      // Recorrente (ex: salário)
    ONE_TIME        // Único (ex: valor recebido pontual)
}

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val amount: Double,
    val category: IncomeCategory = IncomeCategory.OTHER,
    val type: IncomeType = IncomeType.ONE_TIME,
    val receiveDay: Int = 1, // Day of month for recurrent income (1-31)
    val date: Long = System.currentTimeMillis(), // Date for one-time income
    val isReceived: Boolean = false, // For current month tracking
    val isActive: Boolean = true // If the income is still active (for recurrent)
)

