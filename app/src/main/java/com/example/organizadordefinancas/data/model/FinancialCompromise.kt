package com.example.organizadordefinancas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CompromiseCategory {
    RENT,       // Aluguel
    ENERGY,     // Energia
    WATER,      // Água
    INTERNET,   // Internet
    PHONE,      // Telefone
    INSURANCE,  // Seguro
    STREAMING,  // Streaming (Netflix, Spotify, etc.)
    GYM,        // Academia
    EDUCATION,  // Educação
    HEALTH,     // Saúde
    TRANSPORT,  // Transporte
    OTHER       // Outros
}

@Entity(tableName = "financial_compromises")
data class FinancialCompromise(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val dueDay: Int, // Day of month when it's due (1-31)
    val category: CompromiseCategory = CompromiseCategory.OTHER,
    val isPaid: Boolean = false, // For current month
    val isActive: Boolean = true // If the compromise is still active
)

