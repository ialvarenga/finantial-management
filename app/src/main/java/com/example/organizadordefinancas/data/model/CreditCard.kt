package com.example.organizadordefinancas.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cardLimit: Double,
    val dueDay: Int,
    val closingDay: Int,
    val color: Long = 0xFF6200EE,
    val lastFourDigits: String? = null  // Last 4 digits for Google Wallet/notification matching
)
