package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCreateNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime
import uk.gov.service.notify.NotificationClient
import uk.gov.service.notify.NotificationClientException

@Service
class SmsSenderService(
  @Value("\${notify.sms.enabled:}") private val enabled: Boolean,
  val notificationClient: NotificationClient,
  val prisonRegisterService: PrisonRegisterService,
  val templatesConfig: TemplatesConfig,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendSms(visit: VisitDto, visitEventType: VisitEventType, eventAuditId: String): NotifyCreateNotificationDto? {
    if (enabled) {
      val sendSmsNotificationDto = when (visitEventType) {
        VisitEventType.BOOKED -> {
          handleBookedEvent(visit)
        }

        VisitEventType.UPDATED -> {
          handleUpdateEvent(visit)
        }

        VisitEventType.CANCELLED -> {
          handleCancelEvent(visit)
        }
      }

      try {
        LOG.info("Calling notification client")
        val response = notificationClient.sendSms(
          templatesConfig.smsTemplates[sendSmsNotificationDto.templateName.name],
          visit.visitContact.telephone,
          sendSmsNotificationDto.templateVars,
          eventAuditId,
        )
        LOG.info("Calling notification client finished with response ${response.notificationId}")

        return NotifyCreateNotificationDto(response)
      } catch (e: NotificationClientException) {
        LOG.error("Error sending email with exception: $e")
        return null
      }
    } else {
      LOG.info("Sending SMS has been disabled.")
      return null
    }
  }

  private fun handleBookedEvent(visit: VisitDto): SendSmsNotificationDto {
    val templateVars = getCommonTemplateVars(visit)
    templateVars["ref number"] = visit.reference

    val templateName = SmsTemplateNames.VISIT_BOOKING

    return SendSmsNotificationDto(templateName = templateName, templateVars = templateVars)
  }

  private fun handleUpdateEvent(visit: VisitDto): SendSmsNotificationDto {
    val templateVars = getCommonTemplateVars(visit)
    templateVars["ref number"] = visit.reference

    val templateName = SmsTemplateNames.VISIT_UPDATE

    return SendSmsNotificationDto(templateName = templateName, templateVars = templateVars)
  }

  private fun handleCancelEvent(visit: VisitDto): SendSmsNotificationDto {
    val templateVars = getCommonTemplateVars(visit)
    templateVars["reference"] = visit.reference

    val prisonContactNumber: String? = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)?.phoneNumber
    if (!prisonContactNumber.isNullOrEmpty()) {
      templateVars["prison phone number"] = prisonContactNumber
      return SendSmsNotificationDto(templateName = SmsTemplateNames.VISIT_CANCEL, templateVars = templateVars)
    } else {
      return SendSmsNotificationDto(templateName = SmsTemplateNames.VISIT_CANCEL_NO_PRISON_NUMBER, templateVars = templateVars)
    }
  }

  private fun getCommonTemplateVars(visit: VisitDto): MutableMap<String, String> {
    val templateVars = mutableMapOf(
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
    )

    return templateVars
  }
}
