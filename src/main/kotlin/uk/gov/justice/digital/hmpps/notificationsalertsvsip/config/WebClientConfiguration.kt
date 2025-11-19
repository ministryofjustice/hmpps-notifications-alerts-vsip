package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfiguration(
  @param:Value("\${visit-scheduler.api.url}")
  private val visitSchedulerBaseUrl: String,

  @param:Value("\${prisoner.offender.search.url}")
  private val prisonOffenderSearchBaseUrl: String,

  @param:Value("\${prison-register.api.url}")
  private val prisonRegisterBaseUrl: String,

  @param:Value("\${booker-registry.api.url}")
  private val bookerRegistryBaseUrl: String,

  @param:Value("\${prisoner-contact.registry.url}")
  private val prisonerContactRegistryBaseUrl: String,
) {
  private enum class HmppsAuthClientRegistrationId(val clientRegistrationId: String) {
    VISIT_SCHEDULER("other-hmpps-apis"),
    PRISONER_SEARCH("other-hmpps-apis"),
    PRISON_REGISTER_CLIENT("other-hmpps-apis"),
    PRISON_CONTACT_REGISTRY_CLIENT("other-hmpps-apis"),
    BOOKER_REGISTRY("other-hmpps-apis"),
  }

  @Bean
  fun visitSchedulerWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.VISIT_SCHEDULER.clientRegistrationId)
    return getWebClient(visitSchedulerBaseUrl, oauth2Client)
  }

  @Bean
  fun prisonerOffenderSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.PRISONER_SEARCH.clientRegistrationId)
    return getWebClient(prisonOffenderSearchBaseUrl, oauth2Client)
  }

  @Bean
  fun prisonerContactRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.PRISON_CONTACT_REGISTRY_CLIENT.clientRegistrationId)
    return getWebClient(prisonerContactRegistryBaseUrl, oauth2Client)
  }

  @Bean
  fun prisonRegisterWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.PRISON_REGISTER_CLIENT.clientRegistrationId)
    return getWebClient(prisonRegisterBaseUrl, oauth2Client)
  }

  @Bean
  fun bookerRegistryWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.BOOKER_REGISTRY.clientRegistrationId)
    return getWebClient(bookerRegistryBaseUrl, oauth2Client)
  }

  private fun getOauth2Client(authorizedClientManager: OAuth2AuthorizedClientManager, clientRegistrationId: String): ServletOAuth2AuthorizedClientExchangeFilterFunction {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(clientRegistrationId)
    return oauth2Client
  }

  private fun getExchangeStrategies(): ExchangeStrategies = ExchangeStrategies.builder()
    .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
    .build()

  private fun getWebClient(baseUrl: String, oauth2Client: ServletOAuth2AuthorizedClientExchangeFilterFunction): WebClient = WebClient.builder()
    .baseUrl(baseUrl)
    .apply(oauth2Client.oauth2Configuration())
    .exchangeStrategies(getExchangeStrategies())
    .build()

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager? {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager =
      AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, oAuth2AuthorizedClientService)
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
