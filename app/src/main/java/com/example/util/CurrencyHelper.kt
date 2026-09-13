package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val name: String,
    val minorUnitFactor: Int = 100
)

object CurrencyHelper {
    val supportedCurrencies = listOf(
        CurrencyInfo("INR", "₹", "Indian Rupee (₹)"),
        CurrencyInfo("USD", "$", "US Dollar ($)"),
        CurrencyInfo("EUR", "€", "Euro (€)"),
        CurrencyInfo("GBP", "£", "British Pound (£)"),
        CurrencyInfo("AED", "AED ", "UAE Dirham (AED)"),
        CurrencyInfo("CAD", "CA$", "Canadian Dollar (CA$)"),
        CurrencyInfo("AUD", "AU$", "Australian Dollar (AU$)"),
        CurrencyInfo("SGD", "SG$", "Singapore Dollar (SG$)"),
        CurrencyInfo("JPY", "¥", "Japanese Yen (¥)", minorUnitFactor = 1)
    )

    fun getCurrencyInfo(code: String): CurrencyInfo {
        return supportedCurrencies.find { it.code.equals(code, ignoreCase = true) }
            ?: supportedCurrencies[0]
    }

    /**
     * Formats minor unit value (e.g. 125050 paise) into formatted string like "₹1,250.50"
     */
    fun formatMinorUnits(amountMinor: Long, currencyCode: String = "INR", showSign: Boolean = false): String {
        val info = getCurrencyInfo(currencyCode)
        val isNegative = amountMinor < 0
        val absAmount = Math.abs(amountMinor)

        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val formattedNumber = if (info.minorUnitFactor == 1) {
            val formatter = DecimalFormat("#,##0", symbols)
            formatter.format(absAmount)
        } else {
            val formatter = DecimalFormat("#,##0.00", symbols)
            val majorValue = absAmount.toDouble() / info.minorUnitFactor
            formatter.format(majorValue)
        }

        val prefix = if (showSign) {
            if (isNegative) "- " else "+ "
        } else {
            if (isNegative) "-" else ""
        }

        return "$prefix${info.symbol}$formattedNumber"
    }

    /**
     * Converts major string input e.g. "125.50" into minor units long (12550)
     */
    fun parseToMinorUnits(input: String, currencyCode: String = "INR"): Long {
        val info = getCurrencyInfo(currencyCode)
        val clean = input.trim().replace(",", "")
        val doubleVal = clean.toDoubleOrNull() ?: 0.0
        return Math.round(doubleVal * info.minorUnitFactor)
    }

    /**
     * Formats to standard decimal string with 2 decimal places e.g. 12000 -> "120.00"
     */
    fun formatDecimal(amountMinor: Long, currencyCode: String = "INR"): String {
        val info = getCurrencyInfo(currencyCode)
        return if (info.minorUnitFactor == 1) {
            amountMinor.toString()
        } else {
            val major = amountMinor.toDouble() / info.minorUnitFactor
            String.format(Locale.US, "%.2f", major)
        }
    }

    /**
     * Converts minor unit to input string e.g. 12550 -> "125.50"
     */
    fun minorUnitsToInputString(amountMinor: Long, currencyCode: String = "INR"): String {
        val info = getCurrencyInfo(currencyCode)
        return if (info.minorUnitFactor == 1) {
            amountMinor.toString()
        } else {
            val major = amountMinor.toDouble() / info.minorUnitFactor
            if (major % 1.0 == 0.0) {
                major.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", major)
            }
        }
    }
}
