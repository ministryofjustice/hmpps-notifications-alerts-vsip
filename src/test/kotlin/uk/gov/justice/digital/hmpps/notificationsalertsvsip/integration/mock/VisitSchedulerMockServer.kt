package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.put
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto

class VisitSchedulerMockServer(@param:Autowired private val objectMapper: ObjectMapper) : WireMockServer(8092) {
  fun stubGetVisit(reference: String, visitDto: VisitDto?, httpStatus: HttpStatus = HttpStatus.NOT_FOUND) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      get("/visits/$reference")
        .willReturn(
          if (visitDto == null) {
            responseBuilder.withStatus(httpStatus.value())
          } else {
            responseBuilder.withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(visitDto))
          },
        ),
    )
  }

  fun stubCreateNotifyNotification(httpStatus: HttpStatus) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      put("/visits/notify/create")
        .willReturn(responseBuilder.withStatus(httpStatus.value())),
    )
  }

  fun stubProcessNotifyCallbackNotification(httpStatus: HttpStatus) {
    val responseBuilder = createJsonResponseBuilder()
    stubFor(
      put("/visits/notify/callback")
        .willReturn(responseBuilder.withStatus(httpStatus.value())),
    )
  }

  private fun getJsonString(obj: VisitDto): String = objectMapper.writer().writeValueAsString(obj)

  private fun createJsonResponseBuilder(): ResponseDefinitionBuilder = aResponse().withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
}
