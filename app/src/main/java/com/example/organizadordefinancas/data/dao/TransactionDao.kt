package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // Basic CRUD operations
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransactionById(id: Long): Flow<Transaction?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionByIdSync(id: Long): Transaction?

    // Get transactions by balance ID
    @Query("SELECT * FROM transactions WHERE balance_id = :balanceId ORDER BY date DESC")
    fun getTransactionsByBalanceId(balanceId: Long): Flow<List<Transaction>>

    // Get transactions by bill ID
    @Query("SELECT * FROM transactions WHERE bill_id = :billId ORDER BY date DESC")
    fun getTransactionsByBillId(billId: Long): Flow<List<Transaction>>

    // Get transactions by bill ID (excluding installment parents)
    @Query("SELECT * FROM transactions WHERE bill_id = :billId AND is_installment_parent = 0 ORDER BY date DESC")
    fun getTransactionsByBillIdExcludingParents(billId: Long): Flow<List<Transaction>>

    // Get transactions excluding installment parents (CRITICAL for expense calculations)
    @Query("SELECT * FROM transactions WHERE is_installment_parent = 0 ORDER BY date DESC")
    fun getTransactionsExcludingParents(): Flow<List<Transaction>>

    // Get transactions by status
    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY date DESC")
    fun getTransactionsByStatus(status: String): Flow<List<Transaction>>

    // Get transactions by type
    @Query("SELECT * FROM transactions WHERE type = :type AND is_installment_parent = 0 ORDER BY date DESC")
    fun getTransactionsByType(type: String): Flow<List<Transaction>>

    // Get transactions by category
    @Query("SELECT * FROM transactions WHERE category = :category AND is_installment_parent = 0 ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>

    // Get transactions in date range (excluding installment parents)
    @Query("""
        SELECT * FROM transactions 
        WHERE date >= :startDate AND date <= :endDate 
        AND is_installment_parent = 0 
        ORDER BY date DESC
    """)
    fun getTransactionsInDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>

    // Get transactions with filters (excluding installment parents)
    @Query("""
        SELECT * FROM transactions 
        WHERE is_installment_parent = 0
        AND (:balanceId IS NULL OR balance_id = :balanceId)
        AND (:billId IS NULL OR bill_id = :billId)
        AND (:type IS NULL OR type = :type)
        AND (:status IS NULL OR status = :status)
        AND (:category IS NULL OR category = :category)
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY date DESC
    """)
    fun getFilteredTransactions(
        balanceId: Long? = null,
        billId: Long? = null,
        type: String? = null,
        status: String? = null,
        category: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Flow<List<Transaction>>

    // CRITICAL: Total expenses for date range (excludes installment parents)
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'expense' 
        AND status = 'completed'
        AND date >= :startDate AND date <= :endDate 
        AND is_installment_parent = 0
    """)
    fun getTotalExpensesInDateRange(startDate: Long, endDate: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'expense' 
        AND status = 'completed'
        AND date >= :startDate AND date <= :endDate 
        AND is_installment_parent = 0
    """)
    suspend fun getTotalExpensesInDateRangeSync(startDate: Long, endDate: Long): Double

    // Total income for date range
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'income' 
        AND status = 'completed'
        AND date >= :startDate AND date <= :endDate 
        AND is_installment_parent = 0
    """)
    fun getTotalIncomeInDateRange(startDate: Long, endDate: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE type = 'income' 
        AND status = 'completed'
        AND date >= :startDate AND date <= :endDate 
        AND is_installment_parent = 0
    """)
    suspend fun getTotalIncomeInDateRangeSync(startDate: Long, endDate: Long): Double

    // Total for a bill (excludes installment parents)
    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE bill_id = :billId 
        AND is_installment_parent = 0
    """)
    fun getBillTotal(billId: Long): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(amount), 0.0) FROM transactions 
        WHERE bill_id = :billId 
        AND is_installment_parent = 0
    """)
    suspend fun getBillTotalSync(billId: Long): Double

    // Get expenses by category (for reports)
    @Query("""
        SELECT category, SUM(amount) as total FROM transactions 
        WHERE type = 'expense' 
        AND status = 'completed'
        AND date >= :startDate AND date <= :endDate 
        AND is_installment_parent = 0
        AND category != 'Pagamento Cartão'
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getExpensesByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    // Get income by category (for reports)
    @Query("""
        SELECT category, SUM(amount) as total FROM transactions 
        WHERE type = 'income' 
        AND status = 'completed'
        AND date >= :startDate AND date <= :endDate 
        AND is_installment_parent = 0
        GROUP BY category
        ORDER BY total DESC
    """)
    fun getIncomeByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    // Installment-related queries
    // Get installment children by parent ID
    @Query("SELECT * FROM transactions WHERE parent_transaction_id = :parentId ORDER BY installment_number ASC")
    fun getInstallmentChildren(parentId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE parent_transaction_id = :parentId ORDER BY installment_number ASC")
    suspend fun getInstallmentChildrenSync(parentId: Long): List<Transaction>

    // Get active installment parents (use for installment management screen)
    @Query("""
        SELECT * FROM transactions 
        WHERE is_installment_parent = 1 
        AND status != 'cancelled'
        ORDER BY date DESC
    """)
    fun getActiveInstallmentParents(): Flow<List<Transaction>>

    // Get installment parents with remaining payments
    @Query("""
        SELECT p.* FROM transactions p
        WHERE p.is_installment_parent = 1 
        AND p.status != 'cancelled'
        AND EXISTS (
            SELECT 1 FROM transactions c 
            WHERE c.parent_transaction_id = p.id 
            AND c.status = 'expected'
        )
        ORDER BY p.date DESC
    """)
    fun getInstallmentParentsWithRemainingPayments(): Flow<List<Transaction>>

    // Count completed installments for a parent
    @Query("SELECT COUNT(*) FROM transactions WHERE parent_transaction_id = :parentId AND status = 'completed'")
    suspend fun countCompletedInstallments(parentId: Long): Int

    // Count expected installments for a parent
    @Query("SELECT COUNT(*) FROM transactions WHERE parent_transaction_id = :parentId AND status = 'expected'")
    suspend fun countExpectedInstallments(parentId: Long): Int

    // Expected transactions (future/planned)
    @Query("""
        SELECT * FROM transactions 
        WHERE status = 'expected' 
        AND is_installment_parent = 0
        AND date >= :fromDate
        ORDER BY date ASC
    """)
    fun getExpectedTransactions(fromDate: Long = System.currentTimeMillis()): Flow<List<Transaction>>

    // Recent transactions (for dashboard)
    @Query("""
        SELECT * FROM transactions 
        WHERE is_installment_parent = 0
        AND status = 'completed'
        ORDER BY date DESC 
        LIMIT :limit
    """)
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>>

    // Insert/Update/Delete
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    // Update transaction status
    @Query("UPDATE transactions SET status = :status, updated_at = :updatedAt WHERE id = :transactionId")
    suspend fun updateTransactionStatus(transactionId: Long, status: String, updatedAt: Long = System.currentTimeMillis())

    // Cancel remaining installments for a parent
    @Query("UPDATE transactions SET status = 'cancelled', updated_at = :updatedAt WHERE parent_transaction_id = :parentId AND status = 'expected'")
    suspend fun cancelRemainingInstallments(parentId: Long, updatedAt: Long = System.currentTimeMillis())

    // Get transfer pair
    @Query("SELECT * FROM transactions WHERE transfer_pair_id = :transferPairId")
    fun getTransferPair(transferPairId: Long): Flow<List<Transaction>>
}

/**
 * Data class for category totals used in reports
 */
data class CategoryTotal(
    val category: String,
    val total: Double
)

