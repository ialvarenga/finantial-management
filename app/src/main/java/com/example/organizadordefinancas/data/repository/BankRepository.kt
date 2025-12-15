package com.example.organizadordefinancas.data.repository

import com.example.organizadordefinancas.data.dao.BankDao
import com.example.organizadordefinancas.data.model.Bank
import kotlinx.coroutines.flow.Flow

class BankRepository(private val bankDao: BankDao) {
    fun getAllBanks(): Flow<List<Bank>> = bankDao.getAllBanks()

    fun getBankById(id: Long): Flow<Bank?> = bankDao.getBankById(id)

    suspend fun getBankByIdSync(id: Long): Bank? = bankDao.getBankByIdSync(id)

    fun getTotalBalance(): Flow<Double?> = bankDao.getTotalBalance()

    fun getTotalSavingsBalance(): Flow<Double?> = bankDao.getTotalSavingsBalance()

    suspend fun insertBank(bank: Bank): Long = bankDao.insertBank(bank)

    suspend fun updateBank(bank: Bank) = bankDao.updateBank(bank)

    suspend fun deductFromBalance(bankId: Long, amount: Double) =
        bankDao.deductFromBalance(bankId, amount)

    suspend fun addToBalance(bankId: Long, amount: Double) =
        bankDao.addToBalance(bankId, amount)

    suspend fun deleteBank(bank: Bank) = bankDao.deleteBank(bank)

    suspend fun deleteBankById(id: Long) = bankDao.deleteBankById(id)
}

