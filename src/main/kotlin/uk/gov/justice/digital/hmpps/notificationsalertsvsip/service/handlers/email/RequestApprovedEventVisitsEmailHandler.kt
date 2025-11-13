package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_BOOKING_OR_REQUEST_APPROVED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime

@Service
class RequestApprovedEventVisitsEmailHandler : BaseVisitsEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleRequestApproved (email) - Entered")

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

    return SendEmailNotificationDto(templateName = getRequestApprovedEmailTemplateName(visit.visitSubStatus), templateVars = templateVars)
  }

  private fun getRequestApprovedEmailTemplateName(visitSubStatus: String): String {
    val template = when (visitSubStatus) {
      "APPROVED", "AUTO_APPROVED" -> {
        VISIT_BOOKING_OR_REQUEST_APPROVED
      }

      else -> {
        LOG.error("visit request approved for visit sub status $visitSubStatus is unsupported")
        throw ValidationException("visit request approved for visit sub status $visitSubStatus is unsupported")
      }
    }

    return getTemplateName(template)
  }
}
