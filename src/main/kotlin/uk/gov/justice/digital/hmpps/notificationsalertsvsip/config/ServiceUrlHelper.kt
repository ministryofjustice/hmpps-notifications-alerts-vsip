package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import org.springframework.stereotype.Component

@Component
class ServiceUrlHelper {
  val firstPartUrl = "https://hmpps-notifi-alerts-vsip"
  val secondPartUrl = ".prison.service.justice.gov.uk"

  fun getUrl(serverEnvironmentName: String): String = when (serverEnvironmentName.lowercase()) {
    "prod" -> firstPartUrl + secondPartUrl
    "local" -> "http://localhost:8080"
    else -> "$firstPartUrl-${serverEnvironmentName.lowercase()}$secondPartUrl"
  }
}
