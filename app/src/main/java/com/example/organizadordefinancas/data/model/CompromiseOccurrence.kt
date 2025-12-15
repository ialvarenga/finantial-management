package com.example.organizadordefinancas.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Status of a captured occurrence of a recurring compromise.
 */
enum class OccurrenceStatus {
    PENDING,    // Awaiting payment
    PAID,       // Paid on time
    LATE,       // Paid late
    SKIPPED     // Manually skipped/ignored
}

/**
 * Represents a single occurrence of a recurring financial compromise.
 * Each FinancialCompromise can have multiple occurrences based on its frequency.
 */
@Entity(
    tableName = "compromise_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = FinancialCompromise::class,
            parentColumns = ["id"],
            childColumns = ["compromiseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["compromiseId"]),
        Index(value = ["dueDate"]),
        Index(value = ["status"])
    ]
)
data class CompromiseOccurrence(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val compromiseId: Long,           // FK -> FinancialCompromise
    val dueDate: Long,                // Due date timestamp for this occurrence
    val expectedAmount: Double,       // Expected amount at time of generation
    val status: OccurrenceStatus = OccurrenceStatus.PENDING,
    val paidDate: Long? = null,       // When it was paid
    val paidAmount: Double? = null,   // Actual amount paid (may differ from expected)
    val notes: String? = null         // Optional notes for this occurrence
) {
    /**
     * Returns true if this occurrence is considered paid (PAID or LATE status).
     */
    fun isPaid(): Boolean = status == OccurrenceStatus.PAID || status == OccurrenceStatus.LATE

    /**
     * Returns true if this occurrence is overdue (past due date and not paid).
     */
    fun isOverdue(): Boolean {
        return status == OccurrenceStatus.PENDING && dueDate < System.currentTimeMillis()
    }
}

