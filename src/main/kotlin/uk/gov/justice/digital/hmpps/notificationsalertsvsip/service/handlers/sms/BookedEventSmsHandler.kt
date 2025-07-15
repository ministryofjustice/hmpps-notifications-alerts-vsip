package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_BOOKING
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_REQUESTED

@Service
class BookedEventSmsHandler : BaseSmsNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleBookedEvent (sms) - Entered, visit reference: {}, visit sub status: {}", visit.reference, visit.visitSubStatus)

    val templateVars = getCommonTemplateVars(visit)
    templateVars["ref number"] = visit.reference

    return SendSmsNotificationDto(
      templateName = getTemplateName(visit),
      templateVars = templateVars,
    )
  }

  private fun getTemplateName(visit: VisitDto): String = if (visit.visitSubStatus == "REQUESTED") {
    getTemplateName(VISIT_REQUESTED)
  } else {
    getTemplateName(VISIT_BOOKING)
  }
}
