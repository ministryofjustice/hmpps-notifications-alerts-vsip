package uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.personalisations

data class PrisonerVisitorPersonalisationDto(
  val firstNameText: String,
  val lastNameText: String,
  val ageText: String,
) {
  override fun toString(): String = "$firstNameText $lastNameText ($ageText)"
}
