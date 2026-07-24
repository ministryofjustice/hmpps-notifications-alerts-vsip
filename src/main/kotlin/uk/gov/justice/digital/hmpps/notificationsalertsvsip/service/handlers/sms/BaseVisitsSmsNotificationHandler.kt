package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.sms

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendSmsNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.NotificationTemplateResolver
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonRegisterService

@Service
abstract class BaseVisitsSmsNotificationHandler {

  @Autowired
  lateinit var prisonRegisterService: PrisonRegisterService

  @Autowired
  lateinit var notificationTemplateResolver: NotificationTemplateResolver

  abstract fun handle(visit: VisitDto): SendSmsNotificationDto

  protected fun getTemplateName(
    template: SmsTemplateNames,
    languagePreference: LanguagePreference = LanguagePreference.EN,
  ): String = notificationTemplateResolver.getSmsTemplate(template = template, languagePreference = languagePreference)
}
