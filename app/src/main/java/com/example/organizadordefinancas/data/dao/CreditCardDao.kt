package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.CreditCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY name ASC")
    fun getAllCreditCards(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveCreditCards(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    fun getCreditCardById(id: Long): Flow<CreditCard?>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCreditCardByIdSync(id: Long): CreditCard?

    // Get cards that have auto bill generation enabled
    @Query("SELECT * FROM credit_cards WHERE auto_generate_bills = 1 AND is_active = 1")
    fun getCardsWithAutoGenerateBills(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE auto_generate_bills = 1 AND is_active = 1")
    suspend fun getCardsWithAutoGenerateBillsSync(): List<CreditCard>

    // Find card by last four digits (for notification matching)
    @Query("SELECT * FROM credit_cards WHERE last_four_digits = :lastFour AND is_active = 1 LIMIT 1")
    suspend fun findCardByLastFourDigits(lastFour: String): CreditCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(creditCard: CreditCard): Long

    @Update
    suspend fun updateCreditCard(creditCard: CreditCard)

    @Delete
    suspend fun deleteCreditCard(creditCard: CreditCard)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCreditCardById(id: Long)

    @Query("UPDATE credit_cards SET is_active = :isActive WHERE id = :id")
    suspend fun setCreditCardActive(id: Long, isActive: Boolean)
}
