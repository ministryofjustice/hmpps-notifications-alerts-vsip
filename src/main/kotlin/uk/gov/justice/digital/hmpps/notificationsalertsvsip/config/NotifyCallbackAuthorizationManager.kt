package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.function.Supplier

@Service
class NotifyCallbackAuthorizationManager(
  @param:Value("\${notify.callback-token}") private val govNotifyAccessToken: String,
) : AuthorizationManager<RequestAuthorizationContext> {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }
  override fun authorize(
    authentication: Supplier<Authentication>,
    requestAuthorizationContext: RequestAuthorizationContext,
  ): AuthorizationDecision {
    var isTokenValid = false
    val providedToken = requestAuthorizationContext.request.getHeader("Authorization")?.removePrefix("Bearer ")

    // check if the token is valid
    if (providedToken != null) {
      isTokenValid = isTokenValid(providedToken)
    }

    if (!isTokenValid) {
      LOG.error("Received callback with null or invalid token")
    }

    return AuthorizationDecision(isTokenValid)
  }

  private fun isTokenValid(providedToken: String): Boolean {
    // Using MessageDigest to mitigate against timed attacks and other potential attack vectors
    return MessageDigest.isEqual(providedToken.toByteArray(), govNotifyAccessToken.toByteArray())
  }

  @Deprecated("Deprecated in Java")
  override fun check(
    authentication: Supplier<Authentication>,
    requestAuthorizationContext: RequestAuthorizationContext,
  ): AuthorizationDecision? = authorize(authentication, requestAuthorizationContext)
}
