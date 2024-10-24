package uk.gov.justice.digital.hmpps.notificationsalertsvsip.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.VisitSchedulerClient.Companion.LOG
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonContactDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.ClientUtils.Companion.isNotFoundError
import java.time.Duration

@Component
class PrisonRegisterClient(
  @Qualifier("prisonRegisterWebClient") private val webClient: WebClient,
  @Value("\${prison-register.api.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object {
    const val DEPARTMENT_TYPE = "SOCIAL_VISIT"
  }

  fun getPrison(prisonCode: String): PrisonDto? {
    val uri = "/prisons/id/$prisonCode"

    return webClient.get().uri(uri)
      .retrieve()
      .bodyToMono<PrisonDto>()
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("getPrison Failed get request $uri")
          Mono.error(e)
        } else {
          LOG.info("getPrison Not Found get request $uri")
          return@onErrorResume Mono.empty()
        }
      }
      .block(apiTimeout)
  }

  fun getSocialVisitContact(prisonCode: String): PrisonContactDetailsDto? {
    val uri = "/secure/prisons/id/$prisonCode/department/contact-details"

    return webClient.get().uri(uri) {
      it.queryParam("departmentType", DEPARTMENT_TYPE).build()
    }
      .retrieve()
      .bodyToMono<PrisonContactDetailsDto>()
      .onErrorResume {
          e ->
        if (!isNotFoundError(e)) {
          LOG.error("getSocialVisitContact Failed get request $uri")
          Mono.error(e)
        } else {
          LOG.info("getSocialVisitContact Not Found get request $uri")
          return@onErrorResume Mono.empty()
        }
      }
      .block(apiTimeout)
  }
}
