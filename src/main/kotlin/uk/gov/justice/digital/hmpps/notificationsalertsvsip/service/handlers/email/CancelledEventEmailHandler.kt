package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames

@Service
class CancelledEventEmailHandler : BaseEmailNotificationHandler() {
  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleCancelledEvent (email)- Entered for visit reference - ${visit.reference} with sub status - ${visit.visitSubStatus}")

    val templateName = when (visit.visitSubStatus) {
      "REJECTED", "AUTO_REJECTED" -> {
        getTemplateName(EmailTemplateNames.VISIT_REQUEST_REJECTED)
      }
      else -> {
        getCancelledEmailTemplateName(visit.outcomeStatus!!)
      }
    }

    return SendEmailNotificationDto(
      templateName = templateName,
      templateVars = getCommonTemplateVars(visit),
    )
  }

  private fun getCancelledEmailTemplateName(visitOutcome: String): String {
    val template = when (visitOutcome) {
      "PRISONER_CANCELLED" -> {
        EmailTemplateNames.VISIT_CANCELLED_BY_PRISONER
      }

      "ESTABLISHMENT_CANCELLED", "DETAILS_CHANGED_AFTER_BOOKING", "ADMINISTRATIVE_ERROR", "BATCH_CANCELLATION" -> {
        EmailTemplateNames.VISIT_CANCELLED_BY_PRISON
      }

      "VISITOR_CANCELLED", "BOOKER_CANCELLED" -> {
        EmailTemplateNames.VISIT_CANCELLED
      }

      else -> {
        LOG.error("visit cancellation type $visitOutcome is unsupported")
        throw ValidationException("visit cancellation type $visitOutcome is unsupported")
      }
    }

    return getTemplateName(template)
  }
}
