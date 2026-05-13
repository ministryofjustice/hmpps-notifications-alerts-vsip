package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.mock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.ContactWithOptionalPrisonerRelationshipDto

class PrisonerContactRegistryMockServer(private val objectMapper: ObjectMapper) : WireMockServer(8095) {
  fun stubSearchPrisonerContacts(
    prisonerId: String,
    contactIds: List<Long>,
    withRestrictions: Boolean = false,
    contactsList: List<ContactWithOptionalPrisonerRelationshipDto>?,
    httpStatus: HttpStatus = HttpStatus.NOT_FOUND,
  ) {
    val responseBuilder = aResponse()
      .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)

    val uri = "/v2/prisoners/$prisonerId/contacts/search?contactIds=${contactIds.joinToString(",")}&withRestrictions=$withRestrictions"

    stubFor(
      get(uri)
        .willReturn(
          if (contactsList == null) {
            responseBuilder
              .withStatus(httpStatus.value())
          } else {
            responseBuilder
              .withStatus(HttpStatus.OK.value())
              .withBody(getJsonString(contactsList))
          },
        ),
    )
  }

  private fun getJsonString(obj: Any): String = objectMapper.writer().withDefaultPrettyPrinter().writeValueAsString(obj)
}
