package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.sms

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonContactDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PrisonVisitBookedEventSmsTest : EventsIntegrationTestBase() {
  lateinit var visit: VisitDto
  lateinit var visit2: VisitDto
  lateinit var visit3: VisitDto
  lateinit var pastDatedVisit: VisitDto
  lateinit var noContactVisit: VisitDto
  lateinit var singleDigitDateVisit: VisitDto
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
      visitors = emptyList(),
    )

    visit2 = createVisitDto(
      bookingReference = "zz-yy-xx-kk",
      visitDate = LocalDate.now().plusWeeks(2),
      visitTime = LocalTime.of(8, 0),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
    )

    visit3 = createVisitDto(
      bookingReference = "qq-yy-xx-kk",
      visitDate = LocalDate.now().plusDays(1),
      visitTime = LocalTime.of(0, 1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
    )

    pastDatedVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
    )

    noContactVisit = createVisitDto(
      bookingReference = "bb-cc-dd-zz",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", null),
      visitors = emptyList(),
    )

    singleDigitDateVisit = createVisitDto(
      bookingReference = "bb-cc-dd-xd",
      visitDate = LocalDate.now().plusYears(1).withMonth(1).withDayOfMonth(1),
      visitTime = LocalTime.of(1, 5),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
    )

    prison = PrisonDto("HEI", "Hewell", true)

    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333")
  }

  @Test
  fun `when visit booked message is received then booking message is sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_BOOKING.name]
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "ref number" to bookingReference,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit, VisitEventType.BOOKED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        visit.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
  }

  @Test
  fun `when visit booked message is received then booking message is sent in the right time format when start time minutes is 01`() {
    // Given
    val bookingReference = visit3.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit3.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit3)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    val visitDate = visit3.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_BOOKING.name]
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "12:01am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "ref number" to bookingReference,
    )

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit3, VisitEventType.BOOKED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        visit3.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
  }

  @Test
  fun `when visit booked message is received then booking message is sent in the right time format when start time minutes is 00`() {
    // Given
    val bookingReference = visit2.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit2.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit2.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_BOOKING.name]
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "8am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "ref number" to bookingReference,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit2)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit2, VisitEventType.BOOKED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        visit2.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
  }

  @Test
  fun `when visit booked message is received but the visit could not be found then booking message is not sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  @Test
  fun `when visit booked message is received but the visit is in the past then booking message is not sent`() {
    // Given
    val bookingReference = pastDatedVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(pastDatedVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  @Test
  fun `when visit booked message is received but no visit contact found then booking message is not sent`() {
    // Given
    val bookingReference = noContactVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(noContactVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  @Test
  fun `when single digit visit date booked then message is sent out with the right visit date format`() {
    // Given
    val bookingReference = singleDigitDateVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(singleDigitDateVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // expected visit date should not be 2 digits
    val visitYear = singleDigitDateVisit.startTimestamp.toLocalDate().year
    val expectedVisitDate = "1 January $visitYear"
    val expectedDayOfWeek = singleDigitDateVisit.startTimestamp.toLocalDate().dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_BOOKING.name]
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "1:05am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "ref number" to bookingReference,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, singleDigitDateVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(singleDigitDateVisit, VisitEventType.BOOKED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        singleDigitDateVisit.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
  }
}
