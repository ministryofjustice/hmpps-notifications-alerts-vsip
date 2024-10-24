package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils
import uk.gov.service.notify.NotificationClient

@Service
class SmsSenderService(
  @Value("\${notify.sms.enabled:}") private val enabled: Boolean,
  val notificationClient: NotificationClient,
  val prisonRegisterService: PrisonRegisterService,
  val templatesConfig: TemplatesConfig,
  val dateUtils: DateUtils,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendSms(visit: VisitDto, visitEventType: VisitEventType) {
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

      notificationClient.sendSms(
        templatesConfig.smsTemplates[sendSmsNotificationDto.templateName.name],
        visit.visitContact.telephone,
        sendSmsNotificationDto.templateVars,
        visit.reference,
      )
    } else {
      LOG.info("Sending SMS has been disabled.")
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

    val prisonContactNumber: String? = prisonRegisterService.getPrisonSocialVisitsContactNumber(visit.prisonCode)
    if (!prisonContactNumber.isNullOrEmpty()) {
      templateVars["prison phone number"] = prisonContactNumber
      return SendSmsNotificationDto(templateName = SmsTemplateNames.VISIT_CANCEL, templateVars = templateVars)
    } else {
      return SendSmsNotificationDto(templateName = SmsTemplateNames.VISIT_CANCEL_NO_PRISON_NUMBER, templateVars = templateVars)
    }
  }

  private fun getCommonTemplateVars(visit: VisitDto): MutableMap<String, String> {
    val prisonName = prisonRegisterService.getPrisonName(visit.prisonCode)

    val templateVars = mutableMapOf(
      "prison" to prisonName,
      "time" to dateUtils.getFormattedTime(visit.startTimestamp.toLocalTime()),
      "dayofweek" to dateUtils.getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to dateUtils.getFormattedDate(visit.startTimestamp.toLocalDate()),
    )
    return templateVars
  }
}
