package com.organizadorfinancas.data.parser

import com.organizadorfinancas.domain.model.StatementItem
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

interface StatementParser {
    fun parse(inputStream: InputStream): Result<List<StatementItem>>
}

class CsvStatementParser @Inject constructor() : StatementParser {

    override fun parse(inputStream: InputStream): Result<List<StatementItem>> {
        return try {
            val items = mutableListOf<StatementItem>()
            val reader = inputStream.bufferedReader()
            val lines = reader.readLines()

            // Skip header if present
            val dataLines = if (lines.isNotEmpty() && isHeader(lines.first())) {
                lines.drop(1)
            } else {
                lines
            }

            for (line in dataLines) {
                if (line.isBlank()) continue

                val parsedItem = parseCsvLine(line)
                if (parsedItem != null) {
                    items.add(parsedItem)
                }
            }

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isHeader(line: String): Boolean {
        val lowerLine = line.lowercase()
        return lowerLine.contains("data") ||
               lowerLine.contains("date") ||
               lowerLine.contains("descri") ||
               lowerLine.contains("valor") ||
               lowerLine.contains("amount")
    }

    private fun parseCsvLine(line: String): StatementItem? {
        val parts = line.split(";", ",").map { it.trim().removeSurrounding("\"") }

        if (parts.size < 3) return null

        return try {
            val date = parseDate(parts[0])
            val description = parts[1]
            val amount = parseAmount(parts[2])

            StatementItem(
                date = date,
                description = description,
                amount = amount
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDate(dateStr: String): LocalDate {
        val formats = listOf(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy")
        )

        for (format in formats) {
            try {
                return LocalDate.parse(dateStr, format)
            } catch (_: Exception) {
                continue
            }
        }
        throw IllegalArgumentException("Unable to parse date: $dateStr")
    }

    private fun parseAmount(amountStr: String): BigDecimal {
        val cleaned = amountStr
            .replace("R$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()

        return BigDecimal(cleaned).abs()
    }
}

class OfxStatementParser @Inject constructor() : StatementParser {

    override fun parse(inputStream: InputStream): Result<List<StatementItem>> {
        return try {
            val items = mutableListOf<StatementItem>()
            val content = inputStream.bufferedReader().readText()

            val transactionPattern = Regex(
                "<STMTTRN>(.*?)</STMTTRN>",
                RegexOption.DOT_MATCHES_ALL
            )

            val matches = transactionPattern.findAll(content)

            for (match in matches) {
                val transaction = match.groupValues[1]
                val parsedItem = parseTransaction(transaction)
                if (parsedItem != null) {
                    items.add(parsedItem)
                }
            }

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseTransaction(transaction: String): StatementItem? {
        return try {
            val dateStr = extractTag(transaction, "DTPOSTED") ?: return null
            val amountStr = extractTag(transaction, "TRNAMT") ?: return null
            val description = extractTag(transaction, "MEMO")
                ?: extractTag(transaction, "NAME")
                ?: "Unknown"

            val date = parseOfxDate(dateStr)
            val amount = BigDecimal(amountStr).abs()

            StatementItem(
                date = date,
                description = description.trim(),
                amount = amount
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTag(content: String, tagName: String): String? {
        val pattern = Regex("<$tagName>([^<\\n]+)")
        return pattern.find(content)?.groupValues?.get(1)?.trim()
    }

    private fun parseOfxDate(dateStr: String): LocalDate {
        // OFX date format: YYYYMMDDHHMMSS or YYYYMMDD
        val dateOnly = dateStr.take(8)
        return LocalDate.parse(dateOnly, DateTimeFormatter.ofPattern("yyyyMMdd"))
    }
}

