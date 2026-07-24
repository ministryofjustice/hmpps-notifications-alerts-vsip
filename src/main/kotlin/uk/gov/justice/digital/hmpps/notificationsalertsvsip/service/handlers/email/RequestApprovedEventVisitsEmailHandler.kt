package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_BOOKING_OR_REQUEST_APPROVED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime

@Service
class RequestApprovedEventVisitsEmailHandler : BaseVisitsEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleRequestApproved (email) - Entered")

    return SendEmailNotificationDto(templateName = getRequestApprovedEmailTemplateName(visit), templateVars = getTemplateVars(visit))
  }

  private fun getTemplateVars(visit: VisitDto): Map<String, Any> {
    val templateVars: MutableMap<String, Any> = mutableMapOf(
      "ref number" to visit.reference,
      "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
      "end time" to getFormattedTime(visit.endTimestamp.toLocalTime()),
      "arrival time" to "45",
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
      "main contact name" to visit.visitContact.name,
      "closed visit" to (visit.visitRestriction == VisitRestriction.CLOSED).toString(),
      "visitors" to getVisitors(visit),
    )
    templateVars.putAll(getPrisoner(visit))
    templateVars.putAll(getPrisonContactDetails(visit))
    when (visit.visitContact.languagePreference) {
      LanguagePreference.CY -> templateVars.putAll(emptyMap<String, Any>())
      else -> Unit
    }

    return templateVars
  }

  private fun getRequestApprovedEmailTemplateName(visit: VisitDto): String {
    val template = when (visit.visitSubStatus) {
      "APPROVED", "AUTO_APPROVED" -> {
        VISIT_BOOKING_OR_REQUEST_APPROVED
      }

      else -> {
        LOG.error("visit request approved for visit sub status ${visit.visitSubStatus} is unsupported")
        throw ValidationException("visit request approved for visit sub status ${visit.visitSubStatus} is unsupported")
      }
    }

    return getTemplateName(template, languagePreference = visit.visitContact.languagePreference)
  }
}
