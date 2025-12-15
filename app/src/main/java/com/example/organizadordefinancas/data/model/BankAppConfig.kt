package com.example.organizadordefinancas.data.model

/**
 * Represents a bank app configuration for notification monitoring.
 */
data class BankAppConfig(
    val packageName: String,
    val displayName: String,
    val isEnabled: Boolean = true
) {
    companion object {
        /**
         * Default list of supported Brazilian bank apps.
         */
        val DEFAULT_BANK_APPS = listOf(
            BankAppConfig("com.nu.production", "Nubank"),
            BankAppConfig("com.itau", "Itaú"),
            BankAppConfig("com.bradesco", "Bradesco"),
            BankAppConfig("br.com.bb.android", "Banco do Brasil"),
            BankAppConfig("com.santander.app", "Santander"),
            BankAppConfig("br.com.intermedium", "Inter"),
            BankAppConfig("com.c6bank.app", "C6 Bank"),
            BankAppConfig("com.picpay", "PicPay"),
            BankAppConfig("br.com.mercadopago.wallet", "Mercado Pago"),
            BankAppConfig("com.google.android.apps.walletnfcrel", "Google Wallet"),
            BankAppConfig("com.google.android.gms", "Google Pay")  // Some devices use this package
        )

        /**
         * Get display name for a package name.
         */
        fun getDisplayName(packageName: String): String {
            return DEFAULT_BANK_APPS.find { it.packageName == packageName }?.displayName
                ?: packageName.substringAfterLast(".")
        }
    }
}

