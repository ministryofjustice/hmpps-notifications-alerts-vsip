package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonRegisterService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDate
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedDayOfWeek
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime

@Component
abstract class BaseSmsNotificationHandler(
  val prisonRegisterService: PrisonRegisterService,
  val templatesConfig: TemplatesConfig,
) {

  abstract fun handle(visit: VisitDto): SendSmsNotificationDto

  protected fun getTemplateName(template: SmsTemplateNames): String {
    return templatesConfig.smsTemplates[template.name]!!
  }

  protected fun getCommonTemplateVars(visit: VisitDto): MutableMap<String, String> {
    val templateVars = mutableMapOf(
      "prison" to prisonRegisterService.getPrisonName(visit.prisonCode),
      "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
      "dayofweek" to getFormattedDayOfWeek(visit.startTimestamp.toLocalDate()),
      "date" to getFormattedDate(visit.startTimestamp.toLocalDate()),
    )

    return templateVars
  }
}
