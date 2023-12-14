package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.service.notify.NotificationClient

@Service
class SmsSenderService(
  @Value("\${notify.sms.enabled:}")
  private val enabled: Boolean,
  val notificationClient: NotificationClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendSms(templateID: String, phoneNumber: String, personalisation: Map<String, String>, reference: String) {
    if (enabled) {
      notificationClient.sendSms(templateID, phoneNumber, personalisation, reference)
    } else {
      LOG.info("Sending SMS has been disabled.")
    }
  }
}
