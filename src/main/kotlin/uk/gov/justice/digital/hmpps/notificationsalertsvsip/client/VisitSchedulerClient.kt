package uk.gov.justice.digital.hmpps.notificationsalertsvsip.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import java.time.Duration

@Component
class VisitSchedulerClient(
  @Qualifier("visitSchedulerWebClient") private val webClient: WebClient,
  @Value("\${visit-scheduler.api.timeout:10s}") val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getVisitByReference(reference: String): VisitDto? {
    val uri = "/visits/$reference"
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>()
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("getVisitByReference Failed get request $uri")
          Mono.error(e)
        } else {
          LOG.info("getVisitByReference Not Found get request $uri")
          return@onErrorResume Mono.empty()
        }
      }
      .block(apiTimeout)
  }

  fun isNotFoundError(e: Throwable?) =
    e is WebClientResponseException && e.statusCode == NOT_FOUND
}
