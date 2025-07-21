package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType

@Component
class SmsNotificationHandlerFactory(
  private val bookedHandler: BookedEventSmsHandler,
  private val cancelledHandler: CancelledEventSmsHandler,
  private val updatedHandler: UpdatedEventSmsHandler,
  private val requestActionedHandler: RequestActionedEventSmsHandler,
) {

  fun getHandler(eventType: VisitEventType): BaseSmsNotificationHandler = when (eventType) {
    VisitEventType.BOOKED -> bookedHandler
    VisitEventType.CANCELLED -> cancelledHandler
    VisitEventType.UPDATED -> updatedHandler
    VisitEventType.REQUEST_ACTIONED -> requestActionedHandler
  }
}
