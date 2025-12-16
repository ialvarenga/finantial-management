package com.example.organizadordefinancas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a financial account at a bank (e.g., "Nubank Checking", "Itaú Savings").
 * This replaces the previous Bank entity with a more structured approach.
 */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * User-defined account name (e.g., "My Main Account", "Emergency Savings")
     */
    val name: String,

    /**
     * Name of the financial institution from predefined list (e.g., "Nubank", "Itaú", "Bradesco")
     */
    @ColumnInfo(name = "bank_name")
    val bankName: String,

    /**
     * Optional account number for reference
     */
    @ColumnInfo(name = "account_number")
    val accountNumber: String? = null,

    /**
     * Type of account: "checking", "savings", "investment", "wallet"
     */
    @ColumnInfo(name = "account_type")
    val accountType: String = "checking",

    /**
     * Optional URL to the bank's logo for UI display
     */
    @ColumnInfo(name = "logo_url")
    val logoUrl: String? = null,

    /**
     * Optional color code for UI theming (stored as ARGB Long)
     */
    val color: Long = 0xFF03DAC5, // Default teal color

    /**
     * Whether the account is active
     */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    /**
     * Creation timestamp
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Supported account types
 */
object AccountTypes {
    const val CHECKING = "checking"
    const val SAVINGS = "savings"
    const val INVESTMENT = "investment"
    const val WALLET = "wallet"
}

