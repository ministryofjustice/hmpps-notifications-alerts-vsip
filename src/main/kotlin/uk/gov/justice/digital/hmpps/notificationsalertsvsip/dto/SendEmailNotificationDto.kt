package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Gov Notify Email notification details")
data class SendEmailNotificationDto(
  @Schema(description = "Name of gov notify template to use", example = "VISIT_BOOKING", required = true)
  val templateName: EmailTemplateNames,
  @Schema(description = "Map of gov notify template personalisations", required = true)
  val templateVars: Map<String, Any>,
)
