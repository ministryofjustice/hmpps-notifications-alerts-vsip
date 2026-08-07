package uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek
import java.time.LocalDate
import java.util.Locale

class DateUtilsTest {
  private val welshLocale = Locale.forLanguageTag("cy-GB")

  @Test
  fun `formats date in English by default`() {
    assertEquals("6 August 2026", getFormattedDate(LocalDate.of(2026, 8, 6)))
  }

  @Test
  fun `formats day of week in English by default`() {
    assertEquals("Thursday", getFormattedDayOfWeek(LocalDate.of(2026, 8, 6)))
  }

  @Test
  fun `formats date with provided locale`() {
    assertEquals("6 Awst 2026", getFormattedDate(LocalDate.of(2026, 8, 6), welshLocale))
  }

  @Test
  fun `formats day of week with provided locale`() {
    assertEquals("Dydd Iau", getFormattedDayOfWeek(LocalDate.of(2026, 8, 6), welshLocale))
  }
}
