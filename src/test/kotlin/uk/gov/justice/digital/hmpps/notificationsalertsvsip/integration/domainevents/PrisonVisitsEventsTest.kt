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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.NotificationService.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CHANGED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
  lateinit var visit3: VisitDto
  lateinit var visit4: VisitDto
  lateinit var pastDatedVisit: VisitDto
  lateinit var noContactVisit: VisitDto
  lateinit var prison: PrisonDto
  lateinit var prisonContactDetailsDto: PrisonContactDetailsDto

  @BeforeEach
  internal fun setUp() {
    visit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
    )

    visit2 = createVisitDto(
      bookingReference = "aa-xx-wn-ml",
      visitDate = LocalDate.now().plusDays(3),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
    )

    visit3 = createVisitDto(
      bookingReference = "zz-yy-xx-kk",
      visitDate = LocalDate.now().plusWeeks(2),
      visitTime = LocalTime.of(8, 0),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
    )

    visit4 = createVisitDto(
      bookingReference = "qq-yy-xx-kk",
      visitDate = LocalDate.now().plusDays(1),
      visitTime = LocalTime.of(0, 1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
    )

    pastDatedVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
    )

    noContactVisit = createVisitDto(
      bookingReference = "bb-cc-dd-zz",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", null),
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
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, "bi-vn-wn-ml") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("1234-5678-9012"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsBookOrUpdate(
            prisonName = prison.prisonName,
            time = "10:30am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            parameters = it,
          )
        },
        eq(bookingReference),
      )
    }
  }

  @Test
  fun `when visit booked message is received then booking message is sent in the right time format when start time minutes is 01`() {
    // Given
    val bookingReference = visit4.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit4)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    val visitDate = visit4.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, "qq-yy-xx-kk") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("1234-5678-9012"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsBookOrUpdate(
            prisonName = prison.prisonName,
            time = "12:01am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            parameters = it,
          )
        },
        eq(bookingReference),
      )
    }
  }

  @Test
  fun `when visit booked message is received then booking message is sent in the right time format when start time minutes is 00`() {
    // Given
    val bookingReference = visit3.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit3)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    val visitDate = visit3.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, "zz-yy-xx-kk") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("1234-5678-9012"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsBookOrUpdate(
            prisonName = prison.prisonName,
            time = "8am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            parameters = it,
          )
        },
        eq(bookingReference),
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
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
  }

  @Test
  fun `when visit booked message is received but the visit is in the past then booking message is not sent`() {
    // Given
    val bookingReference = pastDatedVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, "aa-bb-cc-dd") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
  }

  @Test
  fun `when visit booked message is received but no visit contact found then booking message is not sent`() {
    // Given
    val bookingReference = noContactVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, "bb-cc-dd-zz") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
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
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "bi-vn-wn-ml") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("5678-9012-3456"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsBookOrUpdate(
            prisonName = prison.prisonName,
            time = "10:30am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            parameters = it,
          )
        },
        eq(bookingReference),
      )
    }
  }

  @Test
  fun `when visit updated message is received then update message is sent with th right time format when start time minutes is 00`() {
    // Given
    val bookingReference = visit3.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit3)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    val visitDate = visit3.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "zz-yy-xx-kk") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("5678-9012-3456"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsBookOrUpdate(
            prisonName = prison.prisonName,
            time = "8am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            parameters = it,
          )
        },
        eq(bookingReference),
      )
    }
  }

  @Test
  fun `when visit updated message is received then update message is sent with th right time format when start time minutes is 01`() {
    // Given
    val bookingReference = visit4.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit4)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    val visitDate = visit4.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "qq-yy-xx-kk") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("5678-9012-3456"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsBookOrUpdate(
            prisonName = prison.prisonName,
            time = "12:01am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            parameters = it,
          )
        },
        eq(bookingReference),
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
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
  }

  @Test
  fun `when visit updated message is received but the visit is in the past then update message is not sent`() {
    // Given
    val bookingReference = pastDatedVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "aa-bb-cc-dd") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
  }

  @Test
  fun `when visit updated message is received but no visit contact then update message is not sent`() {
    // Given
    val bookingReference = noContactVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "bb-cc-dd-zz") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
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
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("7890-1234-5678"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsCancel(
            prisonName = prison.prisonName,
            time = "10:30am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            prisonPhoneNumber = prisonContactDetailsDto.phoneNumber,
            it,
          )
        },
        eq(bookingReference),
      )
    }
  }

  @Test
  fun `when visit cancelled message is received then cancel message is sent with the right time format  when start time minutes is 00`() {
    // Given
    val bookingReference = visit3.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson("zz-yy-xx-kk"))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit3)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    val visitDate = visit3.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "zz-yy-xx-kk") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("7890-1234-5678"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsCancel(
            prisonName = prison.prisonName,
            time = "8am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            prisonPhoneNumber = prisonContactDetailsDto.phoneNumber,
            it,
          )
        },
        eq(bookingReference),
      )
    }
  }

  @Test
  fun `when visit cancelled message is received then cancel message is sent with the right time format  when start time minutes is 01`() {
    // Given
    val bookingReference = visit4.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson("qq-yy-xx-kk"))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit4)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    val visitDate = visit4.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "qq-yy-xx-kk") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("7890-1234-5678"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsCancel(
            prisonName = prison.prisonName,
            time = "12:01am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            prisonPhoneNumber = prisonContactDetailsDto.phoneNumber,
            it,
          )
        },
        eq(bookingReference),
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
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }

    await untilAsserted {
      verify(smsSenderService, times(1)).sendSms(
        eq("9012-3456-7890"),
        eq(visit.visitContact.telephone!!),
        check {
          assertSmsDetailsCancel(
            prisonName = prison.prisonName,
            time = "10:30am",
            dayOfWeek = expectedDayOfWeek,
            date = expectedVisitDate,
            bookingReference = bookingReference,
            prisonPhoneNumber = null,
            it,
          )
        },
        eq(bookingReference),
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
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
  }

  @Test
  fun `when visit cancelled message is received but the visit is in the past then cancel message is not sent`() {
    // Given
    val bookingReference = pastDatedVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "aa-bb-cc-dd") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
  }

  @Test
  fun `when visit cancelled message is received but no visit contact then cancel message is not sent`() {
    // Given
    val bookingReference = noContactVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bb-cc-dd-zz") }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any(), any()) }
  }

  private fun assertSmsDetailsBookOrUpdate(prisonName: String, time: String, dayOfWeek: String, date: String, bookingReference: String, parameters: Map<String, String>) {
    Assertions.assertThat(parameters["prison"]).isEqualTo(prisonName)
    Assertions.assertThat(parameters["time"]).isEqualTo(time)
    Assertions.assertThat(parameters["dayofweek"]).isEqualTo(dayOfWeek)
    Assertions.assertThat(parameters["date"]).isEqualTo(date)
    Assertions.assertThat(parameters["ref number"]).isEqualTo(bookingReference)
  }

  private fun assertSmsDetailsCancel(prisonName: String, time: String, dayOfWeek: String, date: String, bookingReference: String, prisonPhoneNumber: String?, parameters: Map<String, String>) {
    Assertions.assertThat(parameters["prison"]).isEqualTo(prisonName)
    Assertions.assertThat(parameters["time"]).isEqualTo(time)
    Assertions.assertThat(parameters["date"]).isEqualTo(date)
    Assertions.assertThat(parameters["dayofweek"]).isEqualTo(dayOfWeek)
    Assertions.assertThat(parameters["prison phone number"]).isEqualTo(prisonPhoneNumber)
    Assertions.assertThat(parameters["reference"]).isEqualTo(bookingReference)
  }
}
