package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.email

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.personalisations.PrisonerVisitorPersonalisationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonContactDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// TODO VB-4332: Implement tests

class PrisonVisitBookedEventEmailTest : EventsIntegrationTestBase() {
  lateinit var visit: VisitDto
  lateinit var prison: PrisonDto
  lateinit var prisonerSearchResult: PrisonerSearchResultDto
  lateinit var prisonerContactsResult: List<PrisonerContactRegistryContactDto>
  lateinit var prisonContactDetailsDto: PrisonContactDetailsDto
  lateinit var prisonVisitors: List<PrisonerVisitorPersonalisationDto>

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
      PrisonerContactRegistryContactDto("1234", "Visitor", "One", LocalDate.of(1995, 1, 1)),
      PrisonerContactRegistryContactDto("9876", "Visitor", "Two"),
    )

    prisonVisitors = listOf(
      PrisonerVisitorPersonalisationDto("Visitor", "One", "Age: 29"),
      PrisonerVisitorPersonalisationDto("Visitor", "Two", "Age: Unknown"),
    )

    prisonContactDetailsDto = PrisonContactDetailsDto(phoneNumber = "0111222333")
  }

  @Test
  fun `when visit booked message is received then booking email is sent`() {
    // Given
    val bookingReference = visit.reference

    val domainEvent = createDomainEventJson(PRISON_VISIT_BOOKED, createAdditionalInformationJson(bookingReference))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.VISIT_BOOKING.name]
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
      "phone" to prisonContactDetailsDto.phoneNumber!!,
      "prisoner" to prisonerSearchResult.firstName + " " + prisonerSearchResult.lastName,
      "visitors" to prisonVisitors,
    )

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    visitSchedulerMockServer.stubGetVisit(bookingReference, visit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    prisonerOffenderSearchMockServer.stubGetPrisoner(visit.prisonerId, prisonerSearchResult)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(visit.prisonerId, prisonerContactsResult)
    prisonRegisterMockServer.stubGetPrisonSocialVisitContactDetails(prison.prisonId, prisonContactDetailsDto)

    // Then
    await untilAsserted { verify(prisonVisitBookedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(notificationService, times(1)).sendMessage(VisitEventType.BOOKED, "bi-vn-wn-ml") }
    await untilAsserted { verify(emailSenderService, times(1)).sendEmail(visit, VisitEventType.BOOKED) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        visit.visitContact.email,
        templateVars,
        visit.reference,
      )
    }
  }
}
