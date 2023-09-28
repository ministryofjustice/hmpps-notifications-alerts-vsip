package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

@Suppress("PropertyName")
data class SQSMessage(
  @NotBlank
  @JsonProperty("Type")
  val type: String,
  @NotBlank
  @JsonProperty("Message")
  val message: String,
  @JsonProperty("MessageId")
  val messageId: String? = null,
)
