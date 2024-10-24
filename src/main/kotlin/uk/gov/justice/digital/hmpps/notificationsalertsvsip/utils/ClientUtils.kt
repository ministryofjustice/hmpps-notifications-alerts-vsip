package uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils

import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException

@Component
class ClientUtils {
  companion object {
    fun isNotFoundError(e: Throwable?) =
      e is WebClientResponseException && e.statusCode == NOT_FOUND
  }
}
