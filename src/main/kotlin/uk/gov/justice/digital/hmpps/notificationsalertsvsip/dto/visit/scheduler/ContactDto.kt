package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Contact")
data class ContactDto(
  @param:Schema(description = "Contact Name", example = "John Smith", required = true)
  val name: String,
  @param:Schema(description = "Contact Phone Number", example = "01234 567890", required = false)
  val telephone: String? = null,
  @param:Schema(description = "Contact Email", example = "example@email.com", required = false)
  val email: String? = null,
)
