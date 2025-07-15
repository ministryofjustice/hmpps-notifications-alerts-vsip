package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import java.time.LocalDateTime

@Schema(description = "Visit")
@JsonInclude(JsonInclude.Include.NON_NULL)
data class VisitDto(
  @Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @JsonProperty("prisonId")
  @JsonAlias("prisonCode")
  @Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
  @Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  val startTimestamp: LocalDateTime,
  @Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  val endTimestamp: LocalDateTime,
  @Schema(description = "Visit Contact", required = true)
  val visitContact: ContactDto,
  @Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @Schema(description = "List of visitors associated with the visit", required = true)
  val visitors: List<VisitorDto> = listOf(),
  @Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = false)
  val outcomeStatus: String? = null,
  @Schema(description = "External system details associated with the visit")
  val visitExternalSystemDetails: VisitExternalSystemDetailsDto?,
  @Schema(description = "Visit Sub Status", example = "AUTO_APPROVED", required = true)
  val visitSubStatus: String,
)
