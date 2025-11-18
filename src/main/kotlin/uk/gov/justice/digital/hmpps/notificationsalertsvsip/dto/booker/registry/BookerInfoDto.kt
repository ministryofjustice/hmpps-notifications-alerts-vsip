package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Details of a found booker via booker search")
data class BookerInfoDto(
  @param:Schema(name = "reference", description = "This is the booker reference, unique per booker", required = true)
  @field:NotBlank
  val reference: String,

  @param:Schema(name = "email", description = "email registered to booker", required = true)
  @field:NotBlank
  val email: String,
)
