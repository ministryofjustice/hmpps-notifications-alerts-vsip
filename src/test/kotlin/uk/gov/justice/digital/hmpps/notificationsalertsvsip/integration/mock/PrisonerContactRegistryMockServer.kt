package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto

class PrisonerContactRegistryMockServer(@Autowired private val objectMapper: ObjectMapper) : WireMockServer(8095) {
  fun stubGetPrisonersSocialContacts(prisonerId: String, prisonerContact: List<PrisonerContactRegistryContactDto>?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get("/v2/prisoners/$prisonerId/contacts/social?withAddress=false")
        .willReturn(
          if (prisonerContact == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisonerContact))
          },
        ),
    )
  }

  private fun getJsonString(obj: Any): String = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
}
