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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestVisitorInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorApprovedAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.BOOKER_VISITOR_APPROVED
import java.time.LocalDate

class BookerVisitorApprovedEventEmailTest : EventsIntegrationTestBase() {
  private val prisonerId = "A1234BC"
  private val bookerReference = "booker-ref"
  private val bookerEmailAddress = "test@example.com"
  private val contact1 = PrisonerContactRegistryContactDto("1234", "Visitor", "One", (LocalDate.now().minusYears(30)))
  private val contact2 = PrisonerContactRegistryContactDto("9876", "Visitor", "Two")
  private lateinit var prisonerContactsResult: List<PrisonerContactRegistryContactDto>

  @BeforeEach
  internal fun setUp() {
    prisonerContactsResult = listOf(contact1, contact2)
  }

  @Test
  fun `when visitor approved event is received a visitor approved email is sent to the booker`() {
    // Given
    val visitorId = 1234L
    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.BOOKER_VISITOR_APPROVED.name]
    val templateVars = mapOf(
      "visitor" to contact1.firstName + " " + contact1.lastName,
    )

    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorApprovedAdditionalInfo(bookerReference, prisonerId, visitorId.toString())
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        bookerInfo.email,
        templateVars,
        null,
      ),
    ).thenReturn(buildSendEmailResponse(reference = "test"))

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    bookerRegistryMockServer.stubGetBooker(bookerReference, bookerInfo)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(prisonerId, prisonerContactsResult)

    // Then
    verifyBookerEmailSent(templateId!!, bookerAdditionalInfo, bookerInfo, VisitorRequestVisitorInfoDto(contact1), templateVars)
  }

  @Test
  fun `when visitor approved event is received but visitor is not on the social contacts an email is not sent to the booker`() {
    // Given
    // visitor does not exist on the prisoner's social contacts list
    val visitorId = 9999L

    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorApprovedAdditionalInfo(bookerReference, prisonerId, visitorId.toString())
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    bookerRegistryMockServer.stubGetBooker(bookerReference, bookerInfo)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(prisonerId, prisonerContactsResult)

    // Then
    verifyBookerEmailNotSent(bookerAdditionalInfo)
  }

  @Test
  fun `when visitor approved event is received but prisoner contact search returns a NOT_FOUND error an email is not sent to the booker`() {
    // Given
    // visitor does not exist on the prisoner's social contacts list
    val visitorId = 9999L

    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorApprovedAdditionalInfo(bookerReference, prisonerId, visitorId.toString())
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    bookerRegistryMockServer.stubGetBooker(bookerReference, bookerInfo)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(prisonerId, null, HttpStatus.NOT_FOUND)

    // Then
    verifyBookerEmailNotSent(bookerAdditionalInfo)
  }

  @Test
  fun `when visitor approved event is received but prisoner contact search returns an INTERNAL_SERVER error an email is not sent to the booker`() {
    // Given
    // visitor does not exist on the prisoner's social contacts list
    val visitorId = 9999L

    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorApprovedAdditionalInfo(bookerReference, prisonerId, visitorId.toString())
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    bookerRegistryMockServer.stubGetBooker(bookerReference, bookerInfo)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(prisonerId, null, HttpStatus.INTERNAL_SERVER_ERROR)

    // Then
    verifyBookerEmailNotSent(bookerAdditionalInfo)
  }

  @Test
  fun `when visitor approved event is received but booker registry returns a NOT_FOUND error an email is not sent to the booker`() {
    // Given
    // visitor does not exist on the prisoner's social contacts list
    val visitorId = 9999L

    val bookerAdditionalInfo = VisitorApprovedAdditionalInfo(bookerReference, prisonerId, visitorId.toString())
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    bookerRegistryMockServer.stubGetBooker(bookerReference, null, HttpStatus.NOT_FOUND)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(prisonerId, prisonerContactsResult)

    // Then
    verifyBookerEmailNotSent(bookerAdditionalInfo)
  }

  @Test
  fun `when visitor approved event is received but booker registry returns an INTERNAL_SERVER error an email is not sent to the booker`() {
    // Given
    // visitor does not exist on the prisoner's social contacts list
    val visitorId = 9999L

    val bookerAdditionalInfo = VisitorApprovedAdditionalInfo(bookerReference, prisonerId, visitorId.toString())
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    domainEventListenerService.onDomainEvent(jsonSqsMessage)
    bookerRegistryMockServer.stubGetBooker(bookerReference, null, HttpStatus.INTERNAL_SERVER_ERROR)
    prisonerContactRegisterMockServer.stubGetPrisonersSocialContacts(prisonerId, prisonerContactsResult)

    // Then
    verifyBookerEmailNotSent(bookerAdditionalInfo)
  }

  private fun verifyBookerEmailSent(templateId: String, additionalInfo: VisitorApprovedAdditionalInfo, bookerInfoDto: BookerInfoDto, visitorInfo: VisitorRequestVisitorInfoDto, templateVars: Map<String, Any>) {
    await untilAsserted { verify(bookerVisitorApprovedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(bookerNotificationService, times(1)).sendVisitorRequestApprovedEmail(BookerEventType.VISITOR_APPROVED, additionalInfo) }
    await untilAsserted { verify(emailSenderService, times(1)).sendBookerVisitorEmail(bookerInfoDto, visitorInfo, BookerEventType.VISITOR_APPROVED) }

    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        bookerInfoDto.email,
        templateVars,
        null,
      )
    }
  }

  private fun verifyBookerEmailNotSent(additionalInfo: VisitorApprovedAdditionalInfo) {
    await untilAsserted { verify(bookerVisitorApprovedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(bookerNotificationService, times(1)).sendVisitorRequestApprovedEmail(BookerEventType.VISITOR_APPROVED, additionalInfo) }
    await untilAsserted { verify(emailSenderService, times(0)).sendBookerVisitorEmail(any(), any(), any()) }
  }
}
