package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto

class PrisonerOffenderSearchMockServer(@param:Autowired private val objectMapper: ObjectMapper) : WireMockServer(8093) {
  fun stubGetPrisoner(prisonerId: String, prisoner: PrisonerSearchResultDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get("/prisoner/$prisonerId")
        .willReturn(
          if (prisoner == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisoner))
          },
        ),
    )
  }

  private fun getJsonString(obj: Any): String = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
}
