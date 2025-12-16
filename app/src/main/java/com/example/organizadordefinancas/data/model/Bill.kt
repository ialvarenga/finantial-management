package com.example.organizadordefinancas.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a monthly credit card statement/bill.
 * Bills are automatically generated on the card's closing date (fechamento) and group
 * all transactions from that billing cycle.
 *
 * Billing Cycle Example:
 * Bill for January 2024:
 *   Cycle: Dec 11, 2023 - Jan 10, 2024
 *   Closing: Jan 10, 2024
 *   Due: Jan 15, 2024
 *   Transactions in this cycle → Linked to this bill
 */
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = CreditCard::class,
            parentColumns = ["id"],
            childColumns = ["credit_card_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["credit_card_id"]),
        Index(value = ["year", "month"]),
        Index(value = ["status"]),
        Index(value = ["due_date"])
    ]
)
data class Bill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /**
     * Foreign key linking to the Credit Card
     */
    @ColumnInfo(name = "credit_card_id")
    val creditCardId: Long,

    /**
     * Year of the bill (e.g., 2024)
     */
    val year: Int,

    /**
     * Month of the bill (1-12)
     */
    val month: Int,

    /**
     * Date when the billing cycle closed (timestamp in milliseconds)
     */
    @ColumnInfo(name = "closing_date")
    val closingDate: Long,

    /**
     * Date when payment is due (timestamp in milliseconds)
     */
    @ColumnInfo(name = "due_date")
    val dueDate: Long,

    /**
     * Total amount owed (sum of all transactions in cycle)
     */
    @ColumnInfo(name = "total_amount")
    val totalAmount: Double = 0.0,

    /**
     * Amount paid so far (0.00 if unpaid)
     */
    @ColumnInfo(name = "paid_amount")
    val paidAmount: Double = 0.0,

    /**
     * Bill status: "open", "paid", "overdue", "partial"
     */
    val status: String = BillStatus.OPEN,

    /**
     * Optional link to the payment transaction when paid
     */
    @ColumnInfo(name = "payment_transaction_id")
    val paymentTransactionId: Long? = null,

    /**
     * When the bill was generated (timestamp in milliseconds)
     */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Calculates the remaining amount to be paid
     */
    fun getRemainingAmount(): Double = totalAmount - paidAmount

    /**
     * Checks if the bill is fully paid
     */
    fun isFullyPaid(): Boolean = paidAmount >= totalAmount

    /**
     * Checks if the bill is overdue (past due date and not fully paid)
     */
    fun isOverdue(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime > dueDate && !isFullyPaid()
    }

    /**
     * Updates status based on payment and due date
     */
    fun calculateStatus(currentTime: Long = System.currentTimeMillis()): String {
        return when {
            isFullyPaid() -> BillStatus.PAID
            paidAmount > 0 -> BillStatus.PARTIAL
            currentTime > dueDate -> BillStatus.OVERDUE
            else -> BillStatus.OPEN
        }
    }
}

/**
 * Supported bill statuses
 */
object BillStatus {
    const val OPEN = "open"         // Bill created, not paid yet
    const val PAID = "paid"         // Full amount paid (paid_amount = total_amount)
    const val PARTIAL = "partial"   // Partially paid (0 < paid_amount < total_amount)
    const val OVERDUE = "overdue"   // Past due date and not fully paid
}

