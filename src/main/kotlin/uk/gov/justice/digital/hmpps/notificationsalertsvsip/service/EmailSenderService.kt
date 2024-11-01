package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.personalisations.PrisonerVisitorPersonalisationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class EmailSenderService(
  @Value("\${notify.email.enabled:}") private val enabled: Boolean,
  val notificationClient: NotificationClient,
  val prisonRegisterService: PrisonRegisterService,
  val prisonerSearchService: PrisonerSearchService,
  val prisonerContactRegistryService: PrisonerContactRegistryService,
  val templatesConfig: TemplatesConfig,
  val dateUtils: DateUtils,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendEmail(visit: VisitDto, visitEventType: VisitEventType) {
    if (enabled) {
      val sendEmailNotificationDto = when (visitEventType) {
        VisitEventType.BOOKED -> {
          handleBookedEvent(visit)
        }

        else -> {
          LOG.error("visit event type $visitEventType is unsupported")
          return Unit
        }
      }

      LOG.info("Calling notification client")
      val response = notificationClient.sendEmail(
        templatesConfig.emailTemplates[sendEmailNotificationDto.templateName.name],
        visit.visitContact.email,
        sendEmailNotificationDto.templateVars,
        visit.reference,
      )
      LOG.info("Calling notification client finished with response ${response.notificationId}")
    } else {
      LOG.info("Sending Email has been disabled.")
    }
  }

  private fun handleBookedEvent(visit: VisitDto): SendEmailNotificationDto {
    val templateVars = mutableMapOf(
      "ref number" to visit.reference,
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "time" to dateUtils.getFormattedTime(visit.startTimestamp.toLocalTime()),
      "end time" to dateUtils.getFormattedTime(visit.endTimestamp.toLocalTime()),
      "arrival time" to "45",
      "dayofweek" to dateUtils.getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to dateUtils.getFormattedDate(visit.startTimestamp.toLocalDate()),
      "main contact name" to visit.visitContact.name,
      "closed visit" to (visit.visitRestriction == VisitRestriction.CLOSED).toString(),
      "prisoner" to getPrisoner(visit),
      "visitors" to getVisitors(visit),
    )
    templateVars.putAll(getPrisonContactDetails(visit))

    val templateName = EmailTemplateNames.VISIT_BOOKING

    LOG.info("Sending Email template: $templateName")
    return SendEmailNotificationDto(templateName = templateName, templateVars = templateVars)
  }

  private fun getPrisoner(visit: VisitDto): String {
    return prisonerSearchService.getPrisoner(visit.prisonerId)?.let { prisoner ->
      return prisoner.toString()
    } ?: "Prisoner"
  }

  private fun getVisitors(visit: VisitDto): List<String> {
    return prisonerContactRegistryService.getPrisonerContacts(visit).map {
      PrisonerVisitorPersonalisationDto(
        firstNameText = it.firstName,
        lastNameText = it.lastName,
        ageText = calculateAge(it),
      ).toString()
    }
  }

  private fun getPrisonContactDetails(visit: VisitDto): Map<String, String> {
    val prisonContactDetails = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)
    return mutableMapOf(
      "phone" to (prisonContactDetails?.phoneNumber ?: "No phone number"),
      "website" to (prisonContactDetails?.webAddress ?: ""),
    )
  }

  private fun calculateAge(visitor: PrisonerContactRegistryContactDto): String {
    return visitor.dateOfBirth?.let {
      ChronoUnit.YEARS.between(it, LocalDate.now()).toInt().toString()
    } ?: "Unknown"
  }
}
