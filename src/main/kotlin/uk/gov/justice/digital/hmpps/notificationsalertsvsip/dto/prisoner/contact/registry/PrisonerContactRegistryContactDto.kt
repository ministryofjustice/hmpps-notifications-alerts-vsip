package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A contact for a prisoner")
data class PrisonerContactRegistryContactDto(
  @param:Schema(description = "The identifier of the contact", example = "5871791")
  val personId: String,

  @param:Schema(description = "First name", example = "John", required = true)
  val firstName: String,

  @param:Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,

  @param:Schema(description = "Date of birth", example = "1980-01-28", required = false)
  val dateOfBirth: LocalDate? = null,
)
