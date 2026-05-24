package com.example.core.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

object DateUtils {

    fun calculateNextDueDate(currentEpoch: Long, billingCycle: String, customValue: Int): Long {
        val dateValue = if (currentEpoch <= 0) System.currentTimeMillis() else currentEpoch
        val localDate = Instant.ofEpochMilli(dateValue)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        val nextLocalDate = when (billingCycle) {
            "Weekly" -> localDate.plusWeeks(1)
            "Monthly" -> localDate.plusMonths(1)
            "Yearly" -> localDate.plusYears(1)
            "Every X days" -> localDate.plusDays(customValue.coerceAtLeast(1).toLong())
            "Every X months" -> localDate.plusMonths(customValue.coerceAtLeast(1).toLong())
            else -> localDate // One-time, no advanced date
        }

        return nextLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun formatEpoch(epoch: Long, pattern: String = "dd MMM yyyy"): String {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.format(Date(epoch))
        } catch (e: Exception) {
            ""
        }
    }

    fun parseDateString(dateStr: String, pattern: String = "dd MMM yyyy"): Long? {
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    fun getLocalDateFromEpoch(epoch: Long): LocalDate {
        return Instant.ofEpochMilli(epoch)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    fun getEpochFromLocalDate(localDate: LocalDate): Long {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
