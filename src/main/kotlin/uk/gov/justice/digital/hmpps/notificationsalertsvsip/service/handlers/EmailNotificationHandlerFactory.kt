package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.interfaces.EmailNotificationHandler

@Component
class EmailNotificationHandlerFactory(
  private val bookedHandler: BookedEventEmailHandler,
  private val cancelledHandler: CancelledEventEmailHandler,
) {

  fun getHandler(eventType: VisitEventType): EmailNotificationHandler {
    return when (eventType) {
      VisitEventType.BOOKED -> bookedHandler
      VisitEventType.CANCELLED -> cancelledHandler
      VisitEventType.UPDATED -> throw UnsupportedOperationException("No handler for UPDATED event")
    }
  }
}
