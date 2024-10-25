package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry

data class PrisonerVisitorDto(
  val firstName: String,
  val lastName: String,
  val age: Int? = null,
)
