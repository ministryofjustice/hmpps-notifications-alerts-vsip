package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties(prefix = "notify.sms")
@Validated
class SmsTemplatesConfig {
  @NotNull
  var templates: MutableMap<String, String> = HashMap()
}
