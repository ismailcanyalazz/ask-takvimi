package com.asktakvimi.app.ui.components

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Tüm tarih yazıları Türkçe gösterilir. */
private val TR: Locale = Locale.forLanguageTag("tr")

private val LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", TR)
private val SHORT_DATE = DateTimeFormatter.ofPattern("d MMM", TR)
private val DAY_AND_MONTH = DateTimeFormatter.ofPattern("d MMMM", TR)
private val MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", TR)

/** Örn. 12 Ekim 2026 */
fun formatLongDate(date: LocalDate): String = date.format(LONG_DATE)

/** Örn. 12 Eki */
fun formatShortDate(date: LocalDate): String = date.format(SHORT_DATE)

/** Örn. 12 Ekim */
fun formatDayMonth(date: LocalDate): String = date.format(DAY_AND_MONTH)

/** Örn. Ekim 2026 */
fun formatMonthTitle(month: YearMonth): String = month.atDay(1).format(MONTH_TITLE)
    .replaceFirstChar { it.titlecase(TR) }

/** Örn. Pazartesi */
fun dayOfWeekName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, TR).replaceFirstChar { it.titlecase(TR) }

/** Takvim başlığı için kısa gün adları: Pzt, Sal, ... */
fun shortDayNames(): List<String> = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

/**
 * "3 gün kaldı", "bugün", "2 gün gecikti" gibi insan diline yakın anlatım.
 */
fun daysPhrase(days: Int): String = when {
    days == 0 -> "bugün"
    days == 1 -> "yarın"
    days > 1 -> "$days gün sonra"
    days == -1 -> "1 gün gecikmiş görünüyor"
    else -> "${-days} gün gecikmiş görünüyor"
}
