package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCallbackNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCreateNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto

@Service
class VisitSchedulerService(
  private val visitSchedulerClient: VisitSchedulerClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getVisit(bookingReference: String): VisitDto? {
    LOG.info("VisitSchedulerService getVisit entered, booking reference - $bookingReference")
    return visitSchedulerClient.getVisitByReference(bookingReference)
  }

  fun createNotifyNotification(notifyCreateNotificationDto: NotifyCreateNotificationDto) {
    visitSchedulerClient.createNotifyNotification(notifyCreateNotificationDto)
  }

  fun processNotifyCallback(notifyCallbackNotificationDto: NotifyCallbackNotificationDto) {
    visitSchedulerClient.processNotifyCallbackNotification(notifyCallbackNotificationDto)
  }
}
