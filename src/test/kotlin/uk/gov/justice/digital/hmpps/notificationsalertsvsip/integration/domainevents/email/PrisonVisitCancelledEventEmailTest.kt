package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.email

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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitExternalSystemDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email.BaseEmailNotificationHandler.Companion.GOV_UK_PRISON_PAGE
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PrisonVisitCancelledEventEmailTest : EventsIntegrationTestBase() {
  lateinit var visit: VisitDto
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
      outcomeStatus = "BOOKER_CANCELLED",
      visitSubStatus = "CANCELLED",
    )

    prison = PrisonDto("HEI", "Hewell", true)
    prisonerSearchResult = PrisonerSearchResultDto("Prisoner", "One")
    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333", webAddress = "website")
  }

  @Test
  fun `when visit cancelled message is received then cancelled email is sent`() {
    // Given
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED.name]
    val templateVars = createTemplateVars(visit)
    val notificationClientResponse = buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(notificationClientResponse)
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit cancelled message is received with outcomeStaus as ADMINISTRATIVE_CANCELLATION then cancelled email is sent`() {
    // Given
    val cancelledVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "email@example.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = "ADMINISTRATIVE_CANCELLATION",
      visitSubStatus = "CANCELLED",
    )

    val visitAdditionalInfo = VisitAdditionalInfo(cancelledVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED_BY_PRISON.name]
    val templateVars = createTemplateVars(cancelledVisit)
    val notificationClientResponse = buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(cancelledVisit.reference, cancelledVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(cancelledVisit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        cancelledVisit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(notificationClientResponse)
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, cancelledVisit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when single digit visit date booked then message is sent out with the right visit date format`() {
    // Given
    val singleDigitDateVisit = createVisitDto(
      bookingReference = "bb-cc-dd-xd",
      visitDate = LocalDate.now().plusYears(1).withMonth(1).withDayOfMonth(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = "VISITOR_CANCELLED",
      visitSubStatus = "CANCELLED",
    )
    val visitAdditionalInfo = VisitAdditionalInfo(singleDigitDateVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED.name]
    val templateVars = createTemplateVars(singleDigitDateVisit)

    val notificationClientResponse = buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(singleDigitDateVisit.reference, singleDigitDateVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(singleDigitDateVisit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(notificationClientResponse)
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, singleDigitDateVisit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit cancelled by prison then cancelled by prison email template is sent`() {
    // Given
    val cancelledByPrisonVisit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = "ESTABLISHMENT_CANCELLED",
      visitSubStatus = "CANCELLED",
    )
    val visitAdditionalInfo = VisitAdditionalInfo(cancelledByPrisonVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED_BY_PRISON.name]
    val templateVars = createTemplateVars(cancelledByPrisonVisit, phone = GOV_UK_PRISON_PAGE, webAddress = GOV_UK_PRISON_PAGE)

    val notificationClientResponse = buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(cancelledByPrisonVisit.reference, cancelledByPrisonVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(cancelledByPrisonVisit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, null)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(notificationClientResponse)
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, cancelledByPrisonVisit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit cancelled by prisoner then cancelled by prison email template is sent`() {
    // Given
    val cancelledByPrisonerVisit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = "PRISONER_CANCELLED",
      visitSubStatus = "CANCELLED",
    )
    val visitAdditionalInfo = VisitAdditionalInfo(cancelledByPrisonerVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED_BY_PRISONER.name]
    val templateVars = createTemplateVars(cancelledByPrisonerVisit)

    val notificationClientResponse = buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(cancelledByPrisonerVisit.reference, cancelledByPrisonerVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(cancelledByPrisonerVisit.prisonerId, prisonerSearchResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(notificationClientResponse)
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, cancelledByPrisonerVisit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit cancelled by un-supported reason then validation exception is thrown`() {
    // Given
    val unsupportedCancelledTypeVisit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = "NOT_RECORDED",
      visitSubStatus = "CANCELLED",
    )
    val visitAdditionalInfo = VisitAdditionalInfo(unsupportedCancelledTypeVisit.reference, "123456")

    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(unsupportedCancelledTypeVisit.reference, unsupportedCancelledTypeVisit)

    // Then
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(any(), any(), any()) }
    await untilAsserted { verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any()) }
    await untilAsserted { verify(visitSchedulerService, times(0)).createNotifyNotification(any()) }
  }

  @Test
  fun `when visit cancelled message is received and prisoner-search returns an error, cancelled email is still sent`() {
    // Given
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_CANCELLED.name]
    val templateVars = createTemplateVars(visit, openingSentence = "visit to the prison", prisoner = "the prisoner")

    val notificationClientResponse = buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(visit.reference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit.prisonerId, null)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(notificationClientResponse)
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit cancelled message is received but the visit could not be found then cancelled email is not sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  @Test
  fun `when visit cancelled message is received but the visit is in the past then cancelled email is not sent`() {
    // Given
    val pastDatedVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = "VISITOR_CANCELLED",
      visitSubStatus = "CANCELLED",
    )
    val visitAdditionalInfo = VisitAdditionalInfo(pastDatedVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(pastDatedVisit.reference, null, HttpStatus.NOT_FOUND)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  @Test
  fun `when visit cancelled message is received but no visit contact found then cancelled email is not sent`() {
    // Given
    val noContactVisit = createVisitDto(
      bookingReference = "bb-cc-dd-zz",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", null, null),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      outcomeStatus = "VISITOR_CANCELLED",
      visitSubStatus = "CANCELLED",
    )
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(noContactVisit.reference, null, HttpStatus.NOT_FOUND)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
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
      visitSubStatus = "CANCELLED",
    )
    val visitAdditionalInfo = VisitAdditionalInfo(externalVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CANCELLED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(externalVisit.reference, externalVisit)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  private fun createTemplateVars(visit: VisitDto, openingSentence: String? = "visit to see $prisonerSearchResult", prisoner: String? = prisonerSearchResult.toString(), phone: String? = prisonContactDetailsDto.phoneNumber, webAddress: String? = prisonContactDetailsDto.webAddress): Map<String, Any> {
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }

    return mapOf<String, Any>(
      "ref number" to visit.reference,
      "prison" to prison.prisonName,
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "time" to "10:30am",
      "end time" to "11am",
      "main contact name" to visit.visitContact.name,
      "opening sentence" to openingSentence!!,
      "prisoner" to prisoner!!,
      "phone" to phone!!,
      "website" to webAddress!!,
    )
  }

  private fun verifyEmailSent(templateId: String, visit: VisitDto, visitAdditionalInfo: VisitAdditionalInfo, templateVars: Map<String, Any>) {
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(visit, VisitEventType.CANCELLED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
    await untilAsserted {
      verify(visitSchedulerService, times(1)).createNotifyNotification(any())
    }
  }

  private fun verifyEmailNotSent(visitAdditionalInfo: VisitAdditionalInfo) {
    await untilAsserted { verify(prisonVisitCancelledEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.CANCELLED, visitAdditionalInfo) }
    await untilAsserted { verify(emailSenderService, times(0)).sendEmail(any(), any(), any()) }
    await untilAsserted { verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any()) }
    await untilAsserted { verify(visitSchedulerService, times(0)).createNotifyNotification(any()) }
  }
}
