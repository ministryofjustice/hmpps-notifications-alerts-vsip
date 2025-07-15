package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.sms

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonContactDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitExternalSystemDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PrisonVisitCancelledEventSmsTest : EventsIntegrationTestBase() {
  companion object {
    const val EXPECTED_DATE_PATTERN = "d MMMM yyyy"
  }

  lateinit var visit: VisitDto
  lateinit var visit2: VisitDto
  lateinit var visit3: VisitDto
  lateinit var visit4: VisitDto
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
      visitSubStatus = "BOOKED",
    )

    visit2 = createVisitDto(
      bookingReference = "aa-xx-wn-ml",
      visitDate = LocalDate.now().plusDays(3),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
      visitSubStatus = "BOOKED",
    )

    visit3 = createVisitDto(
      bookingReference = "zz-yy-xx-kk",
      visitDate = LocalDate.now().plusWeeks(2),
      visitTime = LocalTime.of(8, 0),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
      visitSubStatus = "BOOKED",
    )

    visit4 = createVisitDto(
      bookingReference = "qq-yy-xx-kk",
      visitDate = LocalDate.now().plusDays(1),
      visitTime = LocalTime.of(0, 1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
      visitSubStatus = "BOOKED",
    )

    pastDatedVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
      visitSubStatus = "BOOKED",
    )

    noContactVisit = createVisitDto(
      bookingReference = "bb-cc-dd-zz",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", null),
      visitors = emptyList(),
      visitSubStatus = "BOOKED",
    )

    singleDigitDateVisit = createVisitDto(
      bookingReference = "bb-cc-dd-xd",
      visitDate = LocalDate.now().plusYears(1).withMonth(1).withDayOfMonth(1),
      visitTime = LocalTime.of(1, 5),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", "01234567890"),
      visitors = emptyList(),
      visitSubStatus = "BOOKED",
    )

    prison = PrisonDto("HEI", "Hewell", true)

    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333")
  }

  @Test
  fun `when visit cancelled message is received then cancel message is sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_CANCEL.name]
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "reference" to bookingReference,
      "prison phone number" to prisonContactDetailsDto.phoneNumber!!,
    )
    val notificationClientResponse = buildSendSmsResponse(reference = visitAdditionalInfo.eventAuditId)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendSms(
        templateId,
        visit.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(notificationClientResponse)
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit, VisitEventType.CANCELLED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        visit.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
    await untilAsserted {
      verify(visitSchedulerService, times(1)).createNotifyNotification(any())
    }
  }

  @Test
  fun `when visit cancelled message is received then cancel message is sent with the right time format  when start time minutes is 00`() {
    // Given
    val bookingReference = visit3.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit3.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit3.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_CANCEL.name]
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "8am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "reference" to bookingReference,
      "prison phone number" to prisonContactDetailsDto.phoneNumber!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit3)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit3, VisitEventType.CANCELLED, visitAdditionalInfo.eventAuditId) }
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
  fun `when visit cancelled message is received then cancel message is sent with the right time format  when start time minutes is 01`() {
    // Given
    val bookingReference = visit4.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit4.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit4.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_CANCEL.name]
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "12:01am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "reference" to bookingReference,
      "prison phone number" to prisonContactDetailsDto.phoneNumber!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit4)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit4, VisitEventType.CANCELLED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        visit4.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
  }

  @Test
  fun `when visit cancelled message is received but no prison contact number then cancel message is sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_CANCEL_NO_PRISON_NUMBER.name]
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "reference" to bookingReference,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, null, HttpStatus.NOT_FOUND)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(visit, VisitEventType.CANCELLED, visitAdditionalInfo.eventAuditId) }
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
  fun `when visit cancelled message is received but the visit could not be found then cancel message is not sent`() {
    // Given
    val bookingReference = visit2.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit2.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  @Test
  fun `when visit cancelled message is received but the visit is in the past then cancel message is not sent`() {
    // Given
    val bookingReference = pastDatedVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(pastDatedVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  @Test
  fun `when visit cancelled message is received but no visit contact then cancel message is not sent`() {
    // Given
    val bookingReference = noContactVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(noContactVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }

  @Test
  fun `when single digit visit date cancelled then message is sent out with the right visit date format`() {
    // Given
    val bookingReference = singleDigitDateVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(singleDigitDateVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitYear = singleDigitDateVisit.startTimestamp.toLocalDate().year
    // expected visit date should not be 2 digits
    val expectedVisitDate = "1 January $visitYear"
    val expectedDayOfWeek = singleDigitDateVisit.startTimestamp.toLocalDate().dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.smsTemplates[SmsTemplateNames.VISIT_CANCEL.name]
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "1:05am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "reference" to bookingReference,
      "prison phone number" to prisonContactDetailsDto.phoneNumber!!,
    )
    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, singleDigitDateVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendSms(singleDigitDateVisit, VisitEventType.CANCELLED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        singleDigitDateVisit.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
  }

  @Test
  fun `when visit cancelled by an external system, message is received and notifications are skipped`() {
    // Given
    val externalVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now().plusWeeks(1),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = "VISITOR_CANCELLED",
      externalSystemDetailsDto = VisitExternalSystemDetailsDto(clientName = "nexus", clientVisitReference = "abc"),
      visitSubStatus = "BOOKED",
    )
    val bookingReference = externalVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(bookingReference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, externalVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(0)).sendSms(any(), any(), any()) }
  }
}
