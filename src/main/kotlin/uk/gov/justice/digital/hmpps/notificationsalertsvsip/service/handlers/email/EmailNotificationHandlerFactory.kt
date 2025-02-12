package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType

@Component
class EmailNotificationHandlerFactory(
  private val bookedHandler: BookedEventEmailHandler,
  private val cancelledHandler: CancelledEventEmailHandler,
  private val updatedEventHandler: UpdatedEventEmailHandler,
) {

  fun getHandler(eventType: VisitEventType): BaseEmailNotificationHandler = when (eventType) {
    VisitEventType.BOOKED -> bookedHandler
    VisitEventType.CANCELLED -> cancelledHandler
    VisitEventType.UPDATED -> updatedEventHandler
  }
}
