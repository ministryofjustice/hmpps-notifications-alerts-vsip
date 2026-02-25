package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.VisitNotificationService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo

const val PRISON_VISIT_BOOKED = "prison-visit.booked"

@Component(value = PRISON_VISIT_BOOKED)
class PrisonVisitBookedEventNotifier(
  private val visitNotificationService: VisitNotificationService,
  @param:Qualifier("objectMapper")
  private val objectMapper: ObjectMapper,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val visitAdditionalInfo: VisitAdditionalInfo = objectMapper.readValue(domainEvent.additionalInformation, VisitAdditionalInfo::class.java)
    LOG.debug("Enter booking event with info : {}", visitAdditionalInfo)
    visitNotificationService.sendMessage(VisitEventType.BOOKED, visitAdditionalInfo)
  }
}
