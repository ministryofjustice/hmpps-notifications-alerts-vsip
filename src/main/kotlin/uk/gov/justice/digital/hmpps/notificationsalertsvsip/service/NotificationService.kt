package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class NotificationService() {

  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendVisitBookedMessage(bookingReference: String) {
    // TODO
  }

  fun sendVisitChangedMessage(bookingReference: String) {
    // TODO
  }

  fun sendVisitCancelledMessage(bookingReference: String) {
    // TODO
  }
}
