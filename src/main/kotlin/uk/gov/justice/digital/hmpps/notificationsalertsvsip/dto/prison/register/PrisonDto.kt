package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Prison Information")
data class PrisonDto(
  @Schema(description = "Prison ID", example = "MDI", required = true) val prisonId: String,
  @Schema(description = "Name of the prison", example = "Moorland HMP", required = true) val prisonName: String,
  @Schema(description = "Whether the prison is still active", required = true) val active: Boolean,
)
