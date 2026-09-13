package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.TransactionEntity
import java.io.File
import java.io.FileOutputStream

object PdfExportHelper {

    fun generateAndSharePdf(
        context: Context,
        dateRangeLabel: String,
        totalIncomeMinor: Long,
        totalExpenseMinor: Long,
        currencyCode: String,
        categoryBreakdown: List<Pair<String, Long>>,
        transactions: List<TransactionEntity>,
        fileName: String = "Expense_Report_${System.currentTimeMillis()}.pdf"
    ) {
        val document = PdfDocument()
        val pageWidth = 595 // Standard A4 width at 72 dpi
        val pageHeight = 842 // Standard A4 height at 72 dpi

        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        var y = 40f

        // Draw Header
        paint.color = Color.rgb(30, 41, 59) // Slate 800
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 90f, paint)

        textPaint.color = Color.WHITE
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 22f
        canvas.drawText("Expense Tracker Financial Report", 40f, 45f, textPaint)

        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.textSize = 12f
        textPaint.color = Color.rgb(203, 213, 225)
        canvas.drawText("Period: $dateRangeLabel | Generated: ${DateUtils.formatDate(System.currentTimeMillis())}", 40f, 70f, textPaint)

        y = 115f

        // Summary Boxes
        val boxWidth = (pageWidth - 80f - 20f) / 3f
        val boxHeight = 65f

        // Income box
        paint.color = Color.rgb(236, 253, 245) // Emerald 50
        canvas.drawRoundRect(40f, y, 40f + boxWidth, y + boxHeight, 8f, 8f, paint)
        textPaint.color = Color.rgb(5, 150, 105) // Emerald 600
        textPaint.textSize = 10f
        canvas.drawText("TOTAL INCOME", 50f, y + 22f, textPaint)
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(CurrencyHelper.formatMinorUnits(totalIncomeMinor, currencyCode), 50f, y + 48f, textPaint)

        // Expense box
        val expenseBoxX = 40f + boxWidth + 10f
        paint.color = Color.rgb(254, 242, 242) // Red 50
        canvas.drawRoundRect(expenseBoxX, y, expenseBoxX + boxWidth, y + boxHeight, 8f, 8f, paint)
        textPaint.color = Color.rgb(220, 38, 38) // Red 600
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("TOTAL EXPENSES", expenseBoxX + 10f, y + 22f, textPaint)
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(CurrencyHelper.formatMinorUnits(totalExpenseMinor, currencyCode), expenseBoxX + 10f, y + 48f, textPaint)

        // Balance box
        val balanceBoxX = expenseBoxX + boxWidth + 10f
        val netBalance = totalIncomeMinor - totalExpenseMinor
        paint.color = Color.rgb(241, 245, 249) // Slate 100
        canvas.drawRoundRect(balanceBoxX, y, balanceBoxX + boxWidth, y + boxHeight, 8f, 8f, paint)
        textPaint.color = Color.rgb(71, 85, 105) // Slate 600
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("NET BALANCE", balanceBoxX + 10f, y + 22f, textPaint)
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = if (netBalance >= 0) Color.rgb(5, 150, 105) else Color.rgb(220, 38, 38)
        canvas.drawText(CurrencyHelper.formatMinorUnits(netBalance, currencyCode), balanceBoxX + 10f, y + 48f, textPaint)

        y += boxHeight + 30f

        // Top Category Breakdown Section
        if (categoryBreakdown.isNotEmpty()) {
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textSize = 14f
            textPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText("Spending by Category", 40f, y, textPaint)
            y += 18f

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 10f
            textPaint.color = Color.rgb(51, 65, 85)

            var catCount = 0
            for ((catName, amount) in categoryBreakdown.take(6)) {
                val percentage = if (totalExpenseMinor > 0) (amount.toDouble() / totalExpenseMinor * 100).toInt() else 0
                val text = "$catName: ${CurrencyHelper.formatMinorUnits(amount, currencyCode)} ($percentage%)"
                canvas.drawText(text, 50f + (catCount % 2) * 250f, y + (catCount / 2) * 16f, textPaint)
                catCount++
            }
            y += ((catCount + 1) / 2) * 16f + 25f
        }

        // Transactions Table Header
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 14f
        textPaint.color = Color.rgb(15, 23, 42)
        canvas.drawText("Transaction Records (${transactions.size})", 40f, y, textPaint)
        y += 18f

        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(40f, y, pageWidth - 40f, y + 24f, paint)

        textPaint.textSize = 10f
        textPaint.color = Color.rgb(71, 85, 105)
        canvas.drawText("DATE", 50f, y + 16f, textPaint)
        canvas.drawText("CATEGORY / MERCHANT", 140f, y + 16f, textPaint)
        canvas.drawText("PAYMENT", 340f, y + 16f, textPaint)
        canvas.drawText("AMOUNT", pageWidth - 110f, y + 16f, textPaint)
        y += 26f

        // Table Rows
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        for ((idx, tx) in transactions.withIndex()) {
            if (y > pageHeight - 50f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = 40f

                // Re-draw table header on new page
                paint.color = Color.rgb(241, 245, 249)
                canvas.drawRect(40f, y, pageWidth - 40f, y + 24f, paint)
                textPaint.textSize = 10f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.color = Color.rgb(71, 85, 105)
                canvas.drawText("DATE", 50f, y + 16f, textPaint)
                canvas.drawText("CATEGORY / MERCHANT", 140f, y + 16f, textPaint)
                canvas.drawText("PAYMENT", 340f, y + 16f, textPaint)
                canvas.drawText("AMOUNT", pageWidth - 110f, y + 16f, textPaint)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                y += 26f
            }

            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(40f, y - 4f, pageWidth - 40f, y + 18f, paint)
            }

            textPaint.color = Color.rgb(51, 65, 85)
            textPaint.textSize = 9.5f
            canvas.drawText(DateUtils.formatDate(tx.date), 50f, y + 10f, textPaint)

            val desc = if (tx.merchant.isNotBlank()) "${tx.categoryName} (${tx.merchant})" else tx.categoryName
            val truncatedDesc = if (desc.length > 32) desc.substring(0, 29) + "..." else desc
            canvas.drawText(truncatedDesc, 140f, y + 10f, textPaint)

            canvas.drawText(tx.paymentMethod, 340f, y + 10f, textPaint)

            val amountStr = CurrencyHelper.formatMinorUnits(tx.amount, currencyCode, showSign = true)
            textPaint.color = if (tx.type == "EXPENSE") Color.rgb(220, 38, 38) else Color.rgb(5, 150, 105)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(amountStr, pageWidth - 110f, y + 10f, textPaint)
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            y += 22f
        }

        document.finishPage(page)

        try {
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Tracker Report - $dateRangeLabel")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Report"))
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
        }
    }
}
