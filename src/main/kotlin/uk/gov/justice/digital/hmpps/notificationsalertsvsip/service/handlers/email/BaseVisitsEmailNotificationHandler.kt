package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.personalisations.PrisonerVisitorPersonalisationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.ContactWithOptionalPrisonerRelationshipDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.NotificationTemplateResolver
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonRegisterService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerContactRegistryService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerSearchService
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
abstract class BaseVisitsEmailNotificationHandler {
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
  lateinit var notificationTemplateResolver: NotificationTemplateResolver

  abstract fun handle(visit: VisitDto): SendEmailNotificationDto

  protected fun getTemplateName(
    template: EmailTemplateNames,
    languagePreference: LanguagePreference = LanguagePreference.EN,
  ): String = notificationTemplateResolver.getEmailTemplate(template = template, languagePreference = languagePreference)

  protected fun getPrisoner(visit: VisitDto): Map<String, Any> {
    val templateVars: MutableMap<String, Any> = prisonerSearchService.getPrisoner(visit.prisonerId)?.let { prisoner ->
      mutableMapOf(
        "opening sentence" to "visit to see $prisoner",
        "prisoner" to "$prisoner",
      )
    } ?: mutableMapOf(
      "opening sentence" to "visit to the prison",
      "prisoner" to "the prisoner",
    )
    when (visit.visitContact.languagePreference) {
      LanguagePreference.CY -> templateVars.putAll(emptyMap<String, Any>())
      else -> Unit
    }

    return templateVars
  }

  protected fun getPrisonContactDetails(visit: VisitDto): Map<String, String> {
    val prisonContactDetails = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)
    val templateVars = mutableMapOf(
      "phone" to (prisonContactDetails?.phoneNumber ?: GOV_UK_PRISON_PAGE),
      "website" to (prisonContactDetails?.webAddress ?: GOV_UK_PRISON_PAGE),
    )
    when (visit.visitContact.languagePreference) {
      LanguagePreference.CY -> templateVars.putAll(emptyMap<String, String>())
      else -> Unit
    }

    return templateVars
  }

  protected fun getVisitors(visit: VisitDto): List<String> {
    val visitorIds = visit.visitors.map { it.nomisPersonId }
    if (visitorIds.isEmpty()) {
      return listOf("You can view visitor information in the bookings section of your GOV.UK One Login")
    }
    val visitors = prisonerContactRegistryService.searchPrisonerContacts(visit.prisonerId, visitorIds, false)

    return if (visitors.isNotEmpty()) {
      visitors.distinctBy { it.contactId }.map {
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

  protected fun calculateAge(visitor: ContactWithOptionalPrisonerRelationshipDto): String = visitor.dateOfBirth?.let {
    ChronoUnit.YEARS.between(it, LocalDate.now()).toInt().toString() + " years old"
  } ?: "age not known"
}
