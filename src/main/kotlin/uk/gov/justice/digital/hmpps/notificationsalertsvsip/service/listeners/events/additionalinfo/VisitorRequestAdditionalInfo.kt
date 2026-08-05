package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo

import jakarta.validation.constraints.NotBlank

data class VisitorRequestAdditionalInfo(
  @field:NotBlank
  val requestReference: String,
)
