package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.GET_BOOKER_BY_BOOKING_REFERENCE
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto

class BookerRegistryMockServer(@param:Autowired private val objectMapper: ObjectMapper) : WireMockServer(8097) {
  fun stubGetBooker(bookerReference: String, bookerInfoDto: BookerInfoDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get(GET_BOOKER_BY_BOOKING_REFERENCE.replace("{bookerReference}", bookerReference))
        .willReturn(
          if (bookerInfoDto == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(bookerInfoDto))
          },
        ),
    )
  }

  private fun getJsonString(obj: Any): String = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
}
