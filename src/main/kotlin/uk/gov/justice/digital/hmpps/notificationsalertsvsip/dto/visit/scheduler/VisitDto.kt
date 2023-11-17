package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Visit")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitDto(
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @JsonProperty("prisonId")
  @JsonAlias("prisonCode")
  @Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  val endTimestamp: LocalDateTime,
  @Schema(description = "Visit Notes", required = false)
  val visitContact: ContactDto? = null,
)
