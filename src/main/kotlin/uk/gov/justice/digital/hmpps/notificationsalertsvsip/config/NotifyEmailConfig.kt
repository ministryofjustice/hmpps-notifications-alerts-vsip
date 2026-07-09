package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties(prefix = "notify.email")
@Validated
class NotifyEmailConfig {
  @NotNull
  var replyToEmailIds: Map<String, String> = emptyMap()
}
