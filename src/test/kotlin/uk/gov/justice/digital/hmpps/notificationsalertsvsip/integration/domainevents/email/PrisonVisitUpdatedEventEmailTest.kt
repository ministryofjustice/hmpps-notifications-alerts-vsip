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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitExternalSystemDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CHANGED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PrisonVisitUpdatedEventEmailTest : EventsIntegrationTestBase() {
  lateinit var visit: VisitDto
  lateinit var prison: PrisonDto
  lateinit var prisonerSearchResult: PrisonerSearchResultDto
  lateinit var prisonerContactsResult: List<PrisonerContactRegistryContactDto>
  lateinit var prisonContactDetailsDto: PrisonContactDetailsDto
  lateinit var prisonVisitors: List<String>

  @BeforeEach
  internal fun setUp() {
    visit = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
    )

    prison = PrisonDto("HEI", "Hewell", true)

    prisonerSearchResult = PrisonerSearchResultDto("Prisoner", "One")

    prisonerContactsResult = listOf(
      PrisonerContactRegistryContactDto("1234", "Visitor", "One", LocalDate.now().minusYears(30)),
      PrisonerContactRegistryContactDto("9876", "Visitor", "Two"),
    )

    prisonVisitors = listOf(
      "Visitor One (30 years old)",
      "Visitor Two (age not known)",
    )

    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333", webAddress = "website")
  }

  @Test
  fun `when visit updated message is received then updated email is sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")

    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_UPDATED.name]
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "end time" to "11am",
      "arrival time" to "45",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "closed visit" to "false",
      "opening sentence" to "visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "visitors" to prisonVisitors,
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit.prisonerId, prisonerSearchResult)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(visit.prisonerId, prisonerContactsResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId))
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit updated message is received then updated email is sent in the right time format when start time minutes is 01`() {
    // Given
    val visit3 = createVisitDto(
      bookingReference = "qq-yy-xx-kk",
      visitDate = LocalDate.now().plusDays(1),
      visitTime = LocalTime.of(0, 1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
    )
    val bookingReference = visit3.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit3.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit3.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_UPDATED.name]
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "time" to "12:01am",
      "end time" to "12:31am",
      "arrival time" to "45",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "closed visit" to "false",
      "opening sentence" to "visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "visitors" to prisonVisitors,
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit3)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit3.prisonerId, prisonerSearchResult)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(visit3.prisonerId, prisonerContactsResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit3.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId))
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visit3, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit updated message is received then updated email is sent in the right time format when start time minutes is 00`() {
    // Given
    val visit2 = createVisitDto(
      bookingReference = "zz-yy-xx-kk",
      visitDate = LocalDate.now().plusWeeks(2),
      visitTime = LocalTime.of(8, 0),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
    )
    val bookingReference = visit2.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit2.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)
    val visitDate = visit2.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_UPDATED.name]
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "time" to "8am",
      "end time" to "8:30am",
      "arrival time" to "45",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "closed visit" to "false",
      "opening sentence" to "visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "visitors" to prisonVisitors,
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit2)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit2.prisonerId, prisonerSearchResult)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(visit2.prisonerId, prisonerContactsResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit2.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId))
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visit2, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit updated message is received but the visit could not be found then updated email is not sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  @Test
  fun `when visit updated message is received but the visit is in the past then updated email is not sent`() {
    // Given
    val pastDatedVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
    )
    val bookingReference = pastDatedVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(pastDatedVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  @Test
  fun `when visit updated message is received but no visit contact found then updated email is not sent`() {
    // Given
    val noContactVisit = createVisitDto(
      bookingReference = "bb-cc-dd-zz",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", null, null),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
    )
    val bookingReference = noContactVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(noContactVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  @Test
  fun `when single digit visit date updated then message is sent out with the right visit date format`() {
    // Given
    val singleDigitDateVisit = createVisitDto(
      bookingReference = "bb-cc-dd-xd",
      visitDate = LocalDate.now().plusYears(1).withMonth(1).withDayOfMonth(1),
      visitTime = LocalTime.of(1, 5),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
    )
    val bookingReference = singleDigitDateVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(singleDigitDateVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // expected visit date should not be 2 digits
    val visitYear = singleDigitDateVisit.startTimestamp.toLocalDate().year
    val expectedVisitDate = "1 January $visitYear"
    val expectedDayOfWeek = singleDigitDateVisit.startTimestamp.toLocalDate().dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_UPDATED.name]
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "time" to "1:05am",
      "end time" to "1:35am",
      "arrival time" to "45",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "closed visit" to "false",
      "opening sentence" to "visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "visitors" to prisonVisitors,
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, singleDigitDateVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(singleDigitDateVisit.prisonerId, prisonerSearchResult)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(singleDigitDateVisit.prisonerId, prisonerContactsResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId))
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, singleDigitDateVisit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit updated message is received but and only matching visitors are on the updated email`() {
    // Given
    val visitWithOneVisitor = createVisitDto(
      bookingReference = "bi-vn-wn-ml",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234)),
    )
    val bookingReference = visitWithOneVisitor.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visitWithOneVisitor.reference, "123456")

    prisonVisitors = listOf("Visitor One (30 years old)")

    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_UPDATED.name]
    val visitDate = visitWithOneVisitor.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "end time" to "11am",
      "arrival time" to "45",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "closed visit" to "false",
      "opening sentence" to "visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "visitors" to prisonVisitors,
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visitWithOneVisitor)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visitWithOneVisitor.prisonerId, prisonerSearchResult)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(visitWithOneVisitor.prisonerId, prisonerContactsResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId))
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visitWithOneVisitor, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit updated message is received and prisoner-contact-registry returns an error, updated email is still sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_UPDATED.name]
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "end time" to "11am",
      "arrival time" to "45",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "closed visit" to "false",
      "opening sentence" to "visit to see Prisoner One",
      "prisoner" to "Prisoner One",
      "visitors" to listOf("You can view visitor information in the bookings section of your GOV.UK One Login"),
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit.prisonerId, prisonerSearchResult)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(visit.prisonerId, null)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId))
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit updated message is received and prisoner-search returns an error, updated email is still sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")

    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_UPDATED.name]
    val visitDate = visit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "ref number" to bookingReference,
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "end time" to "11am",
      "arrival time" to "45",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "main contact name" to "Contact One",
      "closed visit" to "false",
      "opening sentence" to "visit to the prison",
      "prisoner" to "the prisoner",
      "visitors" to prisonVisitors,
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "website" to prisonContactDetailsDto.webAddress!!,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit.prisonerId, null)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(visit.prisonerId, prisonerContactsResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      ),
    ).thenReturn(buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId))
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit updated by an external system, message is received and notifications are skipped`() {
    // Given
    val externalVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now().plusWeeks(1),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      externalSystemDetailsDto = VisitExternalSystemDetailsDto(clientName = "nexus", clientVisitReference = "abc"),
    )
    val bookingReference = externalVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(externalVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_CHANGED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, externalVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  private fun verifyEmailSent(templateId: String, visit: VisitDto, visitAdditionalInfo: VisitAdditionalInfo, templateVars: Map<String, Any>) {
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, visitAdditionalInfo) }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(visit, VisitEventType.UPDATED, visitAdditionalInfo.eventAuditId) }
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
    await untilAsserted { verify(prisonVisitChangedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.UPDATED, visitAdditionalInfo) }
    await untilAsserted { verify(emailSenderService, times(0)).sendEmail(any(), any(), any()) }
    await untilAsserted { verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any()) }
    await untilAsserted { verify(visitSchedulerService, times(0)).createNotifyNotification(any()) }
  }
}
