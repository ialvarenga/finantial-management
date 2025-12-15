package com.organizadorfinancas.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class StatementItem(
    val date: LocalDate,
    val description: String,
    val amount: BigDecimal,
    val isSelected: Boolean = true
)

