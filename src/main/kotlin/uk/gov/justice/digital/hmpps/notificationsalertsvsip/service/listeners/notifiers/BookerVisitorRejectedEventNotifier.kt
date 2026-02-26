package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.BookerRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.BookerNotificationService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorRejectedAdditionalInfo

const val BOOKER_VISITOR_REJECTED = "prison-visit-booker.visitor-rejected"

@Component(value = BOOKER_VISITOR_REJECTED)
class BookerVisitorRejectedEventNotifier(
  private val bookerNotificationService: BookerNotificationService,
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
  private val bookerRegistryClient: BookerRegistryClient,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val visitorRejectedAdditionalInfo: VisitorRejectedAdditionalInfo = objectMapper.readValue(domainEvent.additionalInformation, VisitorRejectedAdditionalInfo::class.java)
    LOG.info("Enter booking event with info : $visitorRejectedAdditionalInfo")

    val visitorRequest = bookerRegistryClient.getVisitorRequestByReference(visitorRejectedAdditionalInfo.requestReference)

    val eventType: BookerEventType = when (visitorRequest.rejectionReason) {
      "REJECT" -> BookerEventType.VISITOR_REJECTED
      "ALREADY_LINKED" -> BookerEventType.VISITOR_REJECTED_ALREADY_LINKED
      else -> throw IllegalStateException("Unexpected rejection reason ${visitorRequest.rejectionReason}")
    }

    bookerNotificationService.sendVisitorRequestRejectedEmail(eventType, visitorRequest)
  }
}
