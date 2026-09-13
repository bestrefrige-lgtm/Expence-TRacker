package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val monthYearFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val monthKeyFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatDate(timeMillis: Long): String {
        return dateFormatter.format(Date(timeMillis))
    }

    fun formatTime(timeMillis: Long): String {
        return timeFormatter.format(Date(timeMillis))
    }

    fun formatIsoDate(timeMillis: Long): String {
        return isoDateFormatter.format(Date(timeMillis))
    }

    fun formatMonthYear(timeMillis: Long): String {
        return monthYearFormatter.format(Date(timeMillis))
    }

    fun getMonthKey(timeMillis: Long): String {
        return monthKeyFormatter.format(Date(timeMillis))
    }

    fun getMonthKeyFromCalendar(calendar: Calendar): String {
        return monthKeyFormatter.format(calendar.time)
    }

    fun formatMonthKeyForDisplay(monthKey: String): String {
        return try {
            val date = monthKeyFormatter.parse(monthKey) ?: Date()
            monthYearFormatter.format(date)
        } catch (e: Exception) {
            monthKey
        }
    }

    fun getTodayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getTodayEndMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getYesterdayStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getThisWeekStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getThisMonthStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getThisMonthEndMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun getThisYearStartMillis(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getMonthStartAndEndMillis(monthKey: String): Pair<Long, Long> {
        return try {
            val date = monthKeyFormatter.parse(monthKey) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            Pair(start, end)
        } catch (e: Exception) {
            Pair(getThisMonthStartMillis(), getThisMonthEndMillis())
        }
    }

    fun getGroupHeader(timeMillis: Long): String {
        val todayStart = getTodayStartMillis()
        val yesterdayStart = getYesterdayStartMillis()
        val thisWeekStart = getThisWeekStartMillis()

        return when {
            timeMillis >= todayStart -> "Today"
            timeMillis >= yesterdayStart -> "Yesterday"
            timeMillis >= thisWeekStart -> "This Week"
            else -> "Earlier"
        }
    }
}
