package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.VisitorRequestNotificationService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorLinkedAdditionalInfo

const val BOOKER_VISITOR_LINKED = "prison-visit-booker.visitor-linked"

@Component(value = BOOKER_VISITOR_LINKED)
class BookerVisitorLinkedEventNotifier(
  private val visitorRequestNotificationService: VisitorRequestNotificationService,
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val visitorLinkedAdditionalInfo: VisitorLinkedAdditionalInfo = objectMapper.readValue(domainEvent.additionalInformation, VisitorLinkedAdditionalInfo::class.java)
    LOG.info("Enter BookerVisitorLinkedEventNotifier event with info : {}", visitorLinkedAdditionalInfo)

    visitorRequestNotificationService.sendVisitorLinkedEmail(visitorLinkedAdditionalInfo)
  }
}
