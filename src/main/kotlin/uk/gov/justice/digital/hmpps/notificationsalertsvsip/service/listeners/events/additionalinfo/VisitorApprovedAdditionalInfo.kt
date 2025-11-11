package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo

import jakarta.validation.constraints.NotBlank

data class VisitorApprovedAdditionalInfo(
  @NotBlank
  val bookerReference: String,
  @NotBlank
  val prisonerId: String,
  @NotBlank
  val visitorId: String,
)
