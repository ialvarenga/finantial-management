package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.Bill
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    // Basic CRUD operations
    @Query("SELECT * FROM bills ORDER BY year DESC, month DESC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    fun getBillById(id: Long): Flow<Bill?>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillByIdSync(id: Long): Bill?

    // Get bills by credit card
    @Query("SELECT * FROM bills WHERE credit_card_id = :creditCardId ORDER BY year DESC, month DESC")
    fun getBillsByCreditCard(creditCardId: Long): Flow<List<Bill>>

    // Get bills by status
    @Query("SELECT * FROM bills WHERE status = :status ORDER BY due_date ASC")
    fun getBillsByStatus(status: String): Flow<List<Bill>>

    // Get open bills
    @Query("SELECT * FROM bills WHERE status = 'open' ORDER BY due_date ASC")
    fun getOpenBills(): Flow<List<Bill>>

    // Get overdue bills (due_date < today AND status != 'paid')
    @Query("SELECT * FROM bills WHERE due_date < :currentTime AND status != 'paid' ORDER BY due_date ASC")
    fun getOverdueBills(currentTime: Long = System.currentTimeMillis()): Flow<List<Bill>>

    // Get bill for specific month/year and card
    @Query("SELECT * FROM bills WHERE credit_card_id = :creditCardId AND year = :year AND month = :month LIMIT 1")
    fun getBillForMonth(creditCardId: Long, year: Int, month: Int): Flow<Bill?>

    @Query("SELECT * FROM bills WHERE credit_card_id = :creditCardId AND year = :year AND month = :month LIMIT 1")
    suspend fun getBillForMonthSync(creditCardId: Long, year: Int, month: Int): Bill?

    // Get current/latest open bill for a card
    @Query("SELECT * FROM bills WHERE credit_card_id = :creditCardId AND status = 'open' ORDER BY year DESC, month DESC LIMIT 1")
    fun getCurrentOpenBill(creditCardId: Long): Flow<Bill?>

    @Query("SELECT * FROM bills WHERE credit_card_id = :creditCardId AND status = 'open' ORDER BY year DESC, month DESC LIMIT 1")
    suspend fun getCurrentOpenBillSync(creditCardId: Long): Bill?

    // Get bills within date range
    @Query("SELECT * FROM bills WHERE due_date >= :startDate AND due_date <= :endDate ORDER BY due_date ASC")
    fun getBillsInDateRange(startDate: Long, endDate: Long): Flow<List<Bill>>

    // Get total unpaid amount across all bills
    @Query("SELECT COALESCE(SUM(total_amount - paid_amount), 0.0) FROM bills WHERE status IN ('open', 'partial', 'overdue')")
    fun getTotalUnpaidAmount(): Flow<Double>

    // Get total unpaid amount for a specific card
    @Query("SELECT COALESCE(SUM(total_amount - paid_amount), 0.0) FROM bills WHERE credit_card_id = :creditCardId AND status IN ('open', 'partial', 'overdue')")
    fun getTotalUnpaidAmountForCard(creditCardId: Long): Flow<Double>

    // Insert/Update/Delete
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteBillById(id: Long)

    // Update bill payment status
    @Query("UPDATE bills SET paid_amount = :paidAmount, status = :status, payment_transaction_id = :paymentTransactionId WHERE id = :billId")
    suspend fun updateBillPayment(billId: Long, paidAmount: Double, status: String, paymentTransactionId: Long?)

    // Update bill total amount (used when recalculating)
    @Query("UPDATE bills SET total_amount = :totalAmount WHERE id = :billId")
    suspend fun updateBillTotal(billId: Long, totalAmount: Double)

    // Update bill status
    @Query("UPDATE bills SET status = :status WHERE id = :billId")
    suspend fun updateBillStatus(billId: Long, status: String)

    // Mark overdue bills
    @Query("UPDATE bills SET status = 'overdue' WHERE due_date < :currentTime AND status IN ('open', 'partial')")
    suspend fun markOverdueBills(currentTime: Long = System.currentTimeMillis())
}

