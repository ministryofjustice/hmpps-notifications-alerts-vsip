package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType

@Component
class EmailNotificationHandlerFactory(
  private val bookedHandler: BookedEventVisitsEmailHandler,
  private val cancelledHandler: CancelledEventVisitsEmailHandler,
  private val updatedEventHandler: UpdatedEventVisitsEmailHandler,
  private val requestApprovedEventHandler: RequestApprovedEventVisitsEmailHandler,
  private val visitorApprovedEventHandler: VisitorApprovedEventBookerEmailHandler,
) {

  fun getHandler(eventType: VisitEventType): BaseVisitsEmailNotificationHandler = when (eventType) {
    VisitEventType.BOOKED -> bookedHandler
    VisitEventType.CANCELLED -> cancelledHandler
    VisitEventType.UPDATED -> updatedEventHandler
    VisitEventType.REQUEST_APPROVED -> requestApprovedEventHandler
  }

  fun getHandler(eventType: BookerEventType): BaseBookerEmailNotificationHandler = when (eventType) {
    BookerEventType.VISITOR_APPROVED -> visitorApprovedEventHandler
  }
}
