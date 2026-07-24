package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_CANCEL
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_CANCEL_NO_PRISON_NUMBER
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_REQUEST_REJECTED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime

@Service
class CancelledEventVisitsSmsHandler : BaseVisitsSmsNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleCancelledEvent (sms) - Entered for visit reference - ${visit.reference} with sub status - ${visit.visitSubStatus}")

    val prisonContactNumber: String? = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)?.phoneNumber
    val isPrisonContactNumberAvailable = !prisonContactNumber.isNullOrEmpty()
    val templateVars = getTemplateVars(visit, prisonContactNumber)
    val templateName = getCancelledSmsTemplateName(visit, isPrisonContactNumberAvailable)
    return SendSmsNotificationDto(templateName, templateVars)
  }

  private fun getTemplateVars(visit: VisitDto, prisonContactNumber: String?): Map<String, String> {
    val templateVars = mutableMapOf(
      "ref number" to visit.reference,
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
      "reference" to visit.reference,
    )
    if (!prisonContactNumber.isNullOrEmpty()) {
      templateVars["prison phone number"] = prisonContactNumber
    }
    when (visit.visitContact.languagePreference) {
      LanguagePreference.CY -> templateVars.putAll(emptyMap<String, String>())
      else -> Unit
    }

    return templateVars
  }

  private fun getCancelledSmsTemplateName(visit: VisitDto, isPrisonContactNumberAvailable: Boolean): String {
    val template = when (visit.visitSubStatus) {
      "REJECTED", "AUTO_REJECTED" -> {
        VISIT_REQUEST_REJECTED
      }

      else -> if (isPrisonContactNumberAvailable) {
        VISIT_CANCEL
      } else {
        VISIT_CANCEL_NO_PRISON_NUMBER
      }
    }

    return getTemplateName(template, languagePreference = visit.visitContact.languagePreference)
  }
}
