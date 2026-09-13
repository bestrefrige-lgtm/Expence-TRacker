package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import java.io.File
import java.io.FileOutputStream

object CsvExportHelper {

    fun generateCsv(transactions: List<TransactionEntity>, currencyCode: String): String {
        val sb = StringBuilder()
        sb.append("Date,Time,Type,Amount,Category,Payment Method,Merchant,Notes,Tags\n")

        for (tx in transactions) {
            val dateStr = DateUtils.formatDate(tx.date)
            val typeStr = tx.type
            val amountStr = CurrencyHelper.formatDecimal(tx.amount, currencyCode)
            val categoryStr = escapeCsv(tx.categoryName)
            val paymentMethodStr = escapeCsv(tx.paymentMethod)
            val merchantStr = escapeCsv(tx.merchant)
            val noteStr = escapeCsv(tx.note)
            val tagsStr = escapeCsv(tx.tags)

            sb.append("$dateStr,${tx.time},$typeStr,$amountStr,$categoryStr,$paymentMethodStr,$merchantStr,$noteStr,$tagsStr\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\"", "\"\"")
        if (str.contains(",") || str.contains("\n") || str.contains("\"")) {
            str = "\"$str\""
        }
        return str
    }

    fun shareCsv(context: Context, csvContent: String, fileName: String = "ExpenseTracker_Export_${System.currentTimeMillis()}.csv") {
        try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { it.write(csvContent.toByteArray()) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Data Export")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share CSV Export"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
