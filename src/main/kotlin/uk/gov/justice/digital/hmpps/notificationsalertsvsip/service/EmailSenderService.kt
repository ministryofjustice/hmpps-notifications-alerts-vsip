package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCreateNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email.EmailNotificationHandlerFactory
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

@Service
class EmailSenderService(
  @Value("\${notify.email.enabled:}") private val enabled: Boolean,
  @Value("\${notify.email.booker.enabled:true}") private val bookerEmailEnabled: Boolean,
  @Value("\${notify.email.visits.enabled:true}") private val visitsEmailEnabled: Boolean,
  private val notificationClient: NotificationClient,
  private val handlerFactory: EmailNotificationHandlerFactory,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendVisitsEmail(visit: VisitDto, visitEventType: VisitEventType, eventAuditId: String): NotifyCreateNotificationDto? {
    if (enabled && visitsEmailEnabled) {
      val sendEmailNotificationDto = handlerFactory.getHandler(visitEventType).handle(visit)

      try {
        LOG.info("Calling notification client for event - $eventAuditId")
        val response = notificationClient.sendEmail(
          sendEmailNotificationDto.templateName,
          visit.visitContact.email,
          sendEmailNotificationDto.templateVars,
          eventAuditId,
        )
        LOG.info("Calling notification client finished with response ${response.notificationId}, for event - $eventAuditId")

        return NotifyCreateNotificationDto(response)
      } catch (e: NotificationClientException) {
        LOG.error("Error sending email with exception: $e")
        return null
      }
    } else {
      LOG.info("Sending visits email has been disabled, email enabled - $enabled, visits email enabled - $visitsEmailEnabled.")
      return null
    }
  }

  fun sendBookerEmail(bookerInfo: BookerInfoDto, contactDto: PrisonerContactRegistryContactDto, bookerEventType: BookerEventType, reference: String? = null) {
    if (enabled && bookerEmailEnabled) {
      val sendEmailNotificationDto = handlerFactory.getHandler(bookerEventType).handle(bookerInfo, contactDto)

      try {
        LOG.info("Calling notification client for booker event - $bookerEventType, booker email: ${bookerInfo.email}, contact details: $contactDto")
        val response = notificationClient.sendEmail(
          sendEmailNotificationDto.templateName,
          bookerInfo.email,
          sendEmailNotificationDto.templateVars,
          reference,
        )

        LOG.info("Calling notification client finished with response ${response.notificationId}, for booker event - $bookerEventType, booker email: ${bookerInfo.email}, contact details: $contactDto")
      } catch (e: NotificationClientException) {
        LOG.error("Error sending booker email with exception: $e")
      }
    } else {
      LOG.info("Sending booker email has been disabled, email enabled - $enabled, booker email enabled - $bookerEmailEnabled.")
    }
  }
}
