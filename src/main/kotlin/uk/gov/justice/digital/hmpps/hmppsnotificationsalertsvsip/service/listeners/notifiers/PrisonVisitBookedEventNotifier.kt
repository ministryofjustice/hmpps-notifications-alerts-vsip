package uk.gov.justice.digital.hmpps.hmppsnotificationsalertsvsip.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsnotificationsalertsvsip.service.listeners.events.DomainEvent

const val PRISON_VISIT_BOOKED = "prison-visit.booked"

@Component(value = PRISON_VISIT_BOOKED)
class PrisonVisitBookedEventNotifier(
  private val objectMapper: ObjectMapper,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
  }
}
