package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerSearchService

@Service
class PrisonerPersonalisationService(
  private val prisonerSearchService: PrisonerSearchService,
) {
  fun getPersonalisations(prisonerId: String, languagePreference: LanguagePreference): Map<String, String> {
    val prisoner = prisonerSearchService.getPrisoner(prisonerId)

    val templateVars: MutableMap<String, String> = prisoner?.let {
      mutableMapOf(
        "opening sentence" to "visit to see $it",
        "prisoner" to "$it",
      )
    } ?: mutableMapOf(
      "opening sentence" to "visit to the prison",
      "prisoner" to "the prisoner",
    )

    when (languagePreference) {
      LanguagePreference.CY -> templateVars.putAll(
        prisoner?.let {
          mapOf(
            "openingsentence_cy" to "ymweliad i weld $it",
            "prisoner_cy" to "$it",
          )
        } ?: mapOf(
          "openingsentence_cy" to "ymweliad â'r carchar",
          "prisoner_cy" to "y carcharor",
        ),
      )

      else -> Unit
    }

    return templateVars
  }
}
