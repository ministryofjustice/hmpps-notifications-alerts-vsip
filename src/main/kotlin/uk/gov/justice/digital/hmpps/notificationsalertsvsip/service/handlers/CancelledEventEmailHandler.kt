package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers

import jakarta.validation.ValidationException
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.interfaces.EmailNotificationHandler
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.EmailSenderService.Companion.LOG
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.EmailTemplateUtils

@Component
class CancelledEventEmailHandler(
  private val templateUtils: EmailTemplateUtils,
) : EmailNotificationHandler {

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleCancelledEvent (email) - Entered")

    return SendEmailNotificationDto(
      templateName = getCancelledEmailTemplateName(visit.outcomeStatus!!),
      templateVars = templateUtils.getCommonTemplateVars(visit),
    )
  }

  private fun getCancelledEmailTemplateName(visitOutcome: String): EmailTemplateNames {
    return when (visitOutcome) {
      "PRISONER_CANCELLED" -> {
        EmailTemplateNames.VISIT_CANCELLED_BY_PRISONER
      }

      "ESTABLISHMENT_CANCELLED", "DETAILS_CHANGED_AFTER_BOOKING", "ADMINISTRATIVE_ERROR" -> {
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
  }
}
