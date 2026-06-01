package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames

@Component
class NotificationTemplateResolver(private val templatesConfig: TemplatesConfig) {

  fun getEmailTemplate(
    template: EmailTemplateNames,
    languagePreference: LanguagePreference = LanguagePreference.EN,
  ): String = templatesConfig.emailTemplates[languagePreference]?.get(template)
    ?: templatesConfig.emailTemplates[LanguagePreference.EN]?.get(template)
    ?: throw IllegalArgumentException("Email template $template not configured for language $languagePreference (or EN fallback)")

  fun getSmsTemplate(
    template: SmsTemplateNames,
    languagePreference: LanguagePreference = LanguagePreference.EN,
  ): String = templatesConfig.smsTemplates[languagePreference]?.get(template)
    ?: templatesConfig.smsTemplates[LanguagePreference.EN]?.get(template)
    ?: throw IllegalArgumentException("SMS template $template not configured for language $languagePreference (or EN fallback)")
}
