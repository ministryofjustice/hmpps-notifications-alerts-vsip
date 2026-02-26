package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.BookerNotificationService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorApprovedAdditionalInfo

const val BOOKER_VISITOR_APPROVED = "prison-visit-booker.visitor-approved"

@Component(value = BOOKER_VISITOR_APPROVED)
class BookerVisitorApprovedEventNotifier(
  private val bookerNotificationService: BookerNotificationService,
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val visitorApprovedAdditionalInfo: VisitorApprovedAdditionalInfo = objectMapper.readValue(domainEvent.additionalInformation, VisitorApprovedAdditionalInfo::class.java)
    LOG.debug("Enter booking event with info : {}", visitorApprovedAdditionalInfo)
    bookerNotificationService.sendVisitorRequestApprovedEmail(BookerEventType.VISITOR_APPROVED, visitorApprovedAdditionalInfo)
  }
}
