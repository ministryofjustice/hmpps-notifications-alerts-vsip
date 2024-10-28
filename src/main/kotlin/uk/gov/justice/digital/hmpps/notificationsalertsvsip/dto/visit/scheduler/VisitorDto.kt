package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Visit Visitor")
data class VisitorDto(
  @Schema(description = "Person ID (nomis) of the visitor", example = "1234", required = true)
  @field:NotNull
  @JsonProperty("nomisPersonId")
  val nomisPersonId: Long,
)
