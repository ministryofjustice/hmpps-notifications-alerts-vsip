package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonContactDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.NotificationService.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CHANGED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "notify.template-id.visit-booking=1234-5678-9012",
    "notify.template-id.visit-update=5678-9012-3456",
    "notify.template-id.visit-cancel=7890-1234-5678",
    "notify.template-id.visit-cancel-no-prison-number=9012-3456-7890",
  ],
)
class PrisonVisitsEventsTest : EventsIntegrationTestBase() {

  lateinit var visit: VisitDto
  lateinit var visit2: VisitDto
  lateinit var prison: PrisonDto
  lateinit var prisonContactDetailsDto: PrisonContactDetailsDto

  @BeforeEach
  internal fun setUp() {
    visit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.of(2023, 11, 30),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      contactName = "John Smith",
      telephoneNumber = "01234567890",
    )

    visit2 = createVisitDto(
      bookingReference = "aa-xx-wn-ml",
      visitDate = LocalDate.of(2023, 11, 30),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      contactName = "John Smith",
      telephoneNumber = "01234567890",
    )

    prison = PrisonDto("HEI", "Hewell", true)

    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333")
  }

  @Test
  fun `when visit booked message is received then booking message is sent`() {
    // Given
    val bookingReference = visit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, "bi-vn-wn-ml") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("1234-5678-9012"),
        eq(visit.visitContact!!.telephone),
        check {
          assertSmsDetailsBookOrUpdate(prisonName = prison.prisonName, time = "10:30 AM", dayOfWeek = "Thursday", date = "30 November 2023", bookingReference = bookingReference, parameters = it)
        },
      )
    }
  }

  @Test
  fun `when visit booked message is received but the visit could not be found then booking message is not sent`() {
    // Given
    val bookingReference = visit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, "bi-vn-wn-ml") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  @Test
  fun `when visit updated message is received then update message is sent`() {
    // Given
    val bookingReference = visit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "bi-vn-wn-ml") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("5678-9012-3456"),
        eq(visit.visitContact!!.telephone),
        check {
          assertSmsDetailsBookOrUpdate(prisonName = prison.prisonName, time = "10:30 AM", dayOfWeek = "Thursday", date = "30 November 2023", bookingReference = bookingReference, parameters = it)
        },
      )
    }
  }

  @Test
  fun `when visit updated message is received but the visit could not be found then update message is not sent`() {
    // Given
    val bookingReference = visit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "bi-vn-wn-ml") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  @Test
  fun `when visit cancelled message is received then cancel message is sent`() {
    // Given
    val bookingReference = visit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson("bi-vn-wn-ml"))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("7890-1234-5678"),
        eq(visit.visitContact!!.telephone),
        check {
          assertSmsDetailsCancel(prisonName = prison.prisonName, time = "10:30 AM", date = "30 November 2023", bookingReference = bookingReference, prisonPhoneNumber = prisonContactDetailsDto.phoneNumber, it)
        },
      )
    }
  }

  @Test
  fun `when visit cancelled message is received but no prison contact number then cancel message is sent`() {
    // Given
    val bookingReference = visit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson("bi-vn-wn-ml"))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, null, HttpStatus.NOT_FOUND)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("9012-3456-7890"),
        eq(visit.visitContact!!.telephone),
        check {
          assertSmsDetailsCancel(prisonName = prison.prisonName, time = "10:30 AM", date = "30 November 2023", bookingReference = bookingReference, prisonPhoneNumber = null, it)
        },
      )
    }
  }

  @Test
  fun `when visit cancelled message is received but the visit could not be found then cancel message is not sent`() {
    // Given
    val bookingReference = visit2.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "aa-xx-wn-ml") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  private fun assertSmsDetailsBookOrUpdate(prisonName: String, time: String, dayOfWeek: String, date: String, bookingReference: String, parameters: Map<String, String>) {
    Assertions.assertThat(parameters["prison"]).isEqualTo(prisonName)
    Assertions.assertThat(parameters["time"]).isEqualTo(time)
    Assertions.assertThat(parameters["dayofweek"]).isEqualTo(dayOfWeek)
    Assertions.assertThat(parameters["date"]).isEqualTo(date)
    Assertions.assertThat(parameters["ref number"]).isEqualTo(bookingReference)
  }

  private fun assertSmsDetailsCancel(prisonName: String, time: String, date: String, bookingReference: String, prisonPhoneNumber: String?, parameters: Map<String, String>) {
    Assertions.assertThat(parameters["prison"]).isEqualTo(prisonName)
    Assertions.assertThat(parameters["time"]).isEqualTo(time)
    Assertions.assertThat(parameters["date"]).isEqualTo(date)
    Assertions.assertThat(parameters["prison phone number"]).isEqualTo(prisonPhoneNumber)
    Assertions.assertThat(parameters["reference"]).isEqualTo(bookingReference)
  }
}
