package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.VisitorRequestNotificationService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorRequestAdditionalInfo

const val BOOKER_VISITOR_REQUEST_REJECTED = "prison-visit-booker.visitor-request-rejected"

@Component(value = BOOKER_VISITOR_REQUEST_REJECTED)
class BookerVisitorRequestRejectedEventNotifier(
  private val visitorRequestNotificationService: VisitorRequestNotificationService,
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val visitorRequestAdditionalInfo: VisitorRequestAdditionalInfo = objectMapper.readValue(domainEvent.additionalInformation, VisitorRequestAdditionalInfo::class.java)
    LOG.info("Enter booking event with info : {}", visitorRequestAdditionalInfo)

    visitorRequestNotificationService.sendVisitorRequestRejectedEmail(visitorRequestAdditionalInfo)
  }
}
