package uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.EmailSenderService.Companion.GOV_UK_PRISON_PAGE
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.PrisonRegisterService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.PrisonerSearchService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek

@Component
class EmailTemplateUtils(
  private val prisonRegisterService: PrisonRegisterService,
  private val prisonerSearchService: PrisonerSearchService,
) {

  fun getCommonTemplateVars(visit: VisitDto): MutableMap<String, Any> {
    val templateVars: MutableMap<String, Any> = mutableMapOf(
      "ref number" to visit.reference,
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
      "main contact name" to visit.visitContact.name,
    )
    templateVars.putAll(getPrisoner(visit))
    templateVars.putAll(getPrisonContactDetails(visit))

    return templateVars
  }

  private fun getPrisoner(visit: VisitDto): Map<String, Any> {
    return prisonerSearchService.getPrisoner(visit.prisonerId)?.let { prisoner ->
      return mutableMapOf(
        "opening sentence" to "visit to see $prisoner",
        "prisoner" to "$prisoner",
      )
    } ?: mutableMapOf(
      "opening sentence" to "visit to the prison",
      "prisoner" to "the prisoner",
    )
  }

  private fun getPrisonContactDetails(visit: VisitDto): Map<String, String> {
    val prisonContactDetails = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)
    return mutableMapOf(
      "phone" to (prisonContactDetails?.phoneNumber ?: GOV_UK_PRISON_PAGE),
      "website" to (prisonContactDetails?.webAddress ?: GOV_UK_PRISON_PAGE),
    )
  }
}
