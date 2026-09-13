package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppSettingsEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.TransactionEntity
import com.example.util.BackupHelper
import com.example.util.CsvExportHelper
import com.example.util.CurrencyHelper
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Expense Tracker", appName)
    }

    @Test
    fun `test currency formatting and parsing`() {
        // Test parsing
        val minor1 = CurrencyHelper.parseToMinorUnits("125.50", "INR")
        assertEquals(12550L, minor1)

        val minor2 = CurrencyHelper.parseToMinorUnits("100", "USD")
        assertEquals(10000L, minor2)

        // Test formatting
        val formattedInr = CurrencyHelper.formatMinorUnits(12550L, "INR")
        assertEquals("₹125.50", formattedInr)

        val formattedUsd = CurrencyHelper.formatMinorUnits(5000L, "USD")
        assertEquals("$50.00", formattedUsd)

        // Test input string conversion
        val inputStr = CurrencyHelper.minorUnitsToInputString(12550L, "INR")
        assertEquals("125.50", inputStr)
    }

    @Test
    fun `test date utils grouping and keys`() {
        val now = System.currentTimeMillis()
        val group = DateUtils.getGroupHeader(now)
        assertEquals("Today", group)

        val monthKey = DateUtils.getMonthKey(now)
        assertTrue(monthKey.matches(Regex("^\\d{4}-\\d{2}$")))
    }

    @Test
    fun `test backup JSON creation and validation`() {
        val testTx = TransactionEntity(
            id = UUID.randomUUID().toString(),
            type = "EXPENSE",
            amount = 45000L,
            categoryId = "cat-1",
            categoryName = "Groceries",
            categoryIcon = "shopping_cart",
            date = System.currentTimeMillis(),
            time = "10:30 AM",
            paymentMethod = "Cash",
            merchant = "Supermarket",
            note = "Weekly groceries"
        )

        val testCat = CategoryEntity(
            id = "cat-1",
            name = "Groceries",
            icon = "shopping_cart",
            color = 0xFF10B981,
            type = "EXPENSE"
        )

        val json = BackupHelper.createBackupJson(
            transactions = listOf(testTx),
            categories = listOf(testCat),
            budgets = emptyList(),
            recurring = emptyList(),
            paymentMethods = emptyList(),
            settings = listOf(AppSettingsEntity("currency", "INR"))
        )

        assertNotNull(json)
        assertTrue(json.contains("Groceries"))
        assertTrue(json.contains("Supermarket"))

        // Validate
        val validation = BackupHelper.parseAndValidateBackup(json, emptySet())
        assertTrue(validation.isValid)
        assertEquals(1, validation.expenseCount)
        assertEquals(0, validation.incomeCount)
        assertEquals(1, validation.categoryCount)
        assertEquals(0, validation.duplicateTransactionCount)

        // Check duplicate detection
        val duplicateCheck = BackupHelper.parseAndValidateBackup(json, setOf(testTx.id))
        assertEquals(1, duplicateCheck.duplicateTransactionCount)
    }

    @Test
    fun `test CSV export formatting`() {
        val testTx = TransactionEntity(
            id = "tx-123",
            type = "EXPENSE",
            amount = 12000L,
            categoryId = "cat-1",
            categoryName = "Dining",
            categoryIcon = "restaurant",
            date = System.currentTimeMillis(),
            time = "01:15 PM",
            paymentMethod = "UPI",
            merchant = "Bistro, Cafe",
            note = "Lunch with team"
        )

        val csv = CsvExportHelper.generateCsv(listOf(testTx), "INR")
        assertTrue(csv.contains("Date,Time,Type,Amount,Category,Payment Method,Merchant,Notes,Tags"))
        assertTrue(csv.contains("EXPENSE"))
        assertTrue(csv.contains("120.00"))
        assertTrue(csv.contains("Dining"))
        assertTrue(csv.contains("\"Bistro, Cafe\"")) // escaped quotes for comma
    }

    @Test
    fun `test Indian number parser`() {
        val result1 = com.example.parser.IndianNumberParser.extractAmount("Paid ₹450 to Swiggy")
        assertNotNull(result1)
        assertEquals(45000L, result1!!.amountMinor)

        val result2 = com.example.parser.IndianNumberParser.extractAmount("Payment of ₹1,25,000 to Dealer")
        assertNotNull(result2)
        assertEquals(12500000L, result2!!.amountMinor)

        val result3 = com.example.parser.IndianNumberParser.extractAmount("Amount: Rs. 1,500.50 debited")
        assertNotNull(result3)
        assertEquals(150050L, result3!!.amountMinor)
    }

    @Test
    fun `test PhonePe transaction parsing`() {
        val phonePeText = """
            Paid ₹450 to Swiggy
            12 Sep 2026, 10:35 AM
            Transaction ID: T260912103524
            UPI Ref No: 425612345678
            Payment using PhonePe
        """.trimIndent()

        val parsed = com.example.parser.TransactionParser.parse(phonePeText)
        assertNotNull(parsed)
        assertEquals(45000L, parsed!!.amount)
        assertEquals("EXPENSE", parsed.transactionType)
        assertTrue(parsed.merchant.contains("Swiggy", ignoreCase = true))
        assertEquals("T260912103524", parsed.transactionId)
        assertEquals("425612345678", parsed.utr)
        assertTrue(parsed.dateIsDetected)
        assertEquals("UPI", parsed.paymentMethod)
        assertEquals("Food & Dining", parsed.suggestedCategory)
    }

    @Test
    fun `test Google Pay transaction parsing and income detection`() {
        val gpayText = """
            Received ₹2,000 from Rahul Kumar
            11 September 2026, 04:20 PM
            UPI transaction ID: 625698741235
            Google Pay
        """.trimIndent()

        val parsed = com.example.parser.TransactionParser.parse(gpayText)
        assertNotNull(parsed)
        assertEquals(200000L, parsed!!.amount)
        assertEquals("INCOME", parsed.transactionType)
        assertTrue(parsed.merchant.contains("Rahul", ignoreCase = true))
        assertEquals("625698741235", parsed.transactionId)
        assertTrue(parsed.dateIsDetected)
    }

    @Test
    fun `test date extractor preserves older historical dates`() {
        val textWithOldDate = "Paid ₹500 to Cafe on 5 August 2026, 8:30 PM"
        val dateResult = com.example.parser.DateExtractor.extractDateTime(textWithOldDate)
        assertTrue(dateResult.isDetected)
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = dateResult.timestamp
        assertEquals(2026, cal.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.AUGUST, cal.get(java.util.Calendar.MONTH))
        assertEquals(5, cal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals("08:30 PM", dateResult.timeString)
    }

    @Test
    fun `test category suggestion keywords`() {
        val categories = listOf(
            CategoryEntity(id = "cat-food", name = "Food & Dining", icon = "restaurant", color = 0L, type = "EXPENSE"),
            CategoryEntity(id = "cat-grocery", name = "Groceries", icon = "shopping_cart", color = 0L, type = "EXPENSE"),
            CategoryEntity(id = "cat-transport", name = "Transportation", icon = "directions_car", color = 0L, type = "EXPENSE")
        )

        val foodSuggestion = com.example.parser.CategoryDetectionHelper.suggestCategory("Zomato", "Order #123", "EXPENSE", categories)
        assertEquals("Food & Dining", foodSuggestion.categoryName)
        assertEquals("cat-food", foodSuggestion.categoryId)

        val grocerySuggestion = com.example.parser.CategoryDetectionHelper.suggestCategory("Blinkit", "Instant grocery", "EXPENSE", categories)
        assertEquals("Groceries", grocerySuggestion.categoryName)
        assertEquals("cat-grocery", grocerySuggestion.categoryId)

        val rideSuggestion = com.example.parser.CategoryDetectionHelper.suggestCategory("Uber", "Trip receipt", "EXPENSE", categories)
        assertEquals("Transportation", rideSuggestion.categoryName)
        assertEquals("cat-transport", rideSuggestion.categoryId)
    }
}
