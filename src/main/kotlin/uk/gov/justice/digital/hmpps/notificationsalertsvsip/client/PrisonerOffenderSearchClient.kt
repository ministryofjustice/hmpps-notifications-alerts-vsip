package uk.gov.justice.digital.hmpps.notificationsalertsvsip.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.ClientUtils.Companion.isNotFoundError
import java.time.Duration

@Component
class PrisonerOffenderSearchClient(
  @Qualifier("prisonerOffenderSearchWebClient") private val webClient: WebClient,
  @Value("\${prisoner.offender.search.timeout:10s}") private val apiTimeout: Duration,
) {

  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
    private val PRISONER_SEARCH_RESULT_DTO = object : ParameterizedTypeReference<PrisonerSearchResultDto>() {}
  }

  fun getPrisoner(prisonerId: String): PrisonerSearchResultDto? {
    val uri = "/prisoner/$prisonerId"
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono(PRISONER_SEARCH_RESULT_DTO)
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("getPrisoner Failed get request $uri")
          Mono.error(e)
        } else {
          LOG.info("getPrisoner Not Found get request $uri")
          return@onErrorResume Mono.empty()
        }
      }
      .block(apiTimeout)
  }
}
