package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.service.notify.SendEmailResponse
import uk.gov.service.notify.SendSmsResponse
import java.time.LocalDateTime
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Gov Notify Create Notification")
data class NotifyCreateNotificationDto(
  @Schema(description = "The UUID of the notification", required = true)
  val id: UUID,
  @Schema(description = "The id of the event audit which the notification is linked to", example = "123456", required = true)
  @JsonProperty("reference")
  val eventAuditId: String,
  @Schema(description = "The timestamp for when the vsip notification service sent the notification to gov notify", required = true)
  val createdAt: LocalDateTime,
  @Schema(description = "The type of the notification", required = true)
  val notificationType: String,
  @Schema(description = "The id the template used for the notification", required = true)
  val templateId: UUID,
  @Schema(description = "The version of the template used for the notification", required = true)
  val templateVersion: Int,
) {
  constructor(sendEmailResponse: SendEmailResponse) : this(
    id = sendEmailResponse.notificationId,
    eventAuditId = sendEmailResponse.reference.toString(),
    createdAt = LocalDateTime.now(),
    notificationType = "email",
    templateId = sendEmailResponse.templateId,
    templateVersion = sendEmailResponse.templateVersion,
  )

  constructor(sendSmsResponse: SendSmsResponse) : this(
    id = sendSmsResponse.notificationId,
    eventAuditId = sendSmsResponse.reference.toString(),
    createdAt = LocalDateTime.now(),
    notificationType = "sms",
    templateId = sendSmsResponse.templateId,
    templateVersion = sendSmsResponse.templateVersion,
  )
}
