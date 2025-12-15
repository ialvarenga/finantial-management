package com.example.organizadordefinancas.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_card_items",
    foreignKeys = [
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cardId"])]
)
data class CreditCardItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val description: String,
    val amount: Double,
    val purchaseDate: Long, // Timestamp in milliseconds
    val installments: Int = 1, // Number of installments
    val currentInstallment: Int = 1, // Current installment number
    val category: String = "Outros"
)

