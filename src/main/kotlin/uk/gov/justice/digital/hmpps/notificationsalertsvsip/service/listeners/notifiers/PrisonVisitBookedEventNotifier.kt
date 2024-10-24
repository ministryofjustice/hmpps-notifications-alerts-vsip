package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.NotificationService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo

const val PRISON_VISIT_BOOKED = "prison-visit.booked"

@Component(value = PRISON_VISIT_BOOKED)
class PrisonVisitBookedEventNotifier(
  private val notificationService: NotificationService,
  private val objectMapper: ObjectMapper,
) : EventNotifier(objectMapper) {
  override fun processEvent(domainEvent: DomainEvent) {
    val visitAdditionalInfo: VisitAdditionalInfo = objectMapper.readValue(domainEvent.additionalInformation)
    LOG.debug("Enter booking event with info : {}", visitAdditionalInfo)
    notificationService.sendMessage(VisitEventType.BOOKED, visitAdditionalInfo.bookingReference)
  }
}
