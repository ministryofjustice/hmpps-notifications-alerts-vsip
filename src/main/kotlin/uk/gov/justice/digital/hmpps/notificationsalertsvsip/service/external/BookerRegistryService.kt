package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.BookerRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestDto

@Service
class BookerRegistryService(
  val bookerRegistryClient: BookerRegistryClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getBookerByBookerReference(bookerReference: String): BookerInfoDto {
    LOG.info("BookerRegistryService getBookerByBookerReference called with bookerReference - $bookerReference")
    return bookerRegistryClient.getBookerByBookerReference(bookerReference)
  }

  fun getVisitorRequestByReference(visitorRequestReference: String): VisitorRequestDto {
    LOG.info("BookerRegistryService getVisitorRequestByReference called with visitorRequestReference - $visitorRequestReference")
    return bookerRegistryClient.getVisitorRequestByReference(visitorRequestReference)
  }
}
