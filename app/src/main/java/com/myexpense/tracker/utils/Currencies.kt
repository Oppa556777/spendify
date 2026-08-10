package com.myexpense.tracker.utils

/** A currency the user can pick during onboarding setup. */
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
)

/** Top 20 world currencies, INR first (the app's default). */
val TopCurrencies: List<CurrencyInfo> = listOf(
    CurrencyInfo("INR", "Indian Rupee", "₹"),
    CurrencyInfo("USD", "US Dollar", "$"),
    CurrencyInfo("EUR", "Euro", "€"),
    CurrencyInfo("GBP", "British Pound", "£"),
    CurrencyInfo("JPY", "Japanese Yen", "¥"),
    CurrencyInfo("CNY", "Chinese Yuan", "¥"),
    CurrencyInfo("AUD", "Australian Dollar", "A$"),
    CurrencyInfo("CAD", "Canadian Dollar", "C$"),
    CurrencyInfo("CHF", "Swiss Franc", "CHF"),
    CurrencyInfo("SGD", "Singapore Dollar", "S$"),
    CurrencyInfo("HKD", "Hong Kong Dollar", "HK$"),
    CurrencyInfo("KRW", "South Korean Won", "₩"),
    CurrencyInfo("AED", "UAE Dirham", "د.إ"),
    CurrencyInfo("SAR", "Saudi Riyal", "﷼"),
    CurrencyInfo("RUB", "Russian Ruble", "₽"),
    CurrencyInfo("BRL", "Brazilian Real", "R$"),
    CurrencyInfo("ZAR", "South African Rand", "R"),
    CurrencyInfo("TRY", "Turkish Lira", "₺"),
    CurrencyInfo("MXN", "Mexican Peso", "MX$"),
    CurrencyInfo("NZD", "New Zealand Dollar", "NZ$"),
)
