package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_BOOKING
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_REQUEST_REJECTED

@Service
class RequestActionedEventSmsHandler : BaseSmsNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleRequestActioned (sms) - Entered")

    val templateVars = getCommonTemplateVars(visit)
    templateVars["ref number"] = visit.reference

    return SendSmsNotificationDto(templateName = getRequestActionedSmsTemplateName(visit.visitSubStatus), templateVars = templateVars)
  }

  private fun getRequestActionedSmsTemplateName(visitSubStatus: String): String {
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
