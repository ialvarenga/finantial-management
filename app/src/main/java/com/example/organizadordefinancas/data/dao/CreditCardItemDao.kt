package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.CreditCardItem
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

