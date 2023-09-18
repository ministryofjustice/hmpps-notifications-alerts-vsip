package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.hmpps.auth

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

data class UserDetailsDto(
  @field:NotNull
  val username: String,

  @JsonProperty("name")
  val fullName: String? = null,
)
