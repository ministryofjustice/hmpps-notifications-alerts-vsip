package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCreateNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email.EmailNotificationHandlerFactory
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

@Service
class EmailSenderService(
  @Value("\${notify.email.enabled:}") private val enabled: Boolean,
  private val notificationClient: NotificationClient,
  private val handlerFactory: EmailNotificationHandlerFactory,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendEmail(visit: VisitDto, visitEventType: VisitEventType, eventAuditId: String): NotifyCreateNotificationDto? {
    if (enabled) {
      val sendEmailNotificationDto = handlerFactory.getHandler(visitEventType).handle(visit)

      try {
        LOG.info("Calling notification client")
        val response = notificationClient.sendEmail(
          sendEmailNotificationDto.templateName,
          visit.visitContact.email,
          sendEmailNotificationDto.templateVars,
          eventAuditId,
        )
        LOG.info("Calling notification client finished with response ${response.notificationId}")

        return NotifyCreateNotificationDto(response)
      } catch (e: NotificationClientException) {
        LOG.error("Error sending email with exception: $e")
        return null
      }
    } else {
      LOG.info("Sending Email has been disabled.")
      return null
    }
  }
}
