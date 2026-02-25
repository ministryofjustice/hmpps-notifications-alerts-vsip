package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonContactDetailsDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonDto

class PrisonRegisterMockServer(private val objectMapper: ObjectMapper) : WireMockServer(8096) {
  fun stubGetPrison(prisonCode: String, prison: PrisonDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get("/prisons/id/$prisonCode")
        .willReturn(
          if (prison == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prison))
          },
        ),
    )
  }

  fun stubGetPrisonSocialVisitContactDetails(prisonCode: String, prisonContactDetailsDto: PrisonContactDetailsDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    stubFor(
      get("/secure/prisons/id/$prisonCode/department/contact-details?departmentType=SOCIAL_VISIT")
        .willReturn(
          if (prisonContactDetailsDto == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(prisonContactDetailsDto))
          },
        ),
    )
  }

  private fun getJsonString(obj: Any): String = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
}
