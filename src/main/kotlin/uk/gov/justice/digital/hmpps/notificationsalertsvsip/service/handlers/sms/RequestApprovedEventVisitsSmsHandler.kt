package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_BOOKING_OR_REQUEST_APPROVED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime

@Service
class RequestApprovedEventVisitsSmsHandler : BaseVisitsSmsNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleRequestApproved (sms) - Entered")

    return SendSmsNotificationDto(templateName = getRequestApprovedSmsTemplateName(visit), templateVars = getTemplateVars(visit))
  }

  private fun getTemplateVars(visit: VisitDto): Map<String, String> {
    val templateVars = mutableMapOf(
      "ref number" to visit.reference,
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
    )
    when (visit.visitContact.languagePreference) {
      LanguagePreference.CY -> templateVars.putAll(emptyMap<String, String>())
      else -> Unit
    }

    return templateVars
  }

  private fun getRequestApprovedSmsTemplateName(visit: VisitDto): String {
    val template = when (visit.visitSubStatus) {
      "APPROVED", "AUTO_APPROVED" -> {
        VISIT_BOOKING_OR_REQUEST_APPROVED
      }

      else -> {
        LOG.error("visit request approved for visit sub status ${visit.visitSubStatus} is unsupported")
        throw ValidationException("visit request approved for visit sub status ${visit.visitSubStatus} is unsupported")
      }
    }

    return getTemplateName(template, languagePreference = visit.visitContact.languagePreference)
  }
}
