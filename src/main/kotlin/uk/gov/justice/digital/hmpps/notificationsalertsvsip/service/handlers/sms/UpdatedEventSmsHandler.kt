package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames

@Service
class UpdatedEventSmsHandler : BaseSmsNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleUpdatedEvent (sms) - Entered")

    val templateVars = getCommonTemplateVars(visit)
    templateVars["ref number"] = visit.reference

    return SendSmsNotificationDto(templateName = getTemplateName(SmsTemplateNames.VISIT_UPDATE), templateVars = templateVars)
  }
}
