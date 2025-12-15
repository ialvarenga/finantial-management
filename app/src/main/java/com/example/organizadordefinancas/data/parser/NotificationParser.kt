package com.example.organizadordefinancas.data.parser

import com.example.organizadordefinancas.data.model.BankAppConfig

/**
 * Result of parsing a notification.
 */
data class ParsedNotification(
    val amount: Double?,
    val merchant: String?,
    val cardLastFour: String?,
    val confidence: Float,
    val transactionType: TransactionType = TransactionType.PURCHASE
)

enum class TransactionType {
    PURCHASE,       // Regular card purchase
    PIX_SENT,       // PIX payment sent
    PIX_RECEIVED,   // PIX payment received
    TRANSFER,       // Bank transfer
    UNKNOWN
}

/**
 * Parser for extracting transaction information from bank notifications.
 */
class NotificationParser {

    companion object {
        // Supported bank package names
        val SUPPORTED_PACKAGES = BankAppConfig.DEFAULT_BANK_APPS.map { it.packageName }.toSet()

        /**
         * Check if a package is a supported bank app.
         */
        fun isSupportedPackage(packageName: String): Boolean {
            return packageName in SUPPORTED_PACKAGES
        }
    }

    /**
     * Parse notification content to extract transaction details.
     */
    fun parse(packageName: String, title: String, content: String): ParsedNotification {
        return when {
            isGoogleWallet(packageName) -> parseGoogleWallet(title, content)
            isNubank(packageName) -> parseNubank(title, content)
            isItau(packageName) -> parseItau(title, content)
            isBradesco(packageName) -> parseBradesco(title, content)
            isBancoDoBrasil(packageName) -> parseBancoDoBrasil(title, content)
            isSantander(packageName) -> parseSantander(title, content)
            isInter(packageName) -> parseInter(title, content)
            isC6Bank(packageName) -> parseC6Bank(title, content)
            isPicPay(packageName) -> parsePicPay(title, content)
            isMercadoPago(packageName) -> parseMercadoPago(title, content)
            else -> parseGeneric(title, content)
        }
    }

    // ========== Package Checks ==========

    private fun isGoogleWallet(packageName: String): Boolean =
        packageName == "com.google.android.apps.walletnfcrel" ||
                packageName == "com.google.android.gms"

    private fun isNubank(packageName: String): Boolean =
        packageName == "com.nu.production"

    private fun isItau(packageName: String): Boolean =
        packageName == "com.itau"

    private fun isBradesco(packageName: String): Boolean =
        packageName == "com.bradesco"

    private fun isBancoDoBrasil(packageName: String): Boolean =
        packageName == "br.com.bb.android"

    private fun isSantander(packageName: String): Boolean =
        packageName == "com.santander.app"

    private fun isInter(packageName: String): Boolean =
        packageName == "br.com.intermedium"

    private fun isC6Bank(packageName: String): Boolean =
        packageName == "com.c6bank.app"

    private fun isPicPay(packageName: String): Boolean =
        packageName == "com.picpay"

    private fun isMercadoPago(packageName: String): Boolean =
        packageName == "br.com.mercadopago.wallet"

    // ========== Google Wallet Parser ==========

    private fun parseGoogleWallet(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        // Google Wallet patterns:
        // "R$ 45,90 • Merchant Name"
        // "Compra de R$ 45,90 em Merchant Name com cartão •••• 1234"
        // "R$ 45,90 com cartão •••• 1234 em Merchant Name"
        // "Pagamento de R$ 50,00 - Merchant - •••• 1234"

        // Try to extract card last four digits
        val cardLastFour = extractCardLastFour(fullText)

        // Try patterns for amount and merchant
        val patterns = listOf(
            // Pattern: "R$ 45,90 • Merchant Name" or "R$45,90 - Merchant"
            Regex("""R\$\s*([\d.,]+)\s*[•\-–]\s*(.+?)(?:\s*[•\-–]\s*|$)""", RegexOption.IGNORE_CASE),
            // Pattern: "Compra de R$ 45,90 em Merchant"
            Regex("""(?:Compra|Pagamento)\s+(?:de\s+)?R\$\s*([\d.,]+)\s+em\s+(.+?)(?:\s+com\s+cartão|$)""", RegexOption.IGNORE_CASE),
            // Pattern: "R$ 45,90 com cartão •••• 1234 em Merchant"
            Regex("""R\$\s*([\d.,]+)\s+com\s+cartão\s+[•\*]+\s*\d+\s+em\s+(.+)""", RegexOption.IGNORE_CASE),
            // Pattern: Simple "R$ 45,90 Merchant"
            Regex("""R\$\s*([\d.,]+)\s+(.+?)(?:\s+•|\s+\*|$)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                val merchant = match.groupValues[2].trim()
                    .replace(Regex("""[•\*]+\s*\d{4}"""), "")  // Remove card number
                    .replace(Regex("""\s+com\s+cartão.*""", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (amount != null && merchant.isNotBlank()) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(merchant),
                        cardLastFour = cardLastFour,
                        confidence = 0.9f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        // Fallback: try to extract just the amount
        val amount = extractAmount(fullText)
        return ParsedNotification(
            amount = amount,
            merchant = extractMerchantGeneric(fullText),
            cardLastFour = cardLastFour,
            confidence = if (amount != null) 0.5f else 0.1f,
            transactionType = TransactionType.PURCHASE
        )
    }

    // ========== Nubank Parser ==========

    private fun parseNubank(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        // Nubank patterns:
        // "Compra aprovada de R$ 45,90 em MERCHANT NAME"
        // "Compra no débito de R$ 45,90 em MERCHANT"
        // "Pix enviado de R$ 100,00 para NOME"
        // "Pix recebido de R$ 50,00 de NOME"
        // "Compra de R$ 45,90 aprovada em MERCHANT"

        val purchasePatterns = listOf(
            Regex("""Compra\s+(?:aprovada|no\s+(?:débito|crédito))\s+de\s+R\$\s*([\d.,]+)\s+em\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""Compra\s+de\s+R\$\s*([\d.,]+)\s+(?:aprovada\s+)?em\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""R\$\s*([\d.,]+)\s+em\s+(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in purchasePatterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                val merchant = match.groupValues[2].trim()
                if (amount != null) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(merchant),
                        cardLastFour = null,
                        confidence = 0.95f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        // PIX patterns
        val pixSentPattern = Regex("""Pix\s+enviado\s+(?:de\s+)?R\$\s*([\d.,]+)\s+para\s+(.+)""", RegexOption.IGNORE_CASE)
        val pixReceivedPattern = Regex("""Pix\s+recebido\s+(?:de\s+)?R\$\s*([\d.,]+)\s+de\s+(.+)""", RegexOption.IGNORE_CASE)

        pixSentPattern.find(fullText)?.let { match ->
            return ParsedNotification(
                amount = parseAmount(match.groupValues[1]),
                merchant = cleanMerchantName(match.groupValues[2]),
                cardLastFour = null,
                confidence = 0.95f,
                transactionType = TransactionType.PIX_SENT
            )
        }

        pixReceivedPattern.find(fullText)?.let { match ->
            return ParsedNotification(
                amount = parseAmount(match.groupValues[1]),
                merchant = cleanMerchantName(match.groupValues[2]),
                cardLastFour = null,
                confidence = 0.95f,
                transactionType = TransactionType.PIX_RECEIVED
            )
        }

        return parseGeneric(title, content)
    }

    // ========== Itaú Parser ==========

    private fun parseItau(title: String, content: String): ParsedNotification {
        val transactionType = if (title == "Pix recebido")
            TransactionType.PIX_RECEIVED
        else
            TransactionType.PIX_SENT

        val amountRegex = """R\$\s*([\d.,]+)""".toRegex()
        val amount = amountRegex.find(content)?.groupValues?.get(1)?.let { parseAmount(it) }

        // Matches "de Nome" or "para Nome" followed by comma
        val merchantRegex = """(?:de|para)\s+(.+?),\s*CPF""".toRegex(RegexOption.IGNORE_CASE)
        val merchant = merchantRegex.find(content)?.groupValues?.get(1)?.trim()

        return ParsedNotification(
            amount = amount,
            merchant = merchant,
            cardLastFour = null,
            confidence = if (amount != null && merchant != null) 0.95f else if (amount != null) 0.9f else 0.5f,
            transactionType = transactionType
        )
    }

    // ========== Bradesco Parser ==========

    private fun parseBradesco(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        val patterns = listOf(
            Regex("""Compra\s+(?:aprovada\s+)?(?:de\s+)?R\$\s*([\d.,]+)\s+(?:em|no)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""R\$\s*([\d.,]+)\s+[-–]\s+(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                if (amount != null) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(match.groupValues[2]),
                        cardLastFour = extractCardLastFour(fullText),
                        confidence = 0.85f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        return parseGeneric(title, content)
    }

    // ========== Banco do Brasil Parser ==========

    private fun parseBancoDoBrasil(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        val patterns = listOf(
            Regex("""Compra\s+(?:de\s+)?R\$\s*([\d.,]+)\s+(?:em|no)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""Débito\s+(?:de\s+)?R\$\s*([\d.,]+)\s+(?:em|no)\s+(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                if (amount != null) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(match.groupValues[2]),
                        cardLastFour = extractCardLastFour(fullText),
                        confidence = 0.85f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        return parseGeneric(title, content)
    }

    // ========== Santander Parser ==========

    private fun parseSantander(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        val patterns = listOf(
            Regex("""Compra\s+(?:aprovada\s+)?(?:de\s+)?R\$\s*([\d.,]+)\s+(?:em|no)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""R\$\s*([\d.,]+)\s+(?:em|no)\s+(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                if (amount != null) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(match.groupValues[2]),
                        cardLastFour = extractCardLastFour(fullText),
                        confidence = 0.85f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        return parseGeneric(title, content)
    }

    // ========== Inter Parser ==========

    private fun parseInter(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        val patterns = listOf(
            Regex("""Compra\s+(?:de\s+)?R\$\s*([\d.,]+)\s+(?:em|no)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""Pagamento\s+(?:de\s+)?R\$\s*([\d.,]+)\s+(?:em|para)\s+(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                if (amount != null) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(match.groupValues[2]),
                        cardLastFour = extractCardLastFour(fullText),
                        confidence = 0.85f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        return parseGeneric(title, content)
    }

    // ========== C6 Bank Parser ==========

    private fun parseC6Bank(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        val patterns = listOf(
            Regex("""Compra\s+(?:aprovada\s+)?(?:de\s+)?R\$\s*([\d.,]+)\s+(?:em|no)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""R\$\s*([\d.,]+)\s+[-–]\s+(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                if (amount != null) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(match.groupValues[2]),
                        cardLastFour = extractCardLastFour(fullText),
                        confidence = 0.85f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        return parseGeneric(title, content)
    }

    // ========== PicPay Parser ==========

    private fun parsePicPay(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        val patterns = listOf(
            Regex("""Pagamento\s+(?:de\s+)?R\$\s*([\d.,]+)\s+(?:para|em)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""Você\s+pagou\s+R\$\s*([\d.,]+)\s+(?:para|em)\s+(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                if (amount != null) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(match.groupValues[2]),
                        cardLastFour = null,
                        confidence = 0.85f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        return parseGeneric(title, content)
    }

    // ========== Mercado Pago Parser ==========

    private fun parseMercadoPago(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        val patterns = listOf(
            Regex("""Pagamento\s+(?:de\s+)?R\$\s*([\d.,]+)\s+(?:para|em)\s+(.+)""", RegexOption.IGNORE_CASE),
            Regex("""Compra\s+(?:de\s+)?R\$\s*([\d.,]+)\s+(?:em|no)\s+(.+)""", RegexOption.IGNORE_CASE)
        )

        for (pattern in patterns) {
            val match = pattern.find(fullText)
            if (match != null) {
                val amount = parseAmount(match.groupValues[1])
                if (amount != null) {
                    return ParsedNotification(
                        amount = amount,
                        merchant = cleanMerchantName(match.groupValues[2]),
                        cardLastFour = extractCardLastFour(fullText),
                        confidence = 0.85f,
                        transactionType = TransactionType.PURCHASE
                    )
                }
            }
        }

        return parseGeneric(title, content)
    }

    // ========== Generic Parser ==========

    private fun parseGeneric(title: String, content: String): ParsedNotification {
        val fullText = "$title $content"

        val amount = extractAmount(fullText)
        val merchant = extractMerchantGeneric(fullText)
        val cardLastFour = extractCardLastFour(fullText)

        val confidence = when {
            amount != null && merchant != null -> 0.6f
            amount != null -> 0.4f
            else -> 0.1f
        }

        return ParsedNotification(
            amount = amount,
            merchant = merchant,
            cardLastFour = cardLastFour,
            confidence = confidence,
            transactionType = TransactionType.UNKNOWN
        )
    }

    // ========== Helper Functions ==========

    /**
     * Parse Brazilian currency format to Double.
     * Handles: "45,90", "1.234,56", "45.90"
     */
    private fun parseAmount(amountStr: String): Double? {
        return try {
            val cleaned = amountStr.trim()
                .replace(".", "")    // Remove thousand separator
                .replace(",", ".")   // Convert decimal separator
            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract amount from text using common patterns.
     */
    private fun extractAmount(text: String): Double? {
        // Pattern: R$ followed by amount
        val pattern = Regex("""R\$\s*([\d.,]+)""", RegexOption.IGNORE_CASE)
        val match = pattern.find(text)
        return match?.let { parseAmount(it.groupValues[1]) }
    }

    /**
     * Extract card last four digits from notification text.
     */
    private fun extractCardLastFour(text: String): String? {
        // Patterns for card last 4 digits:
        // "•••• 1234", "*** 1234", "****1234", "final 1234", "cartão 1234"
        val patterns = listOf(
            Regex("""[•\*]+\s*(\d{4})"""),
            Regex("""final\s+(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""cartão\s+(?:final\s+)?(\d{4})""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{4})(?:\s*$|\s+(?:em|no|para))""")  // Last 4 digits at end or before preposition
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    /**
     * Try to extract merchant name using generic patterns.
     */
    private fun extractMerchantGeneric(text: String): String? {
        // Try common patterns
        val patterns = listOf(
            Regex("""(?:em|no|para)\s+([A-Za-z0-9\s&]+)""", RegexOption.IGNORE_CASE),
            Regex("""[-–]\s+([A-Za-z0-9\s&]+)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val merchant = match.groupValues[1].trim()
                if (merchant.length >= 3) {
                    return cleanMerchantName(merchant)
                }
            }
        }
        return null
    }

    /**
     * Clean and normalize merchant name.
     */
    private fun cleanMerchantName(merchant: String): String {
        return merchant
            .replace(Regex("""\s+"""), " ")  // Normalize whitespace
            .replace(Regex("""[•\*]+\s*\d{4}"""), "")  // Remove card numbers
            .replace(Regex("""\s*[-–]\s*$"""), "")  // Remove trailing dashes
            .trim()
            .take(50)  // Limit length
    }
}

