package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCreateNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms.SmsNotificationHandlerFactory
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

@Service
class SmsSenderService(
  @Value("\${notify.sms.enabled:}") private val enabled: Boolean,
  val notificationClient: NotificationClient,
  val handlerFactory: SmsNotificationHandlerFactory,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendSms(visit: VisitDto, visitEventType: VisitEventType, eventAuditId: String): NotifyCreateNotificationDto? {
    if (enabled) {
      val sendSmsNotificationDto = handlerFactory.getHandler(visitEventType).handle(visit)

      try {
        LOG.info("Calling notification client for event - $eventAuditId")
        val response = notificationClient.sendSms(
          sendSmsNotificationDto.templateName,
          visit.visitContact.telephone,
          sendSmsNotificationDto.templateVars,
          eventAuditId,
        )
        LOG.info("Calling notification client finished with response ${response.notificationId}")

        return NotifyCreateNotificationDto(response)
      } catch (e: NotificationClientException) {
        LOG.error("Error sending email with exception: $e")
        return null
      }
    } else {
      LOG.info("Sending SMS has been disabled.")
      return null
    }
  }
}
