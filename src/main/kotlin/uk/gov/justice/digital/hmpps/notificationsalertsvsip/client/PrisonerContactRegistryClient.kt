package uk.gov.justice.digital.hmpps.notificationsalertsvsip.client

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.ContactWithOptionalPrisonerRelationshipDto
import java.time.Duration

@Component
class PrisonerContactRegistryClient(
  @param:Qualifier("prisonerContactRegistryWebClient") private val webClient: WebClient,
  @param:Value("\${prisoner-contact.registry.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun searchPrisonerContacts(prisonerId: String, contactIds: List<Long>, withRestrictions: Boolean = false): List<ContactWithOptionalPrisonerRelationshipDto>? {
    val uri = "/v2/prisoners/$prisonerId/contacts/search"

    return webClient.get()
      .uri(uri) {
        getSearchPrisonerContactsUriBuilder(contactIds, withRestrictions, it).build()
      }
      .retrieve()
      .bodyToMono<List<ContactWithOptionalPrisonerRelationshipDto>>()
      .onErrorResume { e ->
        LOG.error("searchPrisonerContacts error for get request $uri, with exception $e")
        return@onErrorResume Mono.empty()
      }
      .block(apiTimeout)
  }

  private fun getSearchPrisonerContactsUriBuilder(contactIds: List<Long>, withRestrictions: Boolean = false, uriBuilder: UriBuilder): UriBuilder {
    uriBuilder.queryParam("contactIds", contactIds.joinToString(","))
    uriBuilder.queryParam("withRestrictions", withRestrictions)
    return uriBuilder
  }
}
