package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto

@Schema(description = "Name of visitor within the visitor request")
data class VisitorRequestVisitorInfoDto(
  @param:Schema(description = "First name", example = "John", required = true)
  val firstName: String,

  @param:Schema(description = "Last name", example = "Smith", required = true)
  val lastName: String,
) {
  constructor(prisonerContactRegistryContactDto: PrisonerContactRegistryContactDto) : this(
    firstName = prisonerContactRegistryContactDto.firstName,
    lastName = prisonerContactRegistryContactDto.lastName,
  )

  constructor(visitorRequestDto: VisitorRequestDto) : this(
    firstName = visitorRequestDto.firstName,
    lastName = visitorRequestDto.lastName,
  )
}
