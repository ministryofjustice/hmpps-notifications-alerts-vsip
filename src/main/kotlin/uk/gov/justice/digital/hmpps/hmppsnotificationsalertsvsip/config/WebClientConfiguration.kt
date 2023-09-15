package uk.gov.justice.digital.hmpps.hmppsnotificationsalertsvsip.config

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

  @Value("\${hmpps.auth.url}")
  private val hmppsAuthUrl: String,
) {
  private enum class HmppsAuthClientRegistrationId(val clientRegistrationId: String) {
    HMPPS_AUTH_CLIENT("other-hmpps-apis"),
  }

  @Bean
  fun hmppsAuthWebClient(authorizedClientManager: OAuth2AuthorizedClientManager): WebClient {
    val oauth2Client = getOauth2Client(authorizedClientManager, HmppsAuthClientRegistrationId.HMPPS_AUTH_CLIENT.clientRegistrationId)
    return getWebClient(hmppsAuthUrl, oauth2Client)
  }

  private fun getOauth2Client(authorizedClientManager: OAuth2AuthorizedClientManager, clientRegistrationId: String): ServletOAuth2AuthorizedClientExchangeFilterFunction {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId(clientRegistrationId)
    return oauth2Client
  }

  private fun getExchangeStrategies(): ExchangeStrategies {
    return ExchangeStrategies.builder()
      .codecs { configurer: ClientCodecConfigurer -> configurer.defaultCodecs().maxInMemorySize(-1) }
      .build()
  }

  private fun getWebClient(baseUrl: String, oauth2Client: ServletOAuth2AuthorizedClientExchangeFilterFunction): WebClient {
    return WebClient.builder()
      .baseUrl(baseUrl)
      .apply(oauth2Client.oauth2Configuration())
      .exchangeStrategies(getExchangeStrategies())
      .build()
  }

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
