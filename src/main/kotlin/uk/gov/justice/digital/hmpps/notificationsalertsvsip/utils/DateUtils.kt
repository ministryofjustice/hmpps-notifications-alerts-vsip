package uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils

import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Component
class DateUtils {
  companion object {
    private const val SMS_DATE_PATTERN = "d MMMM yyyy"
    private const val SMS_TIME_PATTERN = "h:mma"
    private const val SMS_TIME_PATTERN_WHEN_MINUTES_IS_ZERO = "ha"
    private const val SMS_DAY_OF_WEEK_PATTERN = "EEEE"

    fun getFormattedDate(visitDate: LocalDate): String {
      return visitDate.format(DateTimeFormatter.ofPattern(SMS_DATE_PATTERN))
    }

    fun getFormattedTime(visitStartTime: LocalTime): String {
      val formatter = if (visitStartTime.minute == 0) {
        DateTimeFormatter.ofPattern(SMS_TIME_PATTERN_WHEN_MINUTES_IS_ZERO)
      } else {
        DateTimeFormatter.ofPattern(SMS_TIME_PATTERN)
      }

      return visitStartTime.format(formatter).lowercase()
    }

    fun getFormattedDayOfWeek(visitDate: LocalDate): String {
      return visitDate.format(DateTimeFormatter.ofPattern(SMS_DAY_OF_WEEK_PATTERN))
    }
  }
}
