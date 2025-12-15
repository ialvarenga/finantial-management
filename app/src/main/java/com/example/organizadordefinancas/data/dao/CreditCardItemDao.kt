package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.CategoryTotal
import com.example.organizadordefinancas.data.model.CreditCardItem
import com.example.organizadordefinancas.data.model.MerchantTotal
import com.example.organizadordefinancas.data.model.MonthlyTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardItemDao {
    @Query("SELECT * FROM credit_card_items WHERE cardId = :cardId ORDER BY purchaseDate DESC")
    fun getItemsByCardId(cardId: Long): Flow<List<CreditCardItem>>

    @Query("SELECT * FROM credit_card_items WHERE cardId = :cardId AND purchaseDate BETWEEN :startDate AND :endDate ORDER BY purchaseDate DESC")
    fun getItemsByCardIdAndPeriod(cardId: Long, startDate: Long, endDate: Long): Flow<List<CreditCardItem>>

    @Query("SELECT * FROM credit_card_items ORDER BY purchaseDate DESC")
    fun getAllItems(): Flow<List<CreditCardItem>>

    @Query("SELECT * FROM credit_card_items WHERE purchaseDate BETWEEN :startDate AND :endDate ORDER BY purchaseDate DESC")
    fun getAllItemsByPeriod(startDate: Long, endDate: Long): Flow<List<CreditCardItem>>

    @Query("SELECT * FROM credit_card_items WHERE id = :id")
    fun getItemById(id: Long): Flow<CreditCardItem?>


    @Query("SELECT SUM(amount) FROM credit_card_items WHERE cardId = :cardId")
    fun getTotalByCardId(cardId: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM credit_card_items WHERE cardId = :cardId AND purchaseDate BETWEEN :startDate AND :endDate")
    fun getTotalByCardIdAndPeriod(cardId: Long, startDate: Long, endDate: Long): Flow<Double?>

    // ========== Analytics Queries ==========

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM credit_card_items 
        WHERE purchaseDate >= :startDate AND purchaseDate < :endDate
        GROUP BY category ORDER BY total DESC
    """)
    fun getSpendingByCategory(startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT strftime('%Y-%m', purchaseDate/1000, 'unixepoch') as month, SUM(amount) as total
        FROM credit_card_items WHERE purchaseDate >= :startDate
        GROUP BY month ORDER BY month ASC
    """)
    fun getMonthlySpending(startDate: Long): Flow<List<MonthlyTotal>>

    @Query("""
        SELECT description, SUM(amount) as total, COUNT(*) as count
        FROM credit_card_items WHERE purchaseDate >= :startDate AND purchaseDate < :endDate
        GROUP BY description ORDER BY total DESC LIMIT :limit
    """)
    fun getTopMerchants(startDate: Long, endDate: Long, limit: Int = 10): Flow<List<MerchantTotal>>

    @Query("""
        SELECT SUM(amount) FROM credit_card_items 
        WHERE purchaseDate >= :startDate AND purchaseDate < :endDate
    """)
    fun getTotalSpending(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT MAX(amount) FROM credit_card_items 
        WHERE purchaseDate >= :startDate AND purchaseDate < :endDate
    """)
    fun getMaxSpending(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT AVG(amount) FROM credit_card_items 
        WHERE purchaseDate >= :startDate AND purchaseDate < :endDate
    """)
    fun getAverageSpending(startDate: Long, endDate: Long): Flow<Double?>

    @Query("""
        SELECT COUNT(*) FROM credit_card_items 
        WHERE purchaseDate >= :startDate AND purchaseDate < :endDate
    """)
    fun getTransactionCount(startDate: Long, endDate: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CreditCardItem): Long

    @Update
    suspend fun updateItem(item: CreditCardItem)

    @Delete
    suspend fun deleteItem(item: CreditCardItem)

    @Query("DELETE FROM credit_card_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("DELETE FROM credit_card_items WHERE cardId = :cardId")
    suspend fun deleteAllItemsByCardId(cardId: Long)
}

