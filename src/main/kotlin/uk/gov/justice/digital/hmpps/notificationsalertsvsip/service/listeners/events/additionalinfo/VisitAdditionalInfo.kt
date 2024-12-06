package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class VisitAdditionalInfo(
  @JsonProperty("reference")
  @NotBlank
  val bookingReference: String,
  @NotBlank
  val eventAuditId: String,
)
