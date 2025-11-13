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
  @param:Schema(description = "Visit Reference", example = "v9-d7-ed-7u", required = true)
  val reference: String,
  @param:Schema(description = "Prisoner Id", example = "AF34567G", required = true)
  val prisonerId: String,
  @param:JsonProperty("prisonId")
  @param:JsonAlias("prisonCode")
  @param:Schema(description = "Prison Id", example = "MDI", required = true)
  val prisonCode: String,
  @param:Schema(description = "The date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  val startTimestamp: LocalDateTime,
  @param:Schema(description = "The finishing date and time of the visit", example = "2018-12-01T13:45:00", required = true)
  val endTimestamp: LocalDateTime,
  @param:Schema(description = "Visit Contact", required = true)
  val visitContact: ContactDto,
  @param:Schema(description = "Visit Restriction", example = "OPEN", required = true)
  val visitRestriction: VisitRestriction,
  @param:Schema(description = "List of visitors associated with the visit", required = true)
  val visitors: List<VisitorDto> = listOf(),
  @param:Schema(description = "Outcome Status", example = "VISITOR_CANCELLED", required = false)
  val outcomeStatus: String? = null,
  @param:Schema(description = "External system details associated with the visit")
  val visitExternalSystemDetails: VisitExternalSystemDetailsDto?,
  @param:Schema(description = "Visit Sub Status", example = "AUTO_APPROVED", required = true)
  val visitSubStatus: String,
)
