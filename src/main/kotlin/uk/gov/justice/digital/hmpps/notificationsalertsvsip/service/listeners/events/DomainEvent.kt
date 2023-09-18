package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.deserializers.RawJsonDeserializer

data class DomainEvent(
  val eventType: String,

  @JsonDeserialize(using = RawJsonDeserializer::class)
  val additionalInformation: String,
)
