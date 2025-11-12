package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class VisitAdditionalInfo(
  @param:JsonProperty("reference")
  @field:NotBlank
  val bookingReference: String,
  @field:NotBlank
  val eventAuditId: String,
)
