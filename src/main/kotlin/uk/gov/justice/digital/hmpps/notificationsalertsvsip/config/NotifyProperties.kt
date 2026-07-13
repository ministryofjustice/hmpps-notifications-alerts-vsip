package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.SmsTemplateNames

@Component
@ConfigurationProperties(prefix = "notify")
@Validated
class NotifyProperties {
  @NotNull
  var smsTemplates: Map<LanguagePreference, Map<SmsTemplateNames, String>> = emptyMap()

  @NotNull
  var emailTemplates: Map<LanguagePreference, Map<EmailTemplateNames, String>> = emptyMap()

  @NotNull
  var replyToEmailIds: Map<String, String> = emptyMap()
}
