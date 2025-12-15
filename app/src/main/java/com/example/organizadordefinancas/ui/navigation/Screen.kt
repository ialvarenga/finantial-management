package com.example.organizadordefinancas.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Início", Icons.Default.Home)
    object CreditCards : Screen("credit_cards", "Cartões", Icons.Default.CreditCard)
    object CreditCardDetail : Screen("credit_card_detail/{cardId}", "Detalhes do Cartão") {
        fun createRoute(cardId: Long) = "credit_card_detail/$cardId"
    }
    object AddEditCreditCard : Screen("add_edit_credit_card?cardId={cardId}", "Cartão") {
        fun createRoute(cardId: Long? = null) = if (cardId != null) "add_edit_credit_card?cardId=$cardId" else "add_edit_credit_card"
    }
    object AddCreditCardItem : Screen("add_credit_card_item/{cardId}", "Adicionar Item") {
        fun createRoute(cardId: Long) = "add_credit_card_item/$cardId"
    }
    object ImportStatement : Screen("import_statement/{cardId}", "Importar Extrato") {
        fun createRoute(cardId: Long) = "import_statement/$cardId"
    }
    object Banks : Screen("banks", "Bancos", Icons.Default.AccountBalance)
    object AddEditBank : Screen("add_edit_bank?bankId={bankId}", "Banco") {
        fun createRoute(bankId: Long? = null) = if (bankId != null) "add_edit_bank?bankId=$bankId" else "add_edit_bank"
    }
    object Compromises : Screen("compromises", "Contas", Icons.Default.Receipt)
    object AddEditCompromise : Screen("add_edit_compromise?compromiseId={compromiseId}", "Conta Fixa") {
        fun createRoute(compromiseId: Long? = null) = if (compromiseId != null) "add_edit_compromise?compromiseId=$compromiseId" else "add_edit_compromise"
    }
    object Incomes : Screen("incomes", "Receitas", Icons.Default.AttachMoney)
    object AddEditIncome : Screen("add_edit_income?incomeId={incomeId}", "Receita") {
        fun createRoute(incomeId: Long? = null) = if (incomeId != null) "add_edit_income?incomeId=$incomeId" else "add_edit_income"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.CreditCards,
    Screen.Banks,
    Screen.Compromises,
    Screen.Incomes
)

