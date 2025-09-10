package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.personalisations.PrisonerVisitorPersonalisationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonRegisterService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerContactRegistryService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerSearchService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
abstract class BaseEmailNotificationHandler {
  companion object {
    const val GOV_UK_PRISON_PAGE = "https://www.gov.uk/government/collections/prisons-in-england-and-wales"
  }

  @Autowired
  lateinit var prisonRegisterService: PrisonRegisterService

  @Autowired
  lateinit var prisonerSearchService: PrisonerSearchService

  @Autowired
  lateinit var prisonerContactRegistryService: PrisonerContactRegistryService

  @Autowired
  lateinit var templatesConfig: TemplatesConfig

  abstract fun handle(visit: VisitDto): SendEmailNotificationDto

  protected fun getTemplateName(template: EmailTemplateNames): String = templatesConfig.emailTemplates[template.name]!!

  protected fun getCommonTemplateVars(visit: VisitDto): MutableMap<String, Any> {
    val templateVars: MutableMap<String, Any> = mutableMapOf(
      "ref number" to visit.reference,
      "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
      "end time" to getFormattedTime(visit.endTimestamp.toLocalTime()),
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
      "main contact name" to visit.visitContact.name,
    )
    templateVars.putAll(getPrisoner(visit))
    templateVars.putAll(getPrisonContactDetails(visit))

    return templateVars
  }

  protected fun getPrisoner(visit: VisitDto): Map<String, Any> {
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

  protected fun getPrisonContactDetails(visit: VisitDto): Map<String, String> {
    val prisonContactDetails = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)
    return mutableMapOf(
      "phone" to (prisonContactDetails?.phoneNumber ?: GOV_UK_PRISON_PAGE),
      "website" to (prisonContactDetails?.webAddress ?: GOV_UK_PRISON_PAGE),
    )
  }

  protected fun getVisitors(visit: VisitDto): List<String> {
    val visitors = prisonerContactRegistryService.getPrisonerContacts(visit)
    return if (visitors.isNotEmpty()) {
      visitors.distinctBy { it.personId }.map {
        PrisonerVisitorPersonalisationDto(
          firstNameText = it.firstName,
          lastNameText = it.lastName,
          ageText = calculateAge(it),
        ).toString()
      }
    } else {
      listOf("You can view visitor information in the bookings section of your GOV.UK One Login")
    }
  }

  protected fun calculateAge(visitor: PrisonerContactRegistryContactDto): String = visitor.dateOfBirth?.let {
    ChronoUnit.YEARS.between(it, LocalDate.now()).toInt().toString() + " years old"
  } ?: "age not known"
}
