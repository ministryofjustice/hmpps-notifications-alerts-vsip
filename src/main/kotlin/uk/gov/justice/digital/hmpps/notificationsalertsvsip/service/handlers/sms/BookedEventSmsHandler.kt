package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonRegisterService

@Component
class BookedEventSmsHandler(
  prisonRegisterService: PrisonRegisterService,
  templatesConfig: TemplatesConfig,
) : BaseSmsNotificationHandler(prisonRegisterService, templatesConfig) {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleBookedEvent (sms) - Entered")

    val templateVars = getCommonTemplateVars(visit)
    templateVars["ref number"] = visit.reference

    return SendSmsNotificationDto(
      templateName = getTemplateName(SmsTemplateNames.VISIT_BOOKING),
      templateVars = templateVars,
    )
  }
}
