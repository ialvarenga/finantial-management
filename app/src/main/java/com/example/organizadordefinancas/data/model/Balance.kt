package com.example.organizadordefinancas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents either the main balance of an account or a savings pool (caixinha) within that account.
 * This unified approach allows both account balances and savings goals to be modeled using the same entity.
 *
 * Available Balance Calculation:
 * For an account with pools, the available balance is calculated as:
 * Available = Main Balance - Sum(All Pool Balances for this account_id)
 */
@Entity(
    tableName = "balances",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["account_id"]),
        Index(value = ["balance_type"])
    ]
)
data class Balance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Display name (e.g., "Main", "Emergency Fund", "Vacation")
     */
    val name: String,

    /**
     * Foreign key linking to the Account this balance belongs to
     */
    @ColumnInfo(name = "account_id")
    val accountId: Long,

    /**
     * The actual balance amount
     */
    @ColumnInfo(name = "current_balance")
    val currentBalance: Double = 0.0,

    /**
     * Either "account" (main balance) or "pool" (savings goal/caixinha)
     */
    @ColumnInfo(name = "balance_type")
    val balanceType: String = BalanceTypes.ACCOUNT,

    /**
     * Optional target amount (typically used for pools)
     */
    @ColumnInfo(name = "goal_amount")
    val goalAmount: Double? = null,

    /**
     * Currency code if supporting multiple currencies (e.g., "BRL", "USD")
     */
    val currency: String = "BRL",

    /**
     * Boolean to mark inactive balances
     */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    /**
     * Optional color code for UI display (stored as ARGB Long)
     */
    val color: Long? = null,

    /**
     * Optional icon identifier for UI display
     */
    val icon: String? = null,

    /**
     * Creation timestamp
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /**
     * Last update timestamp
     */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculates the progress towards the goal (0.0 to 1.0+)
     * Returns null if no goal is set
     */
    fun getGoalProgress(): Double? {
        return goalAmount?.let { goal ->
            if (goal > 0) currentBalance / goal else null
        }
    }

    /**
     * Checks if this is a pool (savings goal/caixinha)
     */
    fun isPool(): Boolean = balanceType == BalanceTypes.POOL

    /**
     * Checks if this is the main account balance
     */
    fun isMainBalance(): Boolean = balanceType == BalanceTypes.ACCOUNT
}

/**
 * Supported balance types
 */
object BalanceTypes {
    const val ACCOUNT = "account"  // Main balance of an account
    const val POOL = "pool"        // Savings goal/caixinha
}

