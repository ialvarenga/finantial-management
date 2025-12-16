package com.example.organizadordefinancas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a credit card account.
 * Works together with Bill entity to manage monthly billing cycles.
 */
@Entity(tableName = "credit_cards")
data class CreditCard(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * User-defined name for the card
     */
    val name: String,

    /**
     * Credit limit (renamed from cardLimit for consistency)
     */
    @ColumnInfo(name = "card_limit")
    val cardLimit: Double,

    /**
     * Due date - day of month when payment is due (vencimento)
     */
    @ColumnInfo(name = "due_day")
    val dueDay: Int,

    /**
     * Closing date - day of month when billing cycle ends (fechamento)
     */
    @ColumnInfo(name = "closing_day")
    val closingDay: Int,

    /**
     * Color code for UI display (stored as ARGB Long)
     */
    val color: Long = 0xFF6200EE,

    /**
     * Last 4 digits for Google Wallet/notification matching
     */
    @ColumnInfo(name = "last_four_digits")
    val lastFourDigits: String? = null,

    /**
     * Boolean to automatically create bills on closing date
     */
    @ColumnInfo(name = "auto_generate_bills")
    val autoGenerateBills: Boolean = true,

    /**
     * Whether the card is active
     */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    /**
     * Creation timestamp
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
