package com.organizadorfinancas.domain.usecase

import com.organizadorfinancas.data.parser.CsvStatementParser
import com.organizadorfinancas.data.parser.OfxStatementParser
import com.organizadorfinancas.domain.model.CreditCardItem
import com.organizadorfinancas.domain.model.StatementItem
import com.organizadorfinancas.domain.repository.CreditCardItemRepository
import java.io.InputStream
import java.math.BigDecimal
import javax.inject.Inject

class ImportStatementUseCase @Inject constructor(
    private val csvParser: CsvStatementParser,
    private val ofxParser: OfxStatementParser,
    private val creditCardItemRepository: CreditCardItemRepository
) {

    fun parseFile(inputStream: InputStream, fileName: String): Result<List<StatementItem>> {
        val parser = when {
            fileName.endsWith(".csv", ignoreCase = true) -> csvParser
            fileName.endsWith(".ofx", ignoreCase = true) -> ofxParser
            else -> return Result.failure(IllegalArgumentException("Unsupported file format. Use CSV or OFX."))
        }

        return parser.parse(inputStream)
    }

    suspend fun importItems(
        creditCardId: Long,
        items: List<StatementItem>,
        categoryId: Long? = null
    ): Result<Int> {
        return try {
            var importedCount = 0

            for (item in items.filter { it.isSelected }) {
                val creditCardItem = CreditCardItem(
                    id = 0,
                    creditCardId = creditCardId,
                    description = item.description,
                    amount = item.amount,
                    purchaseDate = item.date,
                    installmentNumber = 1,
                    totalInstallments = 1,
                    categoryId = categoryId
                )

                creditCardItemRepository.insert(creditCardItem)
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

