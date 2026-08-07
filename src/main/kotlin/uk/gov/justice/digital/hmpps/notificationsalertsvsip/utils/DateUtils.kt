package uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

@Component
class DateUtils {
  companion object {
    private const val SMS_DATE_PATTERN = "d MMMM yyyy"
    private const val SMS_TIME_PATTERN = "h:mma"
    private const val SMS_TIME_PATTERN_WHEN_MINUTES_IS_ZERO = "ha"
    private const val SMS_DAY_OF_WEEK_PATTERN = "EEEE"
    private val ENGLISH_LOCALE: Locale = Locale.UK

    fun getFormattedDate(visitDate: LocalDate, locale: Locale = ENGLISH_LOCALE): String = visitDate.format(DateTimeFormatter.ofPattern(SMS_DATE_PATTERN, locale))

    fun getFormattedTime(visitStartTime: LocalTime): String {
      val formatter = if (visitStartTime.minute == 0) {
        DateTimeFormatter.ofPattern(SMS_TIME_PATTERN_WHEN_MINUTES_IS_ZERO)
      } else {
        DateTimeFormatter.ofPattern(SMS_TIME_PATTERN)
      }

      return visitStartTime.format(formatter).lowercase()
    }

    fun getFormattedDayOfWeek(visitDate: LocalDate, locale: Locale = ENGLISH_LOCALE): String = visitDate.format(DateTimeFormatter.ofPattern(SMS_DAY_OF_WEEK_PATTERN, locale))
  }
}
