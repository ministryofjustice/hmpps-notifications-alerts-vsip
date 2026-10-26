package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationContext
import org.springframework.mock.env.MockEnvironment
import tools.jackson.module.kotlin.jacksonObjectMapper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCreateNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.VisitSchedulerService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.SQSMessage
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CHANGED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_REQUEST_APPROVED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PrisonVisitBookedEventNotifier
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PrisonVisitCancelledEventNotifier
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PrisonVisitChangedEventNotifier
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PrisonVisitRequestApprovedEventNotifier
import java.time.LocalDateTime

class VisitEventConsumptionTest {
  private val objectMapper = jacksonObjectMapper()
  private val context: ApplicationContext = mock()
  private val visitSchedulerService: VisitSchedulerService = mock()
  private val smsSenderService: SmsSenderService = mock()
  private val emailSenderService: EmailSenderService = mock()
  private val replyToEmailResolver: ReplyToEmailResolver = mock()
  private val visitNotificationService = VisitNotificationService(
    visitSchedulerService,
    smsSenderService,
    emailSenderService,
    replyToEmailResolver,
  )
  private val listener = DomainEventListenerService(context, objectMapper, EventFeatureSwitch(MockEnvironment(), true))

  @ParameterizedTest
  @ValueSource(strings = [PRISON_VISIT_BOOKED, PRISON_VISIT_CHANGED, PRISON_VISIT_CANCELLED, PRISON_VISIT_REQUEST_APPROVED])
  fun `video visit events are acknowledged without downstream calls or notifications`(eventType: String) {
    consumeEvent(eventType, "SOCIAL_VIDEO")

    verifyNoInteractions(visitSchedulerService, smsSenderService, emailSenderService, replyToEmailResolver)
  }

  @ParameterizedTest
  @CsvSource(
    "$PRISON_VISIT_BOOKED, BOOKED, SOCIAL",
    "$PRISON_VISIT_CHANGED, UPDATED, SOCIAL",
    "$PRISON_VISIT_CANCELLED, CANCELLED, SOCIAL",
    "$PRISON_VISIT_REQUEST_APPROVED, REQUEST_APPROVED, SOCIAL",
    "$PRISON_VISIT_BOOKED, BOOKED,",
    "$PRISON_VISIT_CHANGED, UPDATED,",
    "$PRISON_VISIT_CANCELLED, CANCELLED,",
    "$PRISON_VISIT_REQUEST_APPROVED, REQUEST_APPROVED,",
  )
  fun `social and legacy events retain SMS and email notifications and records`(eventType: String, expectedType: VisitEventType, visitType: String?) {
    val start = LocalDateTime.now().plusDays(1)
    val visit = VisitDto(
      reference = "ab-cd-ef-gh",
      prisonerId = "A1234BC",
      prisonCode = "HEI",
      startTimestamp = start,
      endTimestamp = start.plusHours(1),
      visitContact = ContactDto(name = "Test Visitor", telephone = "07700900000", email = "visitor@example.com"),
      visitRestriction = VisitRestriction.OPEN,
      visitExternalSystemDetails = null,
      visitSubStatus = "AUTO_APPROVED",
    )
    val smsNotification: NotifyCreateNotificationDto = mock()
    val emailNotification: NotifyCreateNotificationDto = mock()
    whenever(visitSchedulerService.getVisit(visit.reference)).thenReturn(visit)
    whenever(replyToEmailResolver.getReplyToEmailIdForPrison(visit.prisonCode)).thenReturn("reply-to-id")
    whenever(smsSenderService.sendVisitsSms(visit, expectedType, "42")).thenReturn(smsNotification)
    whenever(emailSenderService.sendVisitsEmail(visit, expectedType, "42", "reply-to-id")).thenReturn(emailNotification)

    consumeEvent(eventType, visitType)

    verify(visitSchedulerService).getVisit(visit.reference)
    verify(smsSenderService).sendVisitsSms(visit, expectedType, "42")
    verify(emailSenderService).sendVisitsEmail(visit, expectedType, "42", "reply-to-id")
    verify(replyToEmailResolver).getReplyToEmailIdForPrison(visit.prisonCode)
    verify(visitSchedulerService).createNotifyNotification(smsNotification)
    verify(visitSchedulerService).createNotifyNotification(emailNotification)
    verifyNoMoreInteractions(visitSchedulerService, smsSenderService, emailSenderService, replyToEmailResolver)
  }

  private fun consumeEvent(eventType: String, visitType: String?) {
    val notifier = when (eventType) {
      PRISON_VISIT_BOOKED -> PrisonVisitBookedEventNotifier(visitNotificationService, objectMapper)
      PRISON_VISIT_CHANGED -> PrisonVisitChangedEventNotifier(visitNotificationService, objectMapper)
      PRISON_VISIT_CANCELLED -> PrisonVisitCancelledEventNotifier(visitNotificationService, objectMapper)
      PRISON_VISIT_REQUEST_APPROVED -> PrisonVisitRequestApprovedEventNotifier(visitNotificationService, objectMapper)
      else -> error("Unexpected test event type: $eventType")
    }
    whenever(context.containsBean(eventType)).thenReturn(true)
    whenever(context.getBean(eventType)).thenReturn(notifier)

    val additionalInformation = mutableMapOf<String, Any>("reference" to "ab-cd-ef-gh", "eventAuditId" to 42)
    visitType?.let { additionalInformation["visitType"] = it }
    val domainEvent = objectMapper.writeValueAsString(
      mapOf("eventType" to eventType, "additionalInformation" to additionalInformation),
    )
    val message = SQSMessage(type = "Notification", messageId = "test-message", message = domainEvent)

    listener.onDomainEvent(objectMapper.writeValueAsString(message)).join()

    verify(context).getBean(eventType)
  }
}
