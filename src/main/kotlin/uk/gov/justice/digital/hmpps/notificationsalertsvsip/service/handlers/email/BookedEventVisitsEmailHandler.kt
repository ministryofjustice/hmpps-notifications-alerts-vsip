package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_BOOKING_OR_REQUEST_APPROVED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_REQUESTED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime
import java.util.*

@Service
class BookedEventVisitsEmailHandler : BaseVisitsEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleBookedEvent (email) - Entered, visit reference: {}, visit sub status: {}", visit.reference, visit.visitSubStatus)

    return SendEmailNotificationDto(
      templateName = getTemplateName(visit),
      templateVars = getTemplateVars(visit),
    )
  }

  private fun getTemplateVars(visit: VisitDto): Map<String, Any> {
    val prison = prisonRegisterService.getPrison(visit.prisonCode)
    val templateVars: MutableMap<String, Any> = mutableMapOf(
      "ref number" to visit.reference,
      "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
      "end time" to getFormattedTime(visit.endTimestamp.toLocalTime()),
      "arrival time" to "45",
      "prison" to (prison?.prisonName ?: visit.prisonCode),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
      "main contact name" to visit.visitContact.name,
      "closed visit" to (visit.visitRestriction == VisitRestriction.CLOSED).toString(),
      "visitors" to getVisitors(visit),
    )

    when (visit.visitContact.languagePreference) {
      LanguagePreference.CY -> templateVars.putAll(
        mapOf(
          "prison_cy" to (prison?.prisonNameInWelsh ?: prison?.prisonName ?: visit.prisonCode),
          "dayofweek_cy" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate(), Locale.forLanguageTag("cy-GB")),
          "date_cy" to getFormattedDate(visit.startTimestamp.toLocalDate(), Locale.forLanguageTag("cy-GB")),
        ),
      )

      else -> Unit
    }

    templateVars.putAll(getPrisoner(visit))

    templateVars.putAll(getPrisonContactDetails(visit))

    return templateVars
  }

  private fun getTemplateName(visit: VisitDto): String = if (visit.visitSubStatus == "REQUESTED") {
    getTemplateName(VISIT_REQUESTED, languagePreference = visit.visitContact.languagePreference)
  } else {
    getTemplateName(VISIT_BOOKING_OR_REQUEST_APPROVED, languagePreference = visit.visitContact.languagePreference)
  }
}
