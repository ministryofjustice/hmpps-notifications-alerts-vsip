package uk.gov.justice.digital.hmpps.notificationsalertsvsip.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import java.time.Duration

@Component
class PrisonerContactRegistryClient(
  @Qualifier("prisonerContactRegistryWebClient") private val webClient: WebClient,
  @Value("\${prisoner-contact.registry.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonersSocialContacts(prisonerId: String): List<PrisonerContactRegistryContactDto>? {
    val uri = "/v2/prisoners/$prisonerId/contacts/social?withAddress=false"
    return webClient.get()
      .uri(uri)
      .retrieve()
      .bodyToMono<List<PrisonerContactRegistryContactDto>>()
      .onErrorResume { e ->
        LOG.error("getPrisonersSocialContacts for get request $uri, with exception $e")
        return@onErrorResume Mono.empty()
      }
      .block(apiTimeout)
  }
}
