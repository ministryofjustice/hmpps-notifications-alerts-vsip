package uk.gov.justice.digital.hmpps.notificationsalertsvsip.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
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
    return webClient.get()
      .uri("/visits/$reference")
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitDto>().block(apiTimeout)
  }
}
