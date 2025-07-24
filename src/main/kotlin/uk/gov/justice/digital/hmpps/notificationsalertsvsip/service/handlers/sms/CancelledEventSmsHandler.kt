package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_CANCEL
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_CANCEL_NO_PRISON_NUMBER
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames.VISIT_REQUEST_REJECTED

@Service
class CancelledEventSmsHandler : BaseSmsNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendSmsNotificationDto {
    LOG.info("handleCancelledEvent (sms) - Entered for visit reference - ${visit.reference} with sub status - ${visit.visitSubStatus}")

    val templateVars = getCommonTemplateVars(visit)
    templateVars["reference"] = visit.reference

    val prisonContactNumber: String? = prisonRegisterService.getPrisonSocialVisitsContactDetails(visit.prisonCode)?.phoneNumber
    val isPrisonContactNumberAvailable = !prisonContactNumber.isNullOrEmpty()
    if (isPrisonContactNumberAvailable) {
      templateVars["prison phone number"] = prisonContactNumber
    }
    val templateName = getCancelledSmsTemplateName(visit.visitSubStatus, isPrisonContactNumberAvailable)
    return SendSmsNotificationDto(templateName, templateVars)
  }

  private fun getCancelledSmsTemplateName(visitSubStatus: String, isPrisonContactNumberAvailable: Boolean): String {
    val template = when (visitSubStatus) {
      "REJECTED", "AUTO_REJECTED" -> {
        // TODO - add template for VISIT_REQUEST_REJECTED and no prison contact number
        VISIT_REQUEST_REJECTED
      }

      else -> if (isPrisonContactNumberAvailable) {
        VISIT_CANCEL
      } else {
        VISIT_CANCEL_NO_PRISON_NUMBER
      }
    }

    return getTemplateName(template)
  }
}
