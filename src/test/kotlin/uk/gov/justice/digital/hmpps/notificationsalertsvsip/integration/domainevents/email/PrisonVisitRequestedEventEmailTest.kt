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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_REQUESTED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType.BOOKED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PrisonVisitRequestedEventEmailTest : EventsIntegrationTestBase() {
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
      visitSubStatus = "REQUESTED",
    )

    prison = PrisonDto("HEI", "Hewell", true)

    prisonerSearchResult = PrisonerSearchResultDto("PRISONER", "ONE")

    prisonerContactsResult = listOf(
      PrisonerContactRegistryContactDto("1234", "Visitor", "One", (LocalDate.now().minusYears(30))),
      PrisonerContactRegistryContactDto("9876", "Visitor", "Two"),
    )

    prisonVisitors = listOf(
      "Visitor One (30 years old)",
      "Visitor Two (age not known)",
    )

    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333", webAddress = "website")
  }

  @Test
  fun `when visit requested message is received then visit request email is sent`() {
    // Given
    val bookingReference = visit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(visit.reference, "123456")

    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[VISIT_REQUESTED.name]
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

    val notificationClientResponse = buildSendEmailResponse(reference = visitAdditionalInfo.eventAuditId)

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
    ).thenReturn(notificationClientResponse)
    visitSchedulerMockServer.stubCreateNotifyNotification(HttpStatus.OK)

    // Then
    verifyEmailSent(templateId!!, visit, visitAdditionalInfo, templateVars)
  }

  @Test
  fun `when visit requested message is received but the visit could not be found then request email is not sent`() {
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
    verifyEmailNotSent(visitAdditionalInfo)
  }

  @Test
  fun `when visit requested message is received but the visit is in the past then request email is not sent`() {
    // Given
    val pastDatedVisit = createVisitDto(
      bookingReference = "aa-bb-cc-dd",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", email = "example@email.com"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      visitSubStatus = "BOOKED",
    )
    val bookingReference = pastDatedVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(pastDatedVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  @Test
  fun `when visit requested message is received but no visit contact found then request email is not sent`() {
    // Given
    val noContactVisit = createVisitDto(
      bookingReference = "bb-cc-dd-zz",
      visitDate = LocalDate.now(),
      visitTime = LocalTime.now().minusMinutes(1),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("John Smith", null, null),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      visitSubStatus = "BOOKED",
    )
    val bookingReference = noContactVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(noContactVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, null, HttpStatus.NOT_FOUND)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)

    // Then
    verifyEmailNotSent(visitAdditionalInfo)
  }

  private fun verifyEmailSent(templateId: String, visit: VisitDto, visitAdditionalInfo: VisitAdditionalInfo, templateVars: Map<String, Any>) {
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(visit, BOOKED, visitAdditionalInfo.eventAuditId) }
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
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(BOOKED, visitAdditionalInfo) }
    await untilAsserted { verify(emailSenderService, times(0)).sendEmail(any(), any(), any()) }
    await untilAsserted { verify(notificationClient, times(0)).sendEmail(any(), any(), any(), any()) }
    await untilAsserted { verify(visitSchedulerService, times(0)).createNotifyNotification(any()) }
  }
}
