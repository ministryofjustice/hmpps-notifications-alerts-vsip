package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType

@Component
class SmsNotificationHandlerFactory(
  private val bookedHandler: BookedEventVisitsSmsHandler,
  private val cancelledHandler: CancelledEventVisitsSmsHandler,
  private val updatedHandler: UpdatedEventVisitsSmsHandler,
  private val requestApprovedHandler: RequestApprovedEventVisitsSmsHandler,
) {

  fun getHandler(eventType: VisitEventType): BaseVisitsSmsNotificationHandler = when (eventType) {
    VisitEventType.BOOKED -> bookedHandler
    VisitEventType.CANCELLED -> cancelledHandler
    VisitEventType.UPDATED -> updatedHandler
    VisitEventType.REQUEST_APPROVED -> requestApprovedHandler
  }
}
