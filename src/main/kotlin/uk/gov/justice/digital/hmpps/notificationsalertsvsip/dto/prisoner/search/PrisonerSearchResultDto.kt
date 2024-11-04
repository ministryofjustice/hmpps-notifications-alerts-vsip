package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Prisoner information")
data class PrisonerSearchResultDto(
  @Schema(description = "Prisoner first name", example = "John", required = true)
  val firstName: String,

  @Schema(description = "Prisoner last name", example = "Smith", required = true)
  val lastName: String,
) {
  override fun toString(): String {
    return "Your visit to see $firstName $lastName"
  }
}
