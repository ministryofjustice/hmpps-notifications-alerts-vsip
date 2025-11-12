package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo

import jakarta.validation.constraints.NotBlank

data class VisitorApprovedAdditionalInfo(
  @field:NotBlank
  val bookerReference: String,
  @field:NotBlank
  val prisonerId: String,
  @field:NotBlank
  val visitorId: String,
)
