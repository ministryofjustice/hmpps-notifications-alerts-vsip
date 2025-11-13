package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import jakarta.validation.constraints.NotBlank
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.deserializers.RawJsonDeserializer

data class DomainEvent(
  @field:NotBlank
  val eventType: String,

  @param:JsonDeserialize(using = RawJsonDeserializer::class)
  @field:NotBlank
  val additionalInformation: String,
)
