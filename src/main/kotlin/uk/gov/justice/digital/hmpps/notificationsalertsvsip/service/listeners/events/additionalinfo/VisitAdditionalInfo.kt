package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo

import com.fasterxml.jackson.annotation.JsonProperty

data class VisitAdditionalInfo(
  @JsonProperty("reference")
  val bookingReference: String,
)
