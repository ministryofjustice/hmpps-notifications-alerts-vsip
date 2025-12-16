package uk.gov.justice.digital.hmpps.notificationsalertsvsip.client

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.ClientUtils.Companion.isNotFoundError
import java.time.Duration

const val BOOKER_ADMIN_ENDPOINT = "/public/booker/config"
const val GET_BOOKER_BY_BOOKING_REFERENCE: String = "$BOOKER_ADMIN_ENDPOINT/{bookerReference}"

const val GET_VISITOR_REQUEST_BY_REFERENCE: String = "/visitor-requests/{requestReference}"

@Component
class BookerRegistryClient(
  @param:Qualifier("bookerRegistryWebClient") private val webClient: WebClient,
  @param:Value("\${booker-registry.api.timeout:10s}") private val apiTimeout: Duration,
  val objectMapper: ObjectMapper,
) {
  companion object {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getBookerByBookerReference(bookerReference: String): BookerInfoDto? {
    val uri = GET_BOOKER_BY_BOOKING_REFERENCE.replace("{bookerReference}", bookerReference)
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<BookerInfoDto>()
      .onErrorResume { e ->
        if (!isNotFoundError(e)) {
          logger.error("getBookerByBookerReference - failed get request $uri")
          Mono.error(e)
        } else {
          logger.error("getBookerByBookerReference NOT_FOUND for get request $uri")
          return@onErrorResume Mono.empty()
        }
      }
      .block(apiTimeout)
  }

  fun getVisitorRequestByReference(visitorRequestReference: String): VisitorRequestDto {
    val uri = GET_VISITOR_REQUEST_BY_REFERENCE.replace("{requestReference}", visitorRequestReference)
    return webClient.get()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .retrieve()
      .bodyToMono<VisitorRequestDto>()
      .onErrorResume { e ->
        logger.error("getVisitorRequestByReference - failed get request $uri")
        Mono.error(e)
      }
      .block(apiTimeout) ?: throw IllegalStateException("Visitor request no response for request with reference $visitorRequestReference")
  }
}
