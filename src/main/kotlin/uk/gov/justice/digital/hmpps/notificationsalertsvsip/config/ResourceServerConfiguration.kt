package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
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
      exceptionHandling {
        authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
        accessDeniedHandler = { _, response, _ -> response.sendError(HttpStatus.UNAUTHORIZED.value()) }
      }

      securityMatcher("/visits/notify/callback")
      authorizeHttpRequests {
        authorize("/visits/notify/callback", notifyCallbackAuthorizationManager)
      }
    }
    return http.build()
  }

  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    unauthorizedRequestPaths {
      addPaths = setOf(
        // Protected by the ingress - see Kube config in helm_deploy
        "/queue-admin/retry-all-dlqs",
      )
    }
  }
}
