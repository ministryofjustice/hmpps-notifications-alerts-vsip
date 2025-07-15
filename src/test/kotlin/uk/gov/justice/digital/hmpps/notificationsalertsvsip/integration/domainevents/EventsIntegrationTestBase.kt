package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.json.JSONObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.ContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitExternalSystemDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitorDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.helper.JwtAuthHelper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock.HmppsAuthExtension
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock.PrisonRegisterMockServer
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock.PrisonerContactRegistryMockServer
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock.PrisonerOffenderSearchMockServer
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock.VisitSchedulerMockServer
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.DomainEventListenerService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.EmailSenderService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.NotificationService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.PRISON_VISITS_NOTIFICATION_ALERTS_QUEUE_CONFIG_KEY
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.SmsSenderService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.VisitSchedulerService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.DomainEvent
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.SQSMessage
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PrisonVisitBookedEventNotifier
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PrisonVisitCancelledEventNotifier
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PrisonVisitChangedEventNotifier
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.SendEmailResponse
import uk.gov.service.notify.SendSmsResponse
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(HmppsAuthExtension::class)
abstract class EventsIntegrationTestBase {

  companion object {
    const val EXPECTED_DATE_PATTERN = "d MMMM yyyy"
    private val localStackContainer = LocalStackContainer.instance
    private val objectMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())
    val visitSchedulerMockServer = VisitSchedulerMockServer(objectMapper)
    val prisonRegisterMockServer = PrisonRegisterMockServer(objectMapper)
    val prisonerContactRegisterMockServer = PrisonerContactRegistryMockServer(objectMapper)
    val prisonerOffenderSearchMockServer = PrisonerOffenderSearchMockServer(objectMapper)

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      visitSchedulerMockServer.start()
      prisonRegisterMockServer.start()
      prisonerContactRegisterMockServer.start()
      prisonerOffenderSearchMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      visitSchedulerMockServer.stop()
      prisonRegisterMockServer.stop()
      prisonerContactRegisterMockServer.stop()
      prisonerOffenderSearchMockServer.stop()
    }
  }

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var domainEventListenerService: DomainEventListenerService

  @MockitoSpyBean
  lateinit var eventFeatureSwitch: EventFeatureSwitch

  @MockitoSpyBean
  lateinit var notificationService: NotificationService

  @MockitoSpyBean
  lateinit var smsSenderService: SmsSenderService

  @MockitoSpyBean
  lateinit var emailSenderService: EmailSenderService

  @MockitoSpyBean
  lateinit var visitSchedulerService: VisitSchedulerService

  @MockitoSpyBean
  lateinit var templatesConfig: TemplatesConfig

  @MockitoSpyBean
  lateinit var prisonVisitBookedEventNotifierSpy: PrisonVisitBookedEventNotifier

  @MockitoSpyBean
  lateinit var prisonVisitChangedEventNotifierSpy: PrisonVisitChangedEventNotifier

  @MockitoSpyBean
  lateinit var prisonVisitCancelledEventNotifierSpy: PrisonVisitCancelledEventNotifier

  @MockitoBean
  lateinit var notificationClient: NotificationClient

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var hmppsQueueService: HmppsQueueService

  internal val topic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  internal val prisonVisitsEventsQueue by lazy { hmppsQueueService.findByQueueId(PRISON_VISITS_NOTIFICATION_ALERTS_QUEUE_CONFIG_KEY) as HmppsQueue }

  internal val sqsClient by lazy { prisonVisitsEventsQueue.sqsClient }
  internal val sqsDlqClient by lazy { prisonVisitsEventsQueue.sqsDlqClient }
  internal val queueUrl by lazy { prisonVisitsEventsQueue.queueUrl }
  internal val dlqUrl by lazy { prisonVisitsEventsQueue.dlqUrl }

  internal val awsSnsClient by lazy { topic.snsClient }
  internal val topicArn by lazy { topic.arn }

  lateinit var roleVisitSchedulerHttpHeaders: (HttpHeaders) -> Unit

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @BeforeEach
  fun cleanQueue() {
    purgeQueue(sqsClient, queueUrl)
    purgeQueue(sqsDlqClient!!, dlqUrl!!)
    roleVisitSchedulerHttpHeaders = setAuthorisation(roles = listOf("ROLE_VISIT_SCHEDULER"))
  }

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  fun purgeQueue(client: SqsAsyncClient, url: String) {
    client.purgeQueue(PurgeQueueRequest.builder().queueUrl(url).build()).get()
  }

  fun createDomainEvent(eventType: String, additionalInformation: String = "test"): DomainEvent = DomainEvent(eventType = eventType, additionalInformation)

  fun createSQSMessage(domainEventJson: String): String {
    val sqaMessage = SQSMessage(type = "Notification", messageId = "123", message = domainEventJson)
    return objectMapper.writeValueAsString(sqaMessage)
  }

  fun createDomainEventPublishRequest(eventType: String, domainEvent: String): PublishRequest? = PublishRequest.builder()
    .topicArn(topicArn)
    .message(domainEvent).build()

  fun createDomainEventPublishRequest(eventType: String): PublishRequest? = PublishRequest.builder()
    .topicArn(topicArn)
    .message(objectMapper.writeValueAsString(createDomainEvent(eventType, ""))).build()

  fun createDomainEventJson(eventType: String, additionalInformation: String): String = "{\"eventType\":\"$eventType\",\"additionalInformation\":$additionalInformation}"

  fun createAdditionalInformationJson(visitAdditionalInfo: VisitAdditionalInfo): String {
    val builder = StringBuilder()
    builder.append("{")
    builder.append("\"reference\":\"${visitAdditionalInfo.bookingReference}\",")
    builder.append("\"eventAuditId\":\"${visitAdditionalInfo.eventAuditId}\"")
    builder.append("}")
    return builder.toString()
  }

  fun createVisitDto(
    bookingReference: String,
    prisonCode: String = "HEI",
    prisonerId: String = "AA123456",
    visitDate: LocalDate,
    visitTime: LocalTime,
    duration: Duration,
    visitContact: ContactDto,
    visitRestriction: VisitRestriction = VisitRestriction.OPEN,
    visitors: List<VisitorDto>,
    outcomeStatus: String? = null,
    externalSystemDetailsDto: VisitExternalSystemDetailsDto? = null,
    visitSubStatus: String,
  ): VisitDto = VisitDto(
    reference = bookingReference,
    prisonCode = prisonCode,
    startTimestamp = visitDate.atTime(visitTime),
    endTimestamp = visitDate.atTime(visitTime).plus(duration),
    visitContact = visitContact,
    prisonerId = prisonerId,
    visitRestriction = visitRestriction,
    visitors = visitors,
    outcomeStatus = outcomeStatus,
    visitExternalSystemDetails = externalSystemDetailsDto,
    visitSubStatus = visitSubStatus,
  )

  fun buildSendEmailResponse(
    id: UUID = UUID.randomUUID(),
    reference: String?,
    body: String = "Hello, {{name}}!",
    subject: String = "Test Subject",
    fromEmail: String? = "no-reply@example.com",
    templateId: UUID = UUID.randomUUID(),
    templateVersion: Int = 1,
    templateUri: String = "https://example.com/template/template-id",
    oneClickUnsubscribeURL: String? = "https://example.com/unsubscribe",
  ): SendEmailResponse {
    val jsonResponse = JSONObject().apply {
      put("id", id.toString())
      put("reference", reference)
      put(
        "content",
        JSONObject().apply {
          put("body", body)
          put("subject", subject)
          put("from_email", fromEmail)
        },
      )

      put(
        "template",
        JSONObject().apply {
          put("id", templateId.toString())
          put("version", templateVersion)
          put("uri", templateUri)
        },
      )

      put("one_click_unsubscribe_url", oneClickUnsubscribeURL)
    }.toString()

    return SendEmailResponse(jsonResponse)
  }

  fun buildSendSmsResponse(
    id: UUID = UUID.randomUUID(),
    reference: String?,
    body: String = "Hello, {{name}}!",
    fromNumber: String? = "no-reply@example.com",
    templateId: UUID = UUID.randomUUID(),
    templateVersion: Int = 1,
    templateUri: String = "https://example.com/template/template-id",
  ): SendSmsResponse {
    val jsonResponse = JSONObject().apply {
      put("id", id.toString())
      put("reference", reference)
      put(
        "content",
        JSONObject().apply {
          put("body", body)
          put("from_number", fromNumber)
        },
      )

      put(
        "template",
        JSONObject().apply {
          put("id", templateId.toString())
          put("version", templateVersion)
          put("uri", templateUri)
        },
      )
    }.toString()

    return SendSmsResponse(jsonResponse)
  }
}
