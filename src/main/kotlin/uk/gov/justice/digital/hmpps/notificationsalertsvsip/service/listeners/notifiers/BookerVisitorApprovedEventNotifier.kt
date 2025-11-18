package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.BookerNotificationService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorApprovedAdditionalInfo

const val BOOKER_VISITOR_APPROVED = "prison-visit-booker.visitor-approved"

@Component(value = BOOKER_VISITOR_APPROVED)
class BookerVisitorApprovedEventNotifier(
  private val bookerNotificationService: BookerNotificationService,
  private val objectMapper: ObjectMapper,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val visitorApprovedAdditionalInfo: VisitorApprovedAdditionalInfo = objectMapper.readValue(domainEvent.additionalInformation)
    LOG.debug("Enter booking event with info : {}", visitorApprovedAdditionalInfo)
    bookerNotificationService.sendMessage(BookerEventType.VISITOR_APPROVED, visitorApprovedAdditionalInfo)
  }
}
