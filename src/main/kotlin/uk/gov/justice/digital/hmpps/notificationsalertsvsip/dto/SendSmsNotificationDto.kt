package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Gov Notify SMS notification details")
data class SendSmsNotificationDto(
  @Schema(description = "Name of gov notify template to use", example = "VISIT_CANCELLED", required = true)
  val templateName: SmsTemplateNames,
  @Schema(description = "Map of gov notify template personalisations", required = true)
  val templateVars: Map<String, Any>,
)
