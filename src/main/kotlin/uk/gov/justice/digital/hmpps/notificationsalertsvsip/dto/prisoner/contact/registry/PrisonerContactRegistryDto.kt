package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
data class PrisonerContactRegistryDto(
  @Schema(description = "First name", example = "John", required = true)
  val firstName: String,

  @Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,

  @Schema(description = "Date of birth", example = "1980-01-28", required = false)
  val dateOfBirth: LocalDate? = null,
)
