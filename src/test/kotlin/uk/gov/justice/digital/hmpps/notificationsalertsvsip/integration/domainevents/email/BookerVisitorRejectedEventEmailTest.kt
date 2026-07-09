package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.email

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestVisitorInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorRejectedAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.BOOKER_VISITOR_REJECTED
import java.time.LocalDate

class BookerVisitorRejectedEventEmailTest : EventsIntegrationTestBase() {

  @Test
  fun `when visitor rejected event is received a visitor rejected email is sent to the booker`() {
    // Given
    val prisonerId = "A1234BC"
    val bookerReference = "booker-ref"
    val bookerEmailAddress = "test@example.com"
    val firstName = "John"
    val lastName = "Smith"
    val visitorRequestReference = "abc-def-ghi"

    val visitorRequest = VisitorRequestDto(
      reference = visitorRequestReference,
      bookerReference = bookerReference,
      bookerEmail = bookerEmailAddress,
      prisonerId = prisonerId,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = LocalDate.now().minusYears(21),
      requestedOn = LocalDate.now(),
      visitorId = null,
      rejectionReason = "REJECT",
    )

    val templateId = notificationTemplateResolver.getEmailTemplate(EmailTemplateNames.BOOKER_VISITOR_REJECTED, LanguagePreference.EN)
    val templateVars = mapOf(
      "visitor" to "$firstName $lastName",
    )

    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorRejectedAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REJECTED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        bookerInfo.email,
        templateVars,
        null,
      ),
    ).thenReturn(buildSendEmailResponse(reference = "test"))

    bookerRegistryMockServer.stubGetVisitorRequestByReference(visitorRequestReference, visitorRequest)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    verifyBookerEmailSent(templateId, bookerAdditionalInfo, bookerInfo, VisitorRequestVisitorInfoDto(visitorRequest), templateVars)
  }

  @Test
  fun `when visitor rejected event is received but booker registry returns a NOT_FOUND error an email is not sent to the booker`() {
    // Given
    val visitorRequestReference = "abc-def-ghi"

    val bookerAdditionalInfo = VisitorRejectedAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REJECTED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    bookerRegistryMockServer.stubGetVisitorRequestByReference(visitorRequestReference, null, HttpStatus.NOT_FOUND)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    verifyBookerEmailNotSent(bookerAdditionalInfo)
  }

  @Test
  fun `when visitor rejected event is received but booker registry returns an INTERNAL_SERVER error an email is not sent to the booker`() {
    // Given
    val visitorRequestReference = "abc-def-ghi"

    val bookerAdditionalInfo = VisitorRejectedAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REJECTED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    bookerRegistryMockServer.stubGetVisitorRequestByReference(visitorRequestReference, null, HttpStatus.INTERNAL_SERVER_ERROR)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    verifyBookerEmailNotSent(bookerAdditionalInfo)
  }

  @Test
  fun `when visitor rejected message is received and language is welsh but no welsh template exists, then update message is sent in english`() {
    // Given
    val prisonerId = "A1234BC"
    val bookerReference = "booker-ref"
    val bookerEmailAddress = "test@example.com"
    val firstName = "John"
    val lastName = "Smith"
    val visitorRequestReference = "abc-def-ghi"

    val visitorRequest = VisitorRequestDto(
      reference = visitorRequestReference,
      bookerReference = bookerReference,
      bookerEmail = bookerEmailAddress,
      prisonerId = prisonerId,
      firstName = firstName,
      lastName = lastName,
      dateOfBirth = LocalDate.now().minusYears(21),
      requestedOn = LocalDate.now(),
      visitorId = null,
      rejectionReason = "REJECT",
    )

    val templateId = notificationTemplateResolver.getEmailTemplate(EmailTemplateNames.BOOKER_VISITOR_REJECTED, LanguagePreference.CY)
    val templateVars = mapOf(
      "visitor" to "$firstName $lastName",
    )

    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorRejectedAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REJECTED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        bookerInfo.email,
        templateVars,
        null,
      ),
    ).thenReturn(buildSendEmailResponse(reference = "test"))

    bookerRegistryMockServer.stubGetVisitorRequestByReference(visitorRequestReference, visitorRequest)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    verifyBookerEmailSent(templateId, bookerAdditionalInfo, bookerInfo, VisitorRequestVisitorInfoDto(visitorRequest), templateVars)
  }

  private fun verifyBookerEmailSent(templateId: String, additionalInfo: VisitorRejectedAdditionalInfo, bookerInfoDto: BookerInfoDto, visitorInfo: VisitorRequestVisitorInfoDto, templateVars: Map<String, Any>) {
    await untilAsserted { verify(bookerVisitorRejectedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(bookerNotificationService, times(1)).sendVisitorRequestRejectedEmail(additionalInfo) }
    await untilAsserted { verify(emailSenderService, times(1)).sendBookerVisitorEmail(bookerInfoDto, visitorInfo, BookerEventType.VISITOR_REJECTED) }

    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        bookerInfoDto.email,
        templateVars,
        null,
      )
    }
  }

  private fun verifyBookerEmailNotSent(additionalInfo: VisitorRejectedAdditionalInfo) {
    await untilAsserted { verify(bookerVisitorRejectedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(bookerNotificationService, times(1)).sendVisitorRequestRejectedEmail(additionalInfo) }
    await untilAsserted { verify(emailSenderService, times(0)).sendBookerVisitorEmail(any(), any(), any()) }
  }
}
