package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.personalisations.PrisonerVisitorPersonalisationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.PrisonRegisterService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.PrisonerContactRegistryService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.PrisonerSearchService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Component
class BookedEventEmailHandler(
  prisonRegisterService: PrisonRegisterService,
  prisonerSearchService: PrisonerSearchService,
  templatesConfig: TemplatesConfig,
  private val prisonerContactRegistryService: PrisonerContactRegistryService,
) : BaseEmailNotificationHandler(prisonRegisterService, prisonerSearchService, templatesConfig) {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleBookedEvent (email) - Entered")

    val templateVars = getCommonTemplateVars(visit).toMutableMap()

    templateVars.putAll(
      mapOf(
        "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
        "end time" to getFormattedTime(visit.endTimestamp.toLocalTime()),
        "arrival time" to "45",
        "closed visit" to (visit.visitRestriction == VisitRestriction.CLOSED).toString(),
        "visitors" to getVisitors(visit),
      ),
    )

    return SendEmailNotificationDto(
      templateName = getTemplateName(EmailTemplateNames.VISIT_BOOKING),
      templateVars = templateVars,
    )
  }

  private fun getVisitors(visit: VisitDto): List<String> {
    val visitors = prisonerContactRegistryService.getPrisonerContacts(visit)
    return if (visitors.isNotEmpty()) {
      visitors.map {
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

  private fun calculateAge(visitor: PrisonerContactRegistryContactDto): String {
    return visitor.dateOfBirth?.let {
      ChronoUnit.YEARS.between(it, LocalDate.now()).toInt().toString() + " years old"
    } ?: "age not known"
  }
}
