package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames

@Component
class CancelledEventSmsHandler : BaseSmsNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleCancelledEvent (sms) - Entered")

    val templateVars = getCommonTemplateVars(visit)
    templateVars["reference"] = visit.reference

    val prisonContactNumber: String? = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)?.phoneNumber
    if (!prisonContactNumber.isNullOrEmpty()) {
      templateVars["prison phone number"] = prisonContactNumber
      return SendSmsNotificationDto(templateName = getTemplateName(SmsTemplateNames.VISIT_CANCEL), templateVars = templateVars)
    } else {
      return SendSmsNotificationDto(templateName = getTemplateName(SmsTemplateNames.VISIT_CANCEL_NO_PRISON_NUMBER), templateVars = templateVars)
    }
  }
}
