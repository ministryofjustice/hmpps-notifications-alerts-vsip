package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_BOOKING
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_REQUEST_REJECTED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime

@Service
class RequestActionedEventEmailHandler : BaseEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleRequestActionedEvent (email) - Entered")

    val templateVars = getCommonTemplateVars(visit).toMutableMap()

    templateVars.putAll(
      mapOf(
        "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
        "end time" to getFormattedTime(visit.endTimestamp.toLocalTime()),
        "arrival time" to "45",
        "closed visit" to (visit.visitRestriction == VisitRestriction.CLOSED).toString(),
        "visitors" to getVisitors(visit),
      ),
    )

    return SendEmailNotificationDto(templateName = getRequestActionedEmailTemplateName(visit.visitSubStatus), templateVars = templateVars)
  }

  private fun getRequestActionedEmailTemplateName(visitSubStatus: String): String {
    val template = when (visitSubStatus) {
      "APPROVED", "AUTO_APPROVED" -> {
        VISIT_BOOKING
      }

      "REJECTED", "AUTO_REJECTED" -> {
        VISIT_REQUEST_REJECTED
      }

      else -> {
        LOG.error("visit request actioned for visit sub status $visitSubStatus is unsupported")
        throw ValidationException("visit request actioned for visit sub status $visitSubStatus is unsupported")
      }
    }

    return getTemplateName(template)
  }
}
