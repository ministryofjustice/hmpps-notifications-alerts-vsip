package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.SmsTemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.service.notify.NotificationClient
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class SmsSenderService(
  @Value("\${notify.sms.enabled:}")
  private val enabled: Boolean,
  val notificationClient: NotificationClient,
  val prisonRegisterClient: PrisonRegisterClient,
  val smsTemplateConfig: SmsTemplatesConfig,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    private const val SMS_DATE_PATTERN = "d MMMM yyyy"
    private const val SMS_TIME_PATTERN = "h:mma"
    private const val SMS_TIME_PATTERN_WHEN_MINUTES_IS_ZERO = "ha"
    private const val SMS_DAY_OF_WEEK_PATTERN = "EEEE"
  }

  fun sendSms(visit: VisitDto, visitEventType: VisitEventType) {
    if (enabled) {
      val templateVars = getCommonTemplateVars(visit)

      val templateName: SmsTemplateNames

      when (visitEventType) {
        VisitEventType.BOOKED -> {
          templateVars["ref number"] = visit.reference
          templateName = SmsTemplateNames.VISIT_BOOKING
        }

        VisitEventType.UPDATED -> {
          templateVars["ref number"] = visit.reference
          templateName = SmsTemplateNames.VISIT_UPDATE
        }

        VisitEventType.CANCELLED -> {
          templateVars["reference"] = visit.reference

          val prisonContactNumber: String? = getPrisonSocialVisitsContactNumber(visit.prisonCode)
          if (!prisonContactNumber.isNullOrEmpty()) {
            templateVars["prison phone number"] = prisonContactNumber
            templateName = SmsTemplateNames.VISIT_CANCEL
          } else {
            templateName = SmsTemplateNames.VISIT_CANCEL_NO_PRISON_NUMBER
          }
        }
      }

      val templateId = smsTemplateConfig.templates[templateName.name]

      notificationClient.sendSms(
        templateId,
        visit.visitContact.telephone,
        templateVars,
        visit.reference,
      )
    } else {
      LOG.info("Sending SMS has been disabled.")
    }
  }

  private fun getCommonTemplateVars(visit: VisitDto): MutableMap<String, String> {
    val prisonName = getPrisonName(visit.prisonCode)

    val templateVars = mutableMapOf(
      "prison" to prisonName,
      "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
    )
    return templateVars
  }

  private fun getPrisonSocialVisitsContactNumber(prisonCode: String): String? {
    return try {
      prisonRegisterClient.getSocialVisitContact(prisonCode)?.phoneNumber
    } catch (e: Exception) {
      // if there was an error getting the social visit contact return null
      LOG.info("no social visit contact number returned for prison - $prisonCode")
      null
    }
  }

  private fun getPrisonName(prisonCode: String): String {
    // get prison details from prison register
    val prison = prisonRegisterClient.getPrison(prisonCode)
    return prison?.prisonName ?: prisonCode
  }

  private fun getFormattedDate(visitDate: LocalDate): String {
    return visitDate.format(DateTimeFormatter.ofPattern(SMS_DATE_PATTERN))
  }

  private fun getFormattedTime(visitStartTime: LocalTime): String {
    val formatter = if (visitStartTime.minute == 0) {
      DateTimeFormatter.ofPattern(SMS_TIME_PATTERN_WHEN_MINUTES_IS_ZERO)
    } else {
      DateTimeFormatter.ofPattern(SMS_TIME_PATTERN)
    }

    return visitStartTime.format(formatter).lowercase()
  }

  private fun getFormattedDayOfWeek(visitDate: LocalDate): String {
    return visitDate.format(DateTimeFormatter.ofPattern(SMS_DAY_OF_WEEK_PATTERN))
  }
}
