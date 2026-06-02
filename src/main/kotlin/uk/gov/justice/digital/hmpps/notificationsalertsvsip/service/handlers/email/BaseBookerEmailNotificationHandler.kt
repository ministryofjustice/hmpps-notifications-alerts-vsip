package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestVisitorInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.NotificationTemplateResolver

@Service
abstract class BaseBookerEmailNotificationHandler {
  @Autowired
  lateinit var notificationTemplateResolver: NotificationTemplateResolver

  protected fun getTemplateName(
    template: EmailTemplateNames,
    languagePreference: LanguagePreference = LanguagePreference.EN,
  ): String = notificationTemplateResolver.getEmailTemplate(template = template, languagePreference = languagePreference)

  abstract fun handle(bookerInfoDto: BookerInfoDto, visitorInfo: VisitorRequestVisitorInfoDto): SendEmailNotificationDto
}
