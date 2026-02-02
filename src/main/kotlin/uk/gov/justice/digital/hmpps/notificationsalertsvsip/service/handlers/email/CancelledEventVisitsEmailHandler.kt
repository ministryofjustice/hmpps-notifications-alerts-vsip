package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames

@Service
class CancelledEventVisitsEmailHandler : BaseVisitsEmailNotificationHandler() {
  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleCancelledEvent (email)- Entered for visit reference - ${visit.reference} with sub status - ${visit.visitSubStatus}")

    return SendEmailNotificationDto(
      templateName = getCancelledEmailTemplateName(visit),
      templateVars = getCommonTemplateVars(visit),
    )
  }

  private fun getCancelledEmailTemplateName(visit: VisitDto): String {
    val template = if (visit.visitSubStatus == "REJECTED" || visit.visitSubStatus == "AUTO_REJECTED") {
      EmailTemplateNames.VISIT_REQUEST_REJECTED
    } else {
      when (visit.outcomeStatus) {
        "PRISONER_CANCELLED" -> {
          EmailTemplateNames.VISIT_CANCELLED_BY_PRISONER
        }

        // Also handles NULL (NOMIS doesn't require an outcomeStatus when cancelling a visit.
        "ESTABLISHMENT_CANCELLED", "DETAILS_CHANGED_AFTER_BOOKING", "ADMINISTRATIVE_ERROR", "BATCH_CANCELLATION", "ADMINISTRATIVE_CANCELLATION", null -> {
          EmailTemplateNames.VISIT_CANCELLED_BY_PRISON
        }

        "VISITOR_CANCELLED", "BOOKER_CANCELLED" -> {
          EmailTemplateNames.VISIT_CANCELLED
        }

        else -> {
          LOG.error("visit cancellation type ${visit.outcomeStatus} is unsupported")
          throw ValidationException("visit cancellation type ${visit.outcomeStatus} is unsupported")
        }
      }
    }

    return getTemplateName(template)
  }
}
