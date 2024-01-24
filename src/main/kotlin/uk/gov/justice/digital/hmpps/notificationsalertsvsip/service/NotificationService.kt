package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Service
class NotificationService(
  val visitSchedulerClient: VisitSchedulerClient,
  val prisonRegisterClient: PrisonRegisterClient,
  val smsSenderService: SmsSenderService,
  @Value("\${notify.template-id.visit-booking:}") private val visitBookingTemplateId: String,
  @Value("\${notify.template-id.visit-update:}") private val visitUpdateTemplateId: String,
  @Value("\${notify.template-id.visit-cancel:}") private val visitCancelTemplateId: String,
  @Value("\${notify.template-id.visit-cancel-no-prison-number:}") private val visitCancelNoPrisonNumberTemplateId: String,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    private const val SMS_DATE_PATTERN = "dd MMMM yyyy"
    private const val SMS_TIME_PATTERN = "h:mma"
    private const val SMS_TIME_PATTERN_WHEN_MNIUTES_IS_ZERO = "ha"
    private const val SMS_DAY_OF_WEEK_PATTERN = "EEEE"
  }

  enum class VisitEventType { BOOKED, UPDATED, CANCELLED }

  fun sendMessage(visitEventType: VisitEventType, bookingReference: String) {
    LOG.info("Visit booked event with reference - $bookingReference")

    val visit = getVisit(bookingReference)

    visit?.visitContact?.telephone?.let { telephoneNumber ->
      val prisonName = getPrisonName(visit.prisonCode)
      when (visitEventType) {
        VisitEventType.BOOKED, VisitEventType.UPDATED -> smsSenderService.sendSms(
          getNotificationTemplateId(visitEventType),
          telephoneNumber,
          mapOf(
            "prison" to prisonName,
            "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
            "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
            "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
            "ref number" to visit.reference,
          ),
          visit.reference,
        )

        VisitEventType.CANCELLED -> {
          val prisonContactNumber = getPrisonSocialVisitsContactNumber(visit.prisonCode)
          val cancelSmsValuesMap = mutableMapOf(
            "prison" to getPrisonName(visit.prisonCode),
            "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
            "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
            "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
            "reference" to visit.reference,
          )

          if (prisonContactNumber != null) {
            cancelSmsValuesMap["prison phone number"] = prisonContactNumber
            smsSenderService.sendSms(
              visitCancelTemplateId,
              telephoneNumber,
              cancelSmsValuesMap.toMap(),
              visit.reference,
            )
          } else {
            smsSenderService.sendSms(
              visitCancelNoPrisonNumberTemplateId,
              telephoneNumber,
              cancelSmsValuesMap.toMap(),
              visit.reference,
            )
          }
        }
      }
    }
  }

  private fun getVisit(bookingReference: String): VisitDto? {
    try {
      return visitSchedulerClient.getVisitByReference(bookingReference)
    } catch (e: Exception) {
      // if there was an error getting the visit return null
      if (e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND) {
        LOG.error("no visit found with booking reference - $bookingReference")
        return null
      }

      throw e
    }
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

  private fun getNotificationTemplateId(visitEventType: VisitEventType): String {
    return when (visitEventType) {
      VisitEventType.BOOKED -> visitBookingTemplateId
      VisitEventType.UPDATED -> visitUpdateTemplateId
      VisitEventType.CANCELLED -> visitCancelTemplateId
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
      DateTimeFormatter.ofPattern(SMS_TIME_PATTERN_WHEN_MNIUTES_IS_ZERO)
    } else {
      DateTimeFormatter.ofPattern(SMS_TIME_PATTERN)
    }

    return visitStartTime.format(formatter).lowercase()
  }

  private fun getFormattedDayOfWeek(visitDate: LocalDate): String {
    return visitDate.format(DateTimeFormatter.ofPattern(SMS_DAY_OF_WEEK_PATTERN))
  }
}
