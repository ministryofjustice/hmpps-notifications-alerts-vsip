package uk.gov.justice.digital.hmpps.notificationsalertsvsip.client

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonContactDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonDto
import java.time.Duration

@Component
class PrisonRegisterClient(
  @Qualifier("prisonRegisterWebClient") private val webClient: WebClient,
  @Value("\${prison-register.api.timeout:10s}") private val apiTimeout: Duration,
) {
  companion object{
    const val DEPARTMENT_TYPE = "SOCIAL_VISIT"
  }
  fun getPrison(prisonCode: String): PrisonDto? {
    return webClient.get().uri("/prisons/id/$prisonCode")
      .retrieve()
      .bodyToMono<PrisonDto>()
      .block(apiTimeout)
  }

  fun getSocialVisitContact(prisonCode: String): PrisonContactDetailsDto? {
    return webClient.get().uri("/secure/prisons/id/$prisonCode/department/contact-details") {
        it.queryParam("departmentType", DEPARTMENT_TYPE).build()
      }
      .retrieve()
      .bodyToMono<PrisonContactDetailsDto>()
      .block(apiTimeout)
  }
}
