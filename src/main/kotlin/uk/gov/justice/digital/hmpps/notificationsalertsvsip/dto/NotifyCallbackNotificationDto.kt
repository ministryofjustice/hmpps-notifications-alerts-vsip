package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Gov Notify Callback Notification")
data class NotifyCallbackNotificationDto(
  @Schema(description = "The UUID of the notification", required = true)
  @JsonAlias("id")
  val notificationId: UUID,
  @Schema(description = "The id of the event audit which the notification is linked to", required = true)
  @JsonAlias("reference")
  val eventAuditReference: String,
  @Schema(description = "The final status of the notification", required = true)
  val status: String,
  @Schema(description = "The timestamp for when the vsip notification service sent the notification to gov notify", required = true)
  @JsonAlias("created_at")
  val createdAt: LocalDateTime,
  @Schema(description = "The timestamp for the final update of the notification (when delivered or ultimately failed) ", required = false)
  @JsonAlias("completed_at")
  val completedAt: LocalDateTime?,
  @Schema(description = "The timestamp for when gov notify sent the notification", required = false)
  @JsonAlias("sent_at")
  val sentAt: LocalDateTime?,
  @Schema(description = "The email or phone number the notification was sent to", required = true)
  @JsonAlias("to")
  val sentTo: String,
  @Schema(description = "The type of the notification", required = true)
  @JsonAlias("notification_type")
  val notificationType: String,
  @Schema(description = "The id the template used for the notification", required = true)
  @JsonAlias("template_id")
  val templateId: UUID,
  @Schema(description = "The version of the template used for the notification", required = true)
  @JsonAlias("template_version")
  val templateVersion: Int,
)
