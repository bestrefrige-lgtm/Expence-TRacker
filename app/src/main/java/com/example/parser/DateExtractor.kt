package com.example.parser

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object DateExtractor {

    data class DateResult(
        val timestamp: Long,
        val isDetected: Boolean,
        val matchedDateString: String,
        val timeString: String,
        val confidence: Float
    )

    // Regex for time e.g. "10:35 AM", "8:30 PM", "10:35am", "22:15"
    private val TIME_PATTERN = Pattern.compile(
        "\\b([0-1]?[0-9]|2[0-3]):([0-5][0-9])(?:\\s*([AaPp][Mm]))?\\b"
    )

    // Patterns for dates:
    // 1) "12 September 2026", "12 Sep 2026", "5 August 2026", "5 Aug 26"
    private val DATE_TEXT_MONTH_PATTERN = Pattern.compile(
        "\\b([0-3]?[0-9])(?:st|nd|rd|th)?\\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)(?:\\s*,?\\s*|\\s+)([12][0-9]{3}|[0-9]{2})\\b",
        Pattern.CASE_INSENSITIVE
    )

    // 2) "September 12, 2026", "Sep 12 2026"
    private val DATE_MONTH_FIRST_PATTERN = Pattern.compile(
        "\\b(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+([0-3]?[0-9])(?:st|nd|rd|th)?(?:\\s*,?\\s*|\\s+)([12][0-9]{3}|[0-9]{2})\\b",
        Pattern.CASE_INSENSITIVE
    )

    // 3) "12/09/2026", "12-09-2026", "12.09.2026" (DD/MM/YYYY or DD-MM-YYYY)
    private val DATE_DMY_NUMERIC_PATTERN = Pattern.compile(
        "\\b([0-3]?[0-9])[\\/\\-.]([0-1]?[0-9])[\\/\\-.]([12][0-9]{3}|[0-9]{2})\\b"
    )

    // 4) "2026-09-12" (YYYY-MM-DD ISO)
    private val DATE_YMD_PATTERN = Pattern.compile(
        "\\b([12][0-9]{3})[\\/\\-.]([0-1]?[0-9])[\\/\\-.]([0-3]?[0-9])\\b"
    )

    /**
     * Extracts date and time from text. If no date is found, returns current timestamp with isDetected = false.
     */
    fun extractDateTime(text: String): DateResult {
        val extractedTime = extractTime(text)

        // 1. Try Date with Text Month (e.g. 12 Sep 2026, 5 August 2026)
        val textMonthMatcher = DATE_TEXT_MONTH_PATTERN.matcher(text)
        if (textMonthMatcher.find()) {
            val dayStr = textMonthMatcher.group(1) ?: "1"
            val monthStr = textMonthMatcher.group(2) ?: "Jan"
            var yearStr = textMonthMatcher.group(3) ?: "2026"
            if (yearStr.length == 2) yearStr = "20$yearStr"

            val parsedDate = parseTextMonth(dayStr, monthStr, yearStr)
            if (parsedDate != null) {
                val finalTimestamp = applyTimeToTimestamp(parsedDate.time, extractedTime)
                return DateResult(
                    timestamp = finalTimestamp,
                    isDetected = true,
                    matchedDateString = textMonthMatcher.group(0) ?: "$dayStr $monthStr $yearStr",
                    timeString = extractedTime ?: formatTime(finalTimestamp),
                    confidence = 0.98f
                )
            }
        }

        // 2. Try Month First (e.g. Sep 12, 2026)
        val monthFirstMatcher = DATE_MONTH_FIRST_PATTERN.matcher(text)
        if (monthFirstMatcher.find()) {
            val monthStr = monthFirstMatcher.group(1) ?: "Jan"
            val dayStr = monthFirstMatcher.group(2) ?: "1"
            var yearStr = monthFirstMatcher.group(3) ?: "2026"
            if (yearStr.length == 2) yearStr = "20$yearStr"

            val parsedDate = parseTextMonth(dayStr, monthStr, yearStr)
            if (parsedDate != null) {
                val finalTimestamp = applyTimeToTimestamp(parsedDate.time, extractedTime)
                return DateResult(
                    timestamp = finalTimestamp,
                    isDetected = true,
                    matchedDateString = monthFirstMatcher.group(0) ?: "$monthStr $dayStr $yearStr",
                    timeString = extractedTime ?: formatTime(finalTimestamp),
                    confidence = 0.96f
                )
            }
        }

        // 3. Try YYYY-MM-DD
        val ymdMatcher = DATE_YMD_PATTERN.matcher(text)
        if (ymdMatcher.find()) {
            val year = ymdMatcher.group(1)?.toIntOrNull() ?: 2026
            val month = ymdMatcher.group(2)?.toIntOrNull() ?: 1
            val day = ymdMatcher.group(3)?.toIntOrNull() ?: 1
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            val finalTimestamp = applyTimeToTimestamp(cal.timeInMillis, extractedTime)
            return DateResult(
                timestamp = finalTimestamp,
                isDetected = true,
                matchedDateString = ymdMatcher.group(0) ?: "$year-$month-$day",
                timeString = extractedTime ?: formatTime(finalTimestamp),
                confidence = 0.95f
            )
        }

        // 4. Try DD/MM/YYYY or DD-MM-YYYY
        val dmyMatcher = DATE_DMY_NUMERIC_PATTERN.matcher(text)
        if (dmyMatcher.find()) {
            val p1 = dmyMatcher.group(1)?.toIntOrNull() ?: 1
            val p2 = dmyMatcher.group(2)?.toIntOrNull() ?: 1
            var year = dmyMatcher.group(3)?.toIntOrNull() ?: 2026
            if (year < 100) year += 2000

            // Usually in India DD/MM/YYYY is standard format
            val day: Int
            val month: Int
            if (p1 > 12) {
                day = p1
                month = p2
            } else if (p2 > 12) {
                day = p2
                month = p1
            } else {
                day = p1
                month = p2
            }

            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month - 1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            val finalTimestamp = applyTimeToTimestamp(cal.timeInMillis, extractedTime)
            return DateResult(
                timestamp = finalTimestamp,
                isDetected = true,
                matchedDateString = dmyMatcher.group(0) ?: "$day/$month/$year",
                timeString = extractedTime ?: formatTime(finalTimestamp),
                confidence = 0.90f
            )
        }

        // Fallback: Current date & time
        val now = System.currentTimeMillis()
        val finalTimestamp = applyTimeToTimestamp(now, extractedTime)
        return DateResult(
            timestamp = finalTimestamp,
            isDetected = false,
            matchedDateString = "Today",
            timeString = extractedTime ?: formatTime(finalTimestamp),
            confidence = 0.50f
        )
    }

    /**
     * Extracts time string e.g. "10:35 AM", "8:30 PM"
     */
    fun extractTime(text: String): String? {
        val matcher = TIME_PATTERN.matcher(text)
        if (matcher.find()) {
            val hourStr = matcher.group(1) ?: return null
            val min = matcher.group(2) ?: "00"
            val ampm = matcher.group(3)
            val h = hourStr.toIntOrNull() ?: 12
            return if (ampm != null) {
                String.format(Locale.US, "%02d:%s %s", h, min, ampm.uppercase())
            } else {
                // Convert 24-hr to AM/PM
                val period = if (h >= 12) "PM" else "AM"
                val h12 = if (h % 12 == 0) 12 else h % 12
                String.format(Locale.US, "%02d:%s %s", h12, min, period)
            }
        }
        return null
    }

    private fun parseTextMonth(dayStr: String, monthStr: String, yearStr: String): Date? {
        val cleanMonth = monthStr.lowercase().take(3)
        val monthMap = mapOf(
            "jan" to 0, "feb" to 1, "mar" to 2, "apr" to 3,
            "may" to 4, "jun" to 5, "jul" to 6, "aug" to 7,
            "sep" to 8, "oct" to 9, "nov" to 10, "dec" to 11
        )
        val monthIdx = monthMap[cleanMonth] ?: return null
        val day = dayStr.toIntOrNull() ?: 1
        val year = yearStr.toIntOrNull() ?: 2026

        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthIdx)
        cal.set(Calendar.DAY_OF_MONTH, day)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun applyTimeToTimestamp(dateTimestamp: Long, timeString: String?): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateTimestamp
        if (timeString != null) {
            val matcher = TIME_PATTERN.matcher(timeString)
            if (matcher.find()) {
                var hour = matcher.group(1)?.toIntOrNull() ?: 12
                val min = matcher.group(2)?.toIntOrNull() ?: 0
                val ampm = matcher.group(3)?.uppercase()
                if (ampm != null) {
                    if (ampm == "PM" && hour < 12) hour += 12
                    if (ampm == "AM" && hour == 12) hour = 0
                }
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, min)
            }
        }
        return cal.timeInMillis
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }
}
