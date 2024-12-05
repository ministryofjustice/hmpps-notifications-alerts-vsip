package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Gov Notify Callback Notification")
data class NotifyCallbackNotificationDto(
  @Schema(description = "The UUID of the notification", required = true)
  val id: UUID,
  @Schema(description = "The id of the event audit which the notification is linked to", required = true)
  @JsonProperty("reference")
  val eventAuditId: String,
  @Schema(description = "The final status of the notification", required = true)
  val status: String,
  @Schema(description = "The timestamp for when the vsip notification service sent the notification to gov notify", required = true)
  val createdAt: String?,
  @Schema(description = "The timestamp for the final update of the notification (when delivered or ultimately failed) ", required = false)
  val completedAt: String?,
  @Schema(description = "The timestamp for when gov notify sent the notification", required = false)
  val sentAt: String?,
  @Schema(description = "The type of the notification", required = true)
  val notificationType: String?,
  @Schema(description = "The id the template used for the notification", required = true)
  val templateId: UUID,
  @Schema(description = "The version of the template used for the notification", required = true)
  val templateVersion: Int,
)
