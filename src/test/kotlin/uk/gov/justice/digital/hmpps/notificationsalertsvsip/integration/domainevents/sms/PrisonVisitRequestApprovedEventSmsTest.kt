package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.sms

import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType.REQUEST_APPROVED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_REQUEST_APPROVED
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class PrisonVisitRequestApprovedEventSmsTest : EventsIntegrationTestBase() {
  lateinit var approvedVisit: VisitDto
  lateinit var prison: PrisonDto

  @BeforeEach
  internal fun setUp() {
    approvedVisit = createVisitDto(
      bookingReference = "approved-visit",
      visitDate = LocalDate.now().plusMonths(1),
      visitTime = LocalTime.of(10, 30),
      duration = Duration.of(30, ChronoUnit.MINUTES),
      visitContact = ContactDto("Contact One", "01234567890"),
      visitors = listOf(VisitorDto(1234), VisitorDto(9876)),
      visitSubStatus = "APPROVED",
    )

    prison = PrisonDto("HEI", "Hewell", true)
  }

  @Test
  fun `when visit request approved event received then visit booked sms is sent`() {
    // Given
    val bookingReference = approvedVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(approvedVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_REQUEST_APPROVED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = notificationTemplateResolver.getSmsTemplate(SmsTemplateNames.VISIT_BOOKING_OR_REQUEST_APPROVED, LanguagePreference.EN)
    val visitDate = approvedVisit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "ref number" to bookingReference,
    )

    // When
    visitSchedulerMockServer.stubGetVisit(bookingReference, approvedVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prison)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    await untilAsserted { verify(prisonVisitRequestApprovedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitNotificationService, times(1)).sendMessage(REQUEST_APPROVED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendVisitsSms(approvedVisit, REQUEST_APPROVED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        approvedVisit.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
  }

  @Test
  fun `when visit request approved event received in welsh then visit booked sms is sent with welsh template vars`() {
    // Given
    val welshApprovedVisit = approvedVisit.copy(visitContact = approvedVisit.visitContact.copy(languagePreference = LanguagePreference.CY))
    val prisonWithWelshName = prison.copy(prisonNameInWelsh = "Carchar Hewell")
    val bookingReference = welshApprovedVisit.reference
    val visitAdditionalInfo = VisitAdditionalInfo(welshApprovedVisit.reference, "123456")
    val domainEvent = createDomainEventJson(PRISON_VISIT_REQUEST_APPROVED, createAdditionalInformationJson(visitAdditionalInfo))
    val jsonSqsMessage = createSQSMessage(domainEvent)

    val templateId = notificationTemplateResolver.getSmsTemplate(SmsTemplateNames.VISIT_BOOKING_OR_REQUEST_APPROVED, LanguagePreference.CY)
    val visitDate = welshApprovedVisit.startTimestamp.toLocalDate()
    val expectedVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN))
    val expectedWelshVisitDate = visitDate.format(DateTimeFormatter.ofPattern(EXPECTED_DATE_PATTERN, Locale.forLanguageTag("cy-GB")))
    val expectedDayOfWeek = visitDate.dayOfWeek.toString().lowercase().replaceFirstChar { it.titlecase() }
    val expectedWelshDayOfWeek = visitDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("cy-GB")))
    val templateVars = mutableMapOf<String, Any>(
      "prison" to prison.prisonName,
      "time" to "10:30am",
      "dayofweek" to expectedDayOfWeek,
      "date" to expectedVisitDate,
      "ref number" to bookingReference,
      "prison_cy" to prisonWithWelshName.prisonNameInWelsh!!,
      "dayofweek_cy" to expectedWelshDayOfWeek,
      "date_cy" to expectedWelshVisitDate,
    )

    // When
    visitSchedulerMockServer.stubGetVisit(bookingReference, welshApprovedVisit)
    prisonRegisterMockServer.stubGetPrison(prison.prisonId, prisonWithWelshName)
    domainEventListenerService.onDomainEvent(jsonSqsMessage)

    // Then
    await untilAsserted { verify(prisonVisitRequestApprovedEventNotifierSpy, times(1)).processEvent(any()) }
    await untilAsserted { verify(visitNotificationService, times(1)).sendMessage(REQUEST_APPROVED, visitAdditionalInfo) }
    await untilAsserted { verify(smsSenderService, times(1)).sendVisitsSms(welshApprovedVisit, REQUEST_APPROVED, visitAdditionalInfo.eventAuditId) }
    await untilAsserted {
      verify(notificationClient, times(1)).sendSms(
        templateId,
        welshApprovedVisit.visitContact.telephone,
        templateVars,
        visitAdditionalInfo.eventAuditId,
      )
    }
  }
}
