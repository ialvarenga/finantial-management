package com.example.organizadordefinancas.data.parser

import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Data class representing a parsed statement item
 */
data class ParsedStatementItem(
    val date: LocalDate,
    val description: String,
    val amount: Double,
    val isSelected: Boolean = true
)

/**
 * Interface for statement parsers
 */
interface StatementParser {
    fun parse(inputStream: InputStream): Result<List<ParsedStatementItem>>
}

/**
 * Parser for CSV bank/credit card statements
 */
class CsvStatementParser : StatementParser {

    override fun parse(inputStream: InputStream): Result<List<ParsedStatementItem>> {
        return try {
            val items = mutableListOf<ParsedStatementItem>()
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

    private fun parseCsvLine(line: String): ParsedStatementItem? {
        val parts = line.split(";", ",").map { it.trim().removeSurrounding("\"") }

        if (parts.size < 3) return null

        return try {
            val date = parseDate(parts[0])
            val description = parts[1]
            val amount = parseAmount(parts[2])

            ParsedStatementItem(
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

    private fun parseAmount(amountStr: String): Double {
        val cleaned = amountStr
            .replace("R$", "")
            .replace("$", "")
            .replace(" ", "")
            .trim()

        // Detect format: if contains both . and , we need to figure out which is decimal
        // If only . exists -> English format (. is decimal)
        // If only , exists -> could be Brazilian format (, is decimal)
        // If both exist -> the last one is the decimal separator

        val hasComma = cleaned.contains(",")
        val hasDot = cleaned.contains(".")

        val normalized = when {
            hasComma && hasDot -> {
                // Both exist - last one is decimal separator
                val lastComma = cleaned.lastIndexOf(",")
                val lastDot = cleaned.lastIndexOf(".")
                if (lastComma > lastDot) {
                    // Brazilian format: 1.234,56
                    cleaned.replace(".", "").replace(",", ".")
                } else {
                    // English format: 1,234.56
                    cleaned.replace(",", "")
                }
            }
            hasComma && !hasDot -> {
                // Only comma - likely Brazilian decimal: 123,45
                cleaned.replace(",", ".")
            }
            else -> {
                // Only dot or no separator - English format or integer
                cleaned
            }
        }

        return kotlin.math.abs(normalized.toDouble())
    }
}

/**
 * Parser for OFX (Open Financial Exchange) statements
 */
class OfxStatementParser : StatementParser {

    override fun parse(inputStream: InputStream): Result<List<ParsedStatementItem>> {
        return try {
            val items = mutableListOf<ParsedStatementItem>()
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

    private fun parseTransaction(transaction: String): ParsedStatementItem? {
        return try {
            val dateStr = extractTag(transaction, "DTPOSTED") ?: return null
            val amountStr = extractTag(transaction, "TRNAMT") ?: return null
            val description = extractTag(transaction, "MEMO")
                ?: extractTag(transaction, "NAME")
                ?: "Unknown"

            val date = parseOfxDate(dateStr)
            val amount = kotlin.math.abs(amountStr.toDouble())

            ParsedStatementItem(
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

/**
 * Factory to get the appropriate parser based on file extension
 */
object StatementParserFactory {

    private val csvParser = CsvStatementParser()
    private val ofxParser = OfxStatementParser()

    fun getParser(fileName: String): StatementParser? {
        return when {
            fileName.endsWith(".csv", ignoreCase = true) -> csvParser
            fileName.endsWith(".ofx", ignoreCase = true) -> ofxParser
            else -> null
        }
    }

    fun getSupportedFormats(): List<String> = listOf("CSV", "OFX")
}

/**
 * Extension function to convert LocalDate to timestamp in milliseconds
 */
fun LocalDate.toTimestamp(): Long {
    return this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

