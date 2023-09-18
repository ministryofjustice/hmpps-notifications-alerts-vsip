package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilAsserted
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CHANGED
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

class EventsSqsTest : EventsIntegrationTestBase() {

  @Test
  fun `test PRISON_VISIT_CHANGED is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest(PRISON_VISIT_CHANGED)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsClient.countMessagesOnQueue(queueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
  }

  @Test
  fun `test PRISON_VISIT_CANCELLED is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest(PRISON_VISIT_CANCELLED)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsClient.countMessagesOnQueue(queueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
  }

  @Test
  fun `test PRISON_VISIT_BOOKED is processed`() {
    // Given
    val publishRequest = createDomainEventPublishRequest(PRISON_VISIT_BOOKED)

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilCallTo { sqsClient.countMessagesOnQueue(queueUrl).get() } matches { it == 0 }
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
  }

  @Test
  fun `test event switch set to false stops processing`() {
    // Given
    val publishRequest = createDomainEventPublishRequest("prison-visit.cancelled.test")

    // When
    awsSnsClient.publish(publishRequest).get()

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, never()).processEvent(any()) }
    await untilAsserted { Assertions.assertThat(eventFeatureSwitch.isEnabled("prison-visit.cancelled.test")).isFalse }
  }
}
