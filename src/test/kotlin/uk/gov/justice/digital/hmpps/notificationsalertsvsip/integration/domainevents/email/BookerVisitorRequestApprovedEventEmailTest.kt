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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorRequestAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.BOOKER_VISITOR_REQUEST_APPROVED
import java.time.LocalDate

class BookerVisitorRequestApprovedEventEmailTest : EventsIntegrationTestBase() {

  @Test
  fun `when visitor request approved event is received a visitor approved email is sent to the booker`() {
    // Given
    val prisonerId = "A1234BC"
    val bookerReference = "booker-ref"
    val bookerEmailAddress = "test@example.com"
    val visitorRequestReference = "abc-def-ghi"
    val visitorRequest = createVisitorRequest(
      visitorRequestReference = visitorRequestReference,
      bookerReference = bookerReference,
      bookerEmailAddress = bookerEmailAddress,
      prisonerId = prisonerId,
    )
    val templateId = notificationTemplateResolver.getEmailTemplate(EmailTemplateNames.BOOKER_VISITOR_APPROVED, LanguagePreference.EN)
    val templateVars = mapOf(
      "visitor" to "${visitorRequest.firstName} ${visitorRequest.lastName}",
    )
    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorRequestAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REQUEST_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    Mockito.`when`(
      notificationClient.sendEmail(
        templateId,
        bookerInfo.email,
        templateVars,
        null,
        "00000000-0000-0000-0000-000000000001",
      ),
    ).thenReturn(buildSendEmailResponse(reference = "test"))

    // When
    bookerRegistryMockServer.stubGetVisitorRequestByReference(visitorRequestReference, visitorRequest)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    await untilAsserted { verify(bookerVisitorRequestApprovedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitorRequestNotificationService, times(1)).sendVisitorRequestApprovedEmail(bookerAdditionalInfo) }
    await untilAsserted {
      verify(emailSenderService, times(1)).sendBookerVisitorEmail(
        bookerInfo,
        VisitorRequestVisitorInfoDto(visitorRequest),
        BookerEventType.VISITOR_APPROVED,
        "00000000-0000-0000-0000-000000000001",
      )
    }
    await untilAsserted {
      verify(notificationClient, times(1)).sendEmail(
        templateId,
        bookerInfo.email,
        templateVars,
        null,
        "00000000-0000-0000-0000-000000000001",
      )
    }
  }

  @Test
  fun `when visitor request approved event is received but booker registry returns a NOT_FOUND error an email is not sent to the booker`() {
    // Given
    val visitorRequestReference = "abc-def-ghi"
    val bookerAdditionalInfo = VisitorRequestAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REQUEST_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    bookerRegistryMockServer.stubGetVisitorRequestByReference(visitorRequestReference, null, HttpStatus.NOT_FOUND)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    verifyVisitorRequestApprovedEmailNotSent(bookerAdditionalInfo)
  }

  @Test
  fun `when visitor request approved event is received but booker registry returns an INTERNAL_SERVER error an email is not sent to the booker`() {
    // Given
    val visitorRequestReference = "abc-def-ghi"
    val bookerAdditionalInfo = VisitorRequestAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REQUEST_APPROVED, createAdditionalInformationJson(bookerAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    // When
    bookerRegistryMockServer.stubGetVisitorRequestByReference(visitorRequestReference, null, HttpStatus.INTERNAL_SERVER_ERROR)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    verifyVisitorRequestApprovedEmailNotSent(bookerAdditionalInfo)
  }

  private fun verifyVisitorRequestApprovedEmailNotSent(additionalInfo: VisitorRequestAdditionalInfo) {
    await untilAsserted { verify(bookerVisitorRequestApprovedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitorRequestNotificationService, times(1)).sendVisitorRequestApprovedEmail(additionalInfo) }
    await untilAsserted { verify(emailSenderService, times(0)).sendBookerVisitorEmail(any(), any(), any(), any()) }
  }

  private fun createVisitorRequest(
    visitorRequestReference: String,
    bookerReference: String,
    bookerEmailAddress: String,
    prisonerId: String,
  ): VisitorRequestDto = VisitorRequestDto(
    reference = visitorRequestReference,
    bookerReference = bookerReference,
    bookerEmail = bookerEmailAddress,
    prisonerId = prisonerId,
    firstName = "John",
    lastName = "Smith",
    dateOfBirth = LocalDate.now().minusYears(21),
    requestedOn = LocalDate.now(),
    visitorId = null,
    rejectionReason = null,
  )
}
