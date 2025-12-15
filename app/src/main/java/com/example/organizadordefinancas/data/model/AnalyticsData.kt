package com.example.organizadordefinancas.data.model

/**
 * Represents spending total per category
 */
data class CategoryTotal(
    val category: String,
    val total: Double
)

/**
 * Represents spending total per month
 */
data class MonthlyTotal(
    val month: String,
    val total: Double
)

/**
 * Represents spending total per merchant with transaction count
 */
data class MerchantTotal(
    val description: String,
    val total: Double,
    val count: Int
)

/**
 * Period options for analytics filtering
 */
enum class AnalyticsPeriod(val months: Int, val displayName: String) {
    CURRENT_MONTH(1, "Mês atual"),
    THREE_MONTHS(3, "3 meses"),
    SIX_MONTHS(6, "6 meses"),
    ONE_YEAR(12, "1 ano")
}

