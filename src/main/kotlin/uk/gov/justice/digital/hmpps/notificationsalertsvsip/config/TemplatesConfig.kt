package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties(prefix = "notify")
@Validated
class TemplatesConfig {
  @NotNull
  var smsTemplates: MutableMap<String, String> = HashMap()

  @NotNull
  var emailTemplates: MutableMap<String, String> = HashMap()
}
