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
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType.BOOKED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType.CANCELLED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType.UPDATED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.OutcomeStatus
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException
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
    const val GOV_UK_PRISON_PAGE = "https://www.gov.uk/government/collections/prisons-in-england-and-wales"
  }

  fun sendEmail(visit: VisitDto, visitEventType: VisitEventType) {
    if (enabled) {
      val sendEmailNotificationDto = when (visitEventType) {
        BOOKED -> {
          handleBookedEvent(visit)
        }

        CANCELLED -> {
          handleCancelledEvent(visit)
        }

        UPDATED -> {
          LOG.info("No email template for UPDATED event, skipping email notification for visit ${visit.reference}")
          return Unit
        }
      }

      LOG.info("Calling notification client")
      try {
        val response = notificationClient.sendEmail(
          templatesConfig.emailTemplates[sendEmailNotificationDto.templateName.name],
          visit.visitContact.email,
          sendEmailNotificationDto.templateVars,
          visit.reference,
        )
        LOG.info("Calling notification client finished with response ${response.notificationId}")
      } catch (e: NotificationClientException) {
        LOG.error("Error sending email with exception: $e")
      }
    } else {
      LOG.info("Sending Email has been disabled.")
    }
  }

  private fun handleBookedEvent(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleBookedEvent (email) - Entered")

    val templateVars = getCommonTemplateVars(visit)

    templateVars.putAll(
      mutableMapOf(
        "time" to dateUtils.getFormattedTime(visit.startTimestamp.toLocalTime()),
        "end time" to dateUtils.getFormattedTime(visit.endTimestamp.toLocalTime()),
        "arrival time" to "45",
        "closed visit" to (visit.visitRestriction == VisitRestriction.CLOSED).toString(),
        "visitors" to getVisitors(visit),
      ),
    )

    return SendEmailNotificationDto(templateName = EmailTemplateNames.VISIT_BOOKING, templateVars = templateVars)
  }

  private fun handleCancelledEvent(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleCancelledEvent (email) - Entered")

    return SendEmailNotificationDto(
      templateName = getCancelledEmailTemplateName(visit.outcomeStatus!!),
      templateVars = getCommonTemplateVars(visit),
    )
  }

  private fun getCancelledEmailTemplateName(visitOutcome: OutcomeStatus): EmailTemplateNames {
    return when (visitOutcome) {
      OutcomeStatus.PRISONER_CANCELLED -> {
        EmailTemplateNames.VISIT_CANCELLED_BY_PRISONER
      }

      OutcomeStatus.ESTABLISHMENT_CANCELLED, OutcomeStatus.DETAILS_CHANGED_AFTER_BOOKING, OutcomeStatus.ADMINISTRATIVE_ERROR -> {
        EmailTemplateNames.VISIT_CANCELLED_BY_PRISON
      }

      OutcomeStatus.VISITOR_CANCELLED -> {
        EmailTemplateNames.VISIT_CANCELLED
      }

      else -> {
        LOG.error("visit cancellation type $visitOutcome is unsupported, defaulting to standard cancellation template ${EmailTemplateNames.VISIT_CANCELLED}")
        EmailTemplateNames.VISIT_CANCELLED
      }
    }
  }

  private fun getCommonTemplateVars(visit: VisitDto): MutableMap<String, Any> {
    val templateVars: MutableMap<String, Any> = mutableMapOf(
      "ref number" to visit.reference,
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "dayofweek" to dateUtils.getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to dateUtils.getFormattedDate(visit.startTimestamp.toLocalDate()),
      "main contact name" to visit.visitContact.name,
      "opening sentence" to getPrisoner(visit),
    )
    templateVars.putAll(getPrisonContactDetails(visit))

    return templateVars
  }

  private fun getPrisoner(visit: VisitDto): String {
    return prisonerSearchService.getPrisoner(visit.prisonerId)?.let { prisoner ->
      return "Your visit to see $prisoner"
    } ?: "Your visit to the prison"
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

  private fun getPrisonContactDetails(visit: VisitDto): Map<String, String> {
    val prisonContactDetails = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)
    return mutableMapOf(
      "phone" to (prisonContactDetails?.phoneNumber ?: GOV_UK_PRISON_PAGE),
      "website" to (prisonContactDetails?.webAddress ?: GOV_UK_PRISON_PAGE),
    )
  }

  private fun calculateAge(visitor: PrisonerContactRegistryContactDto): String {
    return visitor.dateOfBirth?.let {
      ChronoUnit.YEARS.between(it, LocalDate.now()).toInt().toString() + " years old"
    } ?: "age not known"
  }
}
