package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils
import uk.gov.service.notify.NotificationClient

@Service
class EmailSenderService(
  @Value("\${notify.email.enabled:}") private val enabled: Boolean,
  val notificationClient: NotificationClient,
  val prisonRegisterService: PrisonRegisterService,
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

        // TODO VB-4332: Implement other event types in next tickets(UPDATED, CANCELLED)
        else -> {
          throw IllegalArgumentException("Unsupported visit event type for EMAIL: $visitEventType")
        }
      }

      notificationClient.sendEmail(
        templatesConfig.emailTemplates[sendEmailNotificationDto.templateName.name],
        visit.visitContact.email,
        sendEmailNotificationDto.templateVars,
        visit.reference,
      )
    } else {
      LOG.info("Sending Email has been disabled.")
    }
  }

  private fun handleBookedEvent(visit: VisitDto): SendEmailNotificationDto {
    val templateVars = getCommonEmailTemplateVars(visit)
    templateVars["ref number"] = visit.reference

    val templateName = EmailTemplateNames.VISIT_BOOKING

    return SendEmailNotificationDto(templateName = templateName, templateVars = templateVars)
  }

  private fun getCommonEmailTemplateVars(visit: VisitDto): MutableMap<String, String> {
    val templateVars = mutableMapOf(
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "time" to dateUtils.getFormattedTime(visit.startTimestamp.toLocalTime()),
      "dayofweek" to dateUtils.getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to dateUtils.getFormattedDate(visit.startTimestamp.toLocalDate()),
      // TODO VB-4332: Add other email template vars (See JIRA Ticket).
    )

    return templateVars
  }
}
