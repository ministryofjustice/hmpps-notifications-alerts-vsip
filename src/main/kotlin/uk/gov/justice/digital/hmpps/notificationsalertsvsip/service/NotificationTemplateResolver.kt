package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames

@Component
class NotificationTemplateResolver(private val templatesConfig: TemplatesConfig) {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun getEmailTemplate(
    template: EmailTemplateNames,
    languagePreference: LanguagePreference = LanguagePreference.EN,
  ): String {
    val requestedTemplate = templatesConfig.emailTemplates[languagePreference]?.get(template)

    if (requestedTemplate != null) {
      return requestedTemplate
    }

    val fallbackTemplate = templatesConfig.emailTemplates[LanguagePreference.EN]?.get(template)

    if (fallbackTemplate != null) {
      LOG.error(
        "Email template {} not configured for language {}, falling back to en template",
        template,
        languagePreference.code,
      )
      return fallbackTemplate
    }

    throw IllegalArgumentException("Email template $template not configured for language ${languagePreference.code} (or en fallback)")
  }

  fun getSmsTemplate(
    template: SmsTemplateNames,
    languagePreference: LanguagePreference = LanguagePreference.EN,
  ): String {
    val requestedTemplate = templatesConfig.smsTemplates[languagePreference]?.get(template)

    if (requestedTemplate != null) {
      return requestedTemplate
    }

    val fallbackTemplate = templatesConfig.smsTemplates[LanguagePreference.EN]?.get(template)

    if (fallbackTemplate != null) {
      LOG.error(
        "SMS template {} not configured for language {}, falling back to en template",
        template,
        languagePreference.code,
      )
      return fallbackTemplate
    }

    throw IllegalArgumentException("SMS template $template not configured for language ${languagePreference.code} (or en fallback)")
  }
}
