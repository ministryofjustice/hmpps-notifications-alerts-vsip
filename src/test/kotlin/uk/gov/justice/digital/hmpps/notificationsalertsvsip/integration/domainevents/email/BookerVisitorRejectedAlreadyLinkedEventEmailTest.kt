package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.email

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestVisitorInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorRejectedAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.BOOKER_VISITOR_REJECTED
import java.time.LocalDate

class BookerVisitorRejectedAlreadyLinkedEventEmailTest : EventsIntegrationTestBase() {

  @Test
  fun `when visitor rejected (with reason already linked) event is received a visitor rejected email is sent to the booker`() {
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
      rejectionReason = "ALREADY_LINKED",
    )

    val templateId = templatesConfig.emailTemplates[EmailTemplateNames.BOOKER_VISITOR_REJECTED_ALREADY_LINKED.name]
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
    verifyBookerEmailSent(templateId!!, bookerInfo, VisitorRequestVisitorInfoDto(visitorRequest), templateVars)
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
    verifyBookerEmailNotSent()
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
    verifyBookerEmailNotSent()
  }

  private fun verifyBookerEmailSent(templateId: String, bookerInfoDto: BookerInfoDto, visitorInfo: VisitorRequestVisitorInfoDto, templateVars: Map<String, Any>) {
    await untilAsserted { verify(bookerVisitorRejectedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(bookerNotificationService, times(1)).sendVisitorRequestRejectedEmail(eq(BookerEventType.VISITOR_REJECTED), any()) }
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

  private fun verifyBookerEmailNotSent() {
    await untilAsserted { verify(bookerVisitorRejectedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(bookerNotificationService, times(0)).sendVisitorRequestRejectedEmail(eq(BookerEventType.VISITOR_REJECTED), any()) }
    await untilAsserted { verify(emailSenderService, times(0)).sendBookerVisitorEmail(any(), any(), any()) }
  }
}
