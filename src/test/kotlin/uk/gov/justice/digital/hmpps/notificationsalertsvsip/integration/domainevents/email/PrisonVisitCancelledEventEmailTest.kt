package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.email

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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.OutcomeStatus
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PrisonVisitCancelledEventEmailTest : EventsIntegrationTestBase() {
  lateinit var visit: VisitDto
  lateinit var pastDatedVisit: VisitDto
  lateinit var noContactVisit: VisitDto
  lateinit var singleDigitDateVisit: VisitDto
  lateinit var cancelledByPrisonerVisit: VisitDto
  lateinit var cancelledByPrisonVisit: VisitDto
  lateinit var unsupportedCancelledTypeVisit: VisitDto
  lateinit var prison: PrisonDto
  lateinit var prisonerSearchResult: PrisonerSearchResultDto
  lateinit var prisonContactDetailsDto: PrisonContactDetailsDto

  @BeforeEach
  internal fun setUp() {
    visit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = OutcomeStatus.VISITOR_CANCELLED,
    )

    pastDatedVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = OutcomeStatus.VISITOR_CANCELLED,
    )

    noContactVisit = createVisitDto(
      bookingReference = "bb-cc-dd-zz",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", null, null),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = OutcomeStatus.VISITOR_CANCELLED,
    )

    singleDigitDateVisit = createVisitDto(
      bookingReference = "bb-cc-dd-xd",
      visitDate = LocalDate.now().plusYears(1).withMonth(1).withDayOfMonth(1),
      visitTime = LocalTime.of(1, 5),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = OutcomeStatus.VISITOR_CANCELLED,
    )

    cancelledByPrisonerVisit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = OutcomeStatus.PRISONER_CANCELLED,
    )

    cancelledByPrisonVisit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = OutcomeStatus.ESTABLISHMENT_CANCELLED,
    )

    unsupportedCancelledTypeVisit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = OutcomeStatus.NOT_RECORDED,
    )

    prison = PrisonDto("HEI", "Hewell", true)

    prisonerSearchResult = PrisonerSearchResultDto("Prisoner", "One")

    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333", webAddress = "website")
  }

  @Test
  fun `when visit cancelled message is received then cancelled email is sent`() {
    // Given
    val bookingReference = visit.reference

    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED.name]
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "opening sentence" to "Your visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit.prisonerId, prisonerSearchResult)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(visit, VisitEventType.CANCELLED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visit.reference,
      )
    }
  }

  @Test
  fun `when single digit visit date booked then message is sent out with the right visit date format`() {
    // Given
    val bookingReference = singleDigitDateVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // expected visit date should not be 2 digits
    val visitYear = singleDigitDateVisit.startTimestamp.toLocalDate().year
    val expectedVisitDate = "1 January $visitYear"
    val expectedDayOfWeek = singleDigitDateVisit.startTimestamp.toLocalDate().dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED.name]
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "opening sentence" to "Your visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, singleDigitDateVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(singleDigitDateVisit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bb-cc-dd-xd") }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(singleDigitDateVisit, VisitEventType.CANCELLED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        singleDigitDateVisit.visitContact.email,
        templateVars,
        singleDigitDateVisit.reference,
      )
    }
  }

  @Test
  fun `when visit cancelled by prison then cancelled by prison email template is sent`() {
    // Given
    val bookingReference = cancelledByPrisonVisit.reference

    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED_BY_PRISON.name]
    val visitDate = cancelledByPrisonVisit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "opening sentence" to "Your visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, cancelledByPrisonVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(cancelledByPrisonVisit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(cancelledByPrisonVisit, VisitEventType.CANCELLED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        cancelledByPrisonVisit.visitContact.email,
        templateVars,
        cancelledByPrisonVisit.reference,
      )
    }
  }

  @Test
  fun `when visit cancelled by prisoner then cancelled by prison email template is sent`() {
    // Given
    val bookingReference = cancelledByPrisonerVisit.reference

    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED_BY_PRISONER.name]
    val visitDate = cancelledByPrisonerVisit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "opening sentence" to "Your visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, cancelledByPrisonerVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(cancelledByPrisonerVisit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(cancelledByPrisonerVisit, VisitEventType.CANCELLED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        cancelledByPrisonerVisit.visitContact.email,
        templateVars,
        cancelledByPrisonerVisit.reference,
      )
    }
  }

  @Test
  fun `when visit cancelled by un-supported reason then default cancelled email template is sent`() {
    // Given
    val bookingReference = unsupportedCancelledTypeVisit.reference

    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED.name]
    val visitDate = unsupportedCancelledTypeVisit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "opening sentence" to "Your visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, unsupportedCancelledTypeVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(unsupportedCancelledTypeVisit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(unsupportedCancelledTypeVisit, VisitEventType.CANCELLED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        unsupportedCancelledTypeVisit.visitContact.email,
        templateVars,
        unsupportedCancelledTypeVisit.reference,
      )
    }
  }

  @Test
  fun `when visit cancelled message is received and prisoner-search returns an error, cancelled email is still sent`() {
    // Given
    val bookingReference = visit.reference

    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED.name]
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "opening sentence" to "Your visit to the prison",
      "prisoner" to "the prisoner",
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit.prisonerId, null)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(visit, VisitEventType.CANCELLED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visit.reference,
      )
    }
  }

  @Test
  fun `when visit cancelled message is received but the visit could not be found then cancelled email is not sent`() {
    // Given
    val bookingReference = visit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bi-vn-wn-ml") }
    await untilAsserted { verify(emailSenderService, times(0)).sendEmail(any(), any()) }
  }

  @Test
  fun `when visit cancelled message is received but the visit is in the past then cancelled email is not sent`() {
    // Given
    val bookingReference = pastDatedVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "aa-bb-cc-dd") }
    await untilAsserted { verify(emailSenderService, times(0)).sendEmail(any(), any()) }
  }

  @Test
  fun `when visit cancelled message is received but no visit contact found then cancelled email is not sent`() {
    // Given
    val bookingReference = noContactVisit.reference
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, "bb-cc-dd-zz") }
    await untilAsserted { verify(emailSenderService, times(0)).sendEmail(any(), any()) }
  }
}
