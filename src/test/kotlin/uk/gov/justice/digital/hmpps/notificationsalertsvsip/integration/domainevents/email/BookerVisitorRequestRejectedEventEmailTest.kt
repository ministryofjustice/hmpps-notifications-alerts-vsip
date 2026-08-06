package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.email

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestVisitorInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorRequestAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.BOOKER_VISITOR_REQUEST_REJECTED
import java.time.LocalDate

class BookerVisitorRequestRejectedEventEmailTest : EventsIntegrationTestBase() {

  @Test
  fun `when visitor request rejected event is received a visitor rejected email is sent to the booker`() {
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
      rejectionReason = "REJECT",
    )
    val templateId = notificationTemplateResolver.getEmailTemplate(EmailTemplateNames.BOOKER_VISITOR_REJECTED, LanguagePreference.EN)
    val templateVars = mapOf(
      "visitor" to "${visitorRequest.firstName} ${visitorRequest.lastName}",
    )
    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorRequestAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REQUEST_REJECTED, createAdditionalInformationJson(bookerAdditionalInfo))
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
    await untilAsserted { verify(bookerVisitorRequestRejectedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitorRequestNotificationService, times(1)).sendVisitorRequestRejectedEmail(bookerAdditionalInfo) }
    await untilAsserted {
      verify(emailSenderService, times(1)).sendBookerVisitorEmail(
        bookerInfo,
        VisitorRequestVisitorInfoDto(visitorRequest),
        BookerEventType.VISITOR_REJECTED,
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
  fun `when visitor request rejected event is received with a welsh language preference a welsh visitor rejected email is sent to the booker`() {
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
      rejectionReason = "REJECT",
      languagePreference = LanguagePreference.CY,
    )
    val templateId = notificationTemplateResolver.getEmailTemplate(EmailTemplateNames.BOOKER_VISITOR_REJECTED, LanguagePreference.CY)
    val templateVars = mapOf(
      "visitor" to "${visitorRequest.firstName} ${visitorRequest.lastName}",
    )
    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorRequestAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REQUEST_REJECTED, createAdditionalInformationJson(bookerAdditionalInfo))
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
    await untilAsserted { verify(bookerVisitorRequestRejectedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitorRequestNotificationService, times(1)).sendVisitorRequestRejectedEmail(bookerAdditionalInfo) }
    await untilAsserted {
      verify(emailSenderService, times(1)).sendBookerVisitorEmail(
        bookerInfo,
        VisitorRequestVisitorInfoDto(visitorRequest),
        BookerEventType.VISITOR_REJECTED,
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
  fun `when visitor request rejected event is received with reason already linked a visitor rejected already linked email is sent to the booker`() {
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
      rejectionReason = "ALREADY_LINKED",
    )
    val templateId = notificationTemplateResolver.getEmailTemplate(EmailTemplateNames.BOOKER_VISITOR_REJECTED_ALREADY_LINKED, LanguagePreference.EN)
    val templateVars = mapOf(
      "visitor" to "${visitorRequest.firstName} ${visitorRequest.lastName}",
    )
    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorRequestAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REQUEST_REJECTED, createAdditionalInformationJson(bookerAdditionalInfo))
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
    await untilAsserted { verify(bookerVisitorRequestRejectedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitorRequestNotificationService, times(1)).sendVisitorRequestRejectedEmail(bookerAdditionalInfo) }
    await untilAsserted {
      verify(emailSenderService, times(1)).sendBookerVisitorEmail(
        bookerInfo,
        VisitorRequestVisitorInfoDto(visitorRequest),
        BookerEventType.VISITOR_REJECTED_ALREADY_LINKED,
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
  fun `when visitor request rejected already linked event is received with a welsh language preference a welsh visitor rejected already linked email is sent to the booker`() {
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
      rejectionReason = "ALREADY_LINKED",
      languagePreference = LanguagePreference.CY,
    )
    val templateId = notificationTemplateResolver.getEmailTemplate(EmailTemplateNames.BOOKER_VISITOR_REJECTED_ALREADY_LINKED, LanguagePreference.CY)
    val templateVars = mapOf(
      "visitor" to "${visitorRequest.firstName} ${visitorRequest.lastName}",
    )
    val bookerInfo = BookerInfoDto(bookerReference, bookerEmailAddress)
    val bookerAdditionalInfo = VisitorRequestAdditionalInfo(visitorRequestReference)
    val domainEvent = createDomainEventJson(BOOKER_VISITOR_REQUEST_REJECTED, createAdditionalInformationJson(bookerAdditionalInfo))
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
    await untilAsserted { verify(bookerVisitorRequestRejectedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitorRequestNotificationService, times(1)).sendVisitorRequestRejectedEmail(bookerAdditionalInfo) }
    await untilAsserted {
      verify(emailSenderService, times(1)).sendBookerVisitorEmail(
        bookerInfo,
        VisitorRequestVisitorInfoDto(visitorRequest),
        BookerEventType.VISITOR_REJECTED_ALREADY_LINKED,
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

  private fun createVisitorRequest(
    visitorRequestReference: String,
    bookerReference: String,
    bookerEmailAddress: String,
    prisonerId: String,
    rejectionReason: String,
    languagePreference: LanguagePreference = LanguagePreference.EN,
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
    rejectionReason = rejectionReason,
    languagePreference = languagePreference,
  )
}
