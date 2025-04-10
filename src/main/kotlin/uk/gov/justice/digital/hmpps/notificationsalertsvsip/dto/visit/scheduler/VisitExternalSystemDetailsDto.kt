package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler

import io.swagger.v3.oas.annotations.media.Schema

data class VisitExternalSystemDetailsDto(
  @Schema(description = "Client name", example = "client_name")
  val clientName: String?,
  @Schema(description = "Client visit reference", example = "Reference ID in the client system")
  val clientVisitReference: String?,
)
