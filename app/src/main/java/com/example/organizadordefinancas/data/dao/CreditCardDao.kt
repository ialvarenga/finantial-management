package com.example.organizadordefinancas.data.dao

import androidx.room.*
import com.example.organizadordefinancas.data.model.CreditCard
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY name ASC")
    fun getAllCreditCards(): Flow<List<CreditCard>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    fun getCreditCardById(id: Long): Flow<CreditCard?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditCard(creditCard: CreditCard): Long

    @Update
    suspend fun updateCreditCard(creditCard: CreditCard)

    @Delete
    suspend fun deleteCreditCard(creditCard: CreditCard)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCreditCardById(id: Long)
}
