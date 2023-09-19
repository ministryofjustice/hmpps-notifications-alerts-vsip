package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CHANGED

class PrisonVisitsEventsTest() : EventsIntegrationTestBase() {

  @Test
  fun `Test visit booked event is processed correctly`() {
    // Given
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson("bi-vn-wn-ml"))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendVisitBookedMessage("bi-vn-wn-ml") }
  }

  @Test
  fun `Test visit changed event is processed correctly`() {
    // Given
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson("bi-vn-wn-ml"))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendVisitChangedMessage("bi-vn-wn-ml") }
  }

  @Test
  fun `Test visit cancelled event is processed correctly`() {
    // Given
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson("bi-vn-wn-ml"))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendVisitCancelledMessage("bi-vn-wn-ml") }
  }
}
