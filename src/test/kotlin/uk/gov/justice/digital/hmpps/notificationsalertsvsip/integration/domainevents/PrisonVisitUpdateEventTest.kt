package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents

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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CHANGED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PrisonVisitUpdateEventTest : EventsIntegrationTestBase() {
  companion object {
    const val EXPECTED_DATE_PATTERN = "d MMMM yyyy"
  }

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
    )

    visit2 = createVisitDto(
      bookingReference = "zz-yy-xx-kk",
      visitDate = LocalDate.now().plusWeeks(2),
      visitTime = LocalTime.of(8, 0),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
    )

    visit3 = createVisitDto(
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

    singleDigitDateVisit = createVisitDto(
      bookingReference = "bb-cc-dd-xd",
      visitDate = LocalDate.now().plusYears(1).withMonth(1).withDayOfMonth(1),
      visitTime = LocalTime.of(1, 5),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
    )

    prison = PrisonDto("HEI", "Hewell", true)

    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333")
  }

  @Test
  fun `when visit updated message is received then update message is sent`() {
    // Given
    val bookingReference = visit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = smsTemplatesConfig.templates[SmsTemplateNames.VISIT_UPDATE.name]
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
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "bi-vn-wn-ml") }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit, VisitEventType.UPDATED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        visit.visitContact.telephone,
        templateVars,
        visit.reference,
      )
    }
  }

  @Test
  fun `when visit updated message is received then update message is sent with th right time format when start time minutes is 00`() {
    // Given
    val bookingReference = visit2.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit2.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = smsTemplatesConfig.templates[SmsTemplateNames.VISIT_UPDATE.name]
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
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "zz-yy-xx-kk") }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit2, VisitEventType.UPDATED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        visit2.visitContact.telephone,
        templateVars,
        visit2.reference,
      )
    }
  }

  @Test
  fun `when visit updated message is received then update message is sent with th right time format when start time minutes is 01`() {
    // Given
    val bookingReference = visit3.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit3.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = smsTemplatesConfig.templates[SmsTemplateNames.VISIT_UPDATE.name]
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "12:01am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "ref number" to bookingReference,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit3)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "qq-yy-xx-kk") }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit3, VisitEventType.UPDATED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        visit3.visitContact.telephone,
        templateVars,
        visit3.reference,
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
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any()) }
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
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any()) }
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
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any()) }
  }

  @Test
  fun `when visit date updated to a single digit date then message is sent out with the right visit date format`() {
    // Given
    val bookingReference = singleDigitDateVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitYear = singleDigitDateVisit.startTimestamp.toLocalDate().year
    // expected visit date should not be 2 digits
    val expectedVisitDate = "1 January $visitYear"
    val expectedDayOfWeek = singleDigitDateVisit.startTimestamp.toLocalDate().dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = smsTemplatesConfig.templates[SmsTemplateNames.VISIT_UPDATE.name]
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
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, "bb-cc-dd-xd") }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(singleDigitDateVisit, VisitEventType.UPDATED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        singleDigitDateVisit.visitContact.telephone,
        templateVars,
        singleDigitDateVisit.reference,
      )
    }
  }
}
