package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@EnableCaching
class ResourceServerConfiguration {

  @Bean
  fun visitNotifyCallbackSecurityFilter(
    http: HttpSecurity,
    @Autowired notifyCallbackAuthorizationManager: NotifyCallbackAuthorizationManager,
  ): SecurityFilterChain {
    http {
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      // Can't have CSRF protection as requires session
      csrf { disable() }

      securityMatcher("/visits/notify/callback")
      authorizeHttpRequests {
        authorize("/visits/notify/callback", notifyCallbackAuthorizationManager)
      }
    }
    return http.build()
  }

  @Throws(Exception::class)
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http {
      headers { frameOptions { sameOrigin = true } }
      sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
      // Can't have CSRF protection as requires session
      csrf { disable() }
      authorizeHttpRequests {
        listOf(
          "/health/**",
          "/info",
          "/v3/api-docs/**",
          "/swagger-ui/**",
          "/swagger-ui.html",
          "/swagger-resources",
          "/swagger-resources/configuration/ui",
          "/swagger-resources/configuration/security",
        ).forEach { authorize(it, permitAll) }
        authorize(anyRequest, authenticated)
      }
      oauth2ResourceServer { jwt { jwtAuthenticationConverter = AuthAwareTokenConverter() } }
    }
    return http.build()
  }

  @Bean
  fun locallyCachedJwtDecoder(
    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") jwkSetUri: String,
    cacheManager: CacheManager,
  ): JwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).cache(cacheManager.getCache("jwks")).build()
}
