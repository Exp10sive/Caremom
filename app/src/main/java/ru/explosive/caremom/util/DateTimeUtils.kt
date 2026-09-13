package ru.explosive.caremom.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.*

fun calculateAgeRussian(birthdayMillis: Long): String {
    val birthDate = Calendar.getInstance().apply { timeInMillis = birthdayMillis }
    val today = Calendar.getInstance()

    if (today.before(birthDate)) return "Еще не родился"

    var years = today.get(YEAR) - birthDate.get(YEAR)
    var months = today.get(MONTH) - birthDate.get(MONTH)
    var days = today.get(DAY_OF_MONTH) - birthDate.get(DAY_OF_MONTH)

    if (days < 0) {
        months -= 1
        val prevMonth = (today.get(MONTH) - 1 + 12) % 12
        val prevYear = if (today.get(MONTH) == 0) today.get(YEAR) - 1 else today.get(YEAR)
        val tempCal = Calendar.getInstance().apply { set(prevYear, prevMonth, 1) }
        days += tempCal.getActualMaximum(DAY_OF_MONTH)
    }

    if (months < 0) {
        years -= 1
        months += 12
    }

    return when {
        years > 0 -> {
            val yearsStr = getYearString(years)
            if (months > 0) "$yearsStr ${getMonthString(months)}" else yearsStr
        }
        months > 0 -> {
            val monthsStr = getMonthString(months)
            if (days > 0) "$monthsStr ${getDayString(days)}" else monthsStr
        }
        else -> getDayString(days)
    }
}

fun formatHeaderDate(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Calendar.getInstance()
    val eventDate = Calendar.getInstance().apply { time = date }

    return when {
        isSameDay(now, eventDate) -> "Сегодня"
        isYesterday(now, eventDate) -> "Вчера"
        else -> SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(date)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(YEAR) == cal2.get(YEAR) && cal1.get(DAY_OF_YEAR) == cal2.get(DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, then: Calendar): Boolean {
    val yesterday = now.clone() as Calendar
    yesterday.add(DAY_OF_YEAR, -1)
    return isSameDay(yesterday, then)
}

private fun getYearString(years: Int): String {
    val lastDigit = years % 10
    val lastTwoDigits = years % 100
    return when {
        lastTwoDigits in 11..14 -> "$years лет"
        lastDigit == 1 -> "$years год"
        lastDigit in 2..4 -> "$years года"
        else -> "$years лет"
    }
}

private fun getMonthString(months: Int): String {
    val lastDigit = months % 10
    val lastTwoDigits = months % 100
    return when {
        lastTwoDigits in 11..14 -> "$months месяцев"
        lastDigit == 1 -> "$months месяц"
        lastDigit in 2..4 -> "$months месяца"
        else -> "$months месяцев"
    }
}

private fun getDayString(days: Int): String {
    val lastDigit = days % 10
    val lastTwoDigits = days % 100
    return when {
        lastTwoDigits in 11..14 -> "$days дней"
        lastDigit == 1 -> "$days день"
        lastDigit in 2..4 -> "$days дня"
        else -> "$days дней"
    }
}
