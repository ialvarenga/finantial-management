package com.example.organizadordefinancas.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
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
    object EditCreditCardItem : Screen("edit_credit_card_item/{itemId}", "Editar Item") {
        fun createRoute(itemId: Long) = "edit_credit_card_item/$itemId"
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
    // Analytics screen
    object Analytics : Screen("analytics", "Análises", Icons.Default.Analytics)
    // Notification screens
    object PendingNotifications : Screen("pending_notifications", "Transações Pendentes", Icons.Default.Notifications)
    object NotificationSettings : Screen("notification_settings", "Configurações de Notificações", Icons.Default.Settings)

    // ==================== New Screens (Phase 5) ====================

    // Account screens
    object Accounts : Screen("accounts", "Contas", Icons.Default.AccountBalance)
    object AccountDetail : Screen("account_detail/{accountId}", "Detalhes da Conta") {
        fun createRoute(accountId: Long) = "account_detail/$accountId"
    }
    object AddEditAccount : Screen("add_edit_account?accountId={accountId}", "Conta") {
        fun createRoute(accountId: Long? = null) = if (accountId != null) "add_edit_account?accountId=$accountId" else "add_edit_account"
    }

    // Balance screens
    object BalanceDetail : Screen("balance_detail/{balanceId}", "Detalhes do Saldo") {
        fun createRoute(balanceId: Long) = "balance_detail/$balanceId"
    }
    object AddPool : Screen("add_pool/{accountId}", "Nova Caixinha") {
        fun createRoute(accountId: Long) = "add_pool/$accountId"
    }

    // Transaction screens
    object Transactions : Screen("transactions", "Transações", Icons.Default.Receipt)
    object TransactionForm : Screen("transaction_form?transactionId={transactionId}", "Transação") {
        fun createRoute(transactionId: Long? = null) = if (transactionId != null) "transaction_form?transactionId=$transactionId" else "transaction_form"
    }
    object TransactionDetail : Screen("transaction_detail/{transactionId}", "Detalhes da Transação") {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }

    // Bill screens
    object Bills : Screen("bills", "Faturas", Icons.Default.Receipt)
    object BillDetail : Screen("bill_detail/{billId}", "Detalhes da Fatura") {
        fun createRoute(billId: Long) = "bill_detail/$billId"
    }
    object BillPayment : Screen("bill_payment/{billId}", "Pagar Fatura") {
        fun createRoute(billId: Long) = "bill_payment/$billId"
    }

    // Installment screens
    object Installments : Screen("installments", "Parcelamentos", Icons.Default.Repeat)
    object InstallmentDetail : Screen("installment_detail/{parentTransactionId}", "Detalhes do Parcelamento") {
        fun createRoute(parentTransactionId: Long) = "installment_detail/$parentTransactionId"
    }

    // Transfer screen
    object Transfer : Screen("transfer?fromBalanceId={fromBalanceId}", "Transferir") {
        fun createRoute(fromBalanceId: Long? = null) = if (fromBalanceId != null) "transfer?fromBalanceId=$fromBalanceId" else "transfer"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Accounts,
    Screen.CreditCards,
    Screen.Transactions,
    Screen.Analytics
)

