package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.service.notify.NotificationClient

@Configuration
class NotifyConfiguration(@param:Value("\${notify.apikey:}") private val apiKey: String) {
  @Bean
  fun notifyClient(): NotificationClient = NotificationClient(apiKey)
}
