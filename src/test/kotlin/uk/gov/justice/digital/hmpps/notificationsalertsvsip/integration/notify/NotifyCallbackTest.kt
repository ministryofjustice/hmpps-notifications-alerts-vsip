package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.notify

import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCallbackNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents.EventsIntegrationTestBase
import java.time.LocalDateTime
import java.util.UUID

class NotifyCallbackTest : EventsIntegrationTestBase() {
  @Test
  fun `should process callback successfully with valid token`() {
    // Given
    val validToken = "test-valid-token"
    val dto = NotifyCallbackNotificationDto(
      notificationId = UUID.randomUUID(),
      eventAuditReference = "123456",
      status = "delivered",
      sentTo = "testemail@example.com",
      createdAt = LocalDateTime.now().minusDays(2),
      completedAt = LocalDateTime.now().minusDays(1),
      sentAt = LocalDateTime.now().minusDays(1),
      notificationType = "email",
      templateId = UUID.randomUUID(),
      templateVersion = 1,
    )

    visitSchedulerMockServer.stubProcessNotifyCallbackNotification(HttpStatus.OK)

    // Then
    webTestClient.post()
      .uri("/visits/notify/callback")
      .header("Authorization", "Bearer $validToken")
      .contentType(APPLICATION_JSON)
      .bodyValue(dto)
      .exchange()
      .expectStatus().isOk
  }

  @Test
  fun `should return unauthorized when token is null`() {
    val dto = NotifyCallbackNotificationDto(
      notificationId = UUID.randomUUID(),
      eventAuditReference = "123456",
      status = "delivered",
      sentTo = "testemail@example.com",
      createdAt = LocalDateTime.now().minusDays(2),
      completedAt = LocalDateTime.now().minusDays(1),
      sentAt = LocalDateTime.now().minusDays(1),
      notificationType = "email",
      templateId = UUID.randomUUID(),
      templateVersion = 1,
    )

    webTestClient.post()
      .uri("/visits/notify/callback")
      .contentType(APPLICATION_JSON)
      .bodyValue(dto)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
  }

  @Test
  fun `should return unauthorized when token is invalid`() {
    val invalidToken = "Bearer invalid-token"
    val dto = NotifyCallbackNotificationDto(
      notificationId = UUID.randomUUID(),
      eventAuditReference = "123456",
      status = "delivered",
      sentTo = "testemail@example.com",
      createdAt = LocalDateTime.now().minusDays(2),
      completedAt = LocalDateTime.now().minusDays(1),
      sentAt = LocalDateTime.now().minusDays(1),
      notificationType = "email",
      templateId = UUID.randomUUID(),
      templateVersion = 1,
    )

    webTestClient.post()
      .uri("/visits/notify/callback")
      .header("Authorization", "Bearer $invalidToken")
      .contentType(APPLICATION_JSON)
      .bodyValue(dto)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
  }

  @Test
  fun `should not forward call to visit-scheduler if eventAuditReference is missing (manually generated notifications)`() {
    // Given
    val validToken = "test-valid-token"
    val dto = NotifyCallbackNotificationDto(
      notificationId = UUID.randomUUID(),
      eventAuditReference = null,
      status = "delivered",
      sentTo = "testemail@example.com",
      createdAt = LocalDateTime.now().minusDays(2),
      completedAt = LocalDateTime.now().minusDays(1),
      sentAt = LocalDateTime.now().minusDays(1),
      notificationType = "email",
      templateId = UUID.randomUUID(),
      templateVersion = 1,
    )

    // Then
    webTestClient.post()
      .uri("/visits/notify/callback")
      .header("Authorization", "Bearer $validToken")
      .contentType(APPLICATION_JSON)
      .bodyValue(dto)
      .exchange()
      .expectStatus().isOk

    verifyNoInteractions(visitSchedulerService)
  }
}
