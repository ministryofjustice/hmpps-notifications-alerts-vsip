package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Contact information for a prison department")
class PrisonContactDetailsDto(
  @Schema(description = "email address", example = "example@example.com", required = false)
  val emailAddress: String? = null,
  @Schema(description = "Phone Number", example = "01234567890", required = false)
  val phoneNumber: String? = null,
)
