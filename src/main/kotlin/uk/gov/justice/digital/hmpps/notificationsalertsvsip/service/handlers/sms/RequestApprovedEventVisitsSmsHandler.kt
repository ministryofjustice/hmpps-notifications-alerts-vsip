package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_BOOKING_OR_REQUEST_APPROVED

@Service
class RequestApprovedEventVisitsSmsHandler : BaseVisitsSmsNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleRequestApproved (sms) - Entered")

    val templateVars = getCommonTemplateVars(visit)
    templateVars["ref number"] = visit.reference

    return SendSmsNotificationDto(templateName = getRequestApprovedSmsTemplateName(visit.visitSubStatus), templateVars = templateVars)
  }

  private fun getRequestApprovedSmsTemplateName(visitSubStatus: String): String {
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
