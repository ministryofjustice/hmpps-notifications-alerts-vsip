package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.BookerRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestVisitorInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.exception.NotFoundException
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorApprovedAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorRejectedAdditionalInfo

@Service
class BookerNotificationService(
  val emailSenderService: EmailSenderService,
  val bookerRegistryClient: BookerRegistryClient,
  val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendVisitorRequestApprovedEmail(bookerEventType: BookerEventType, additionalInfo: VisitorApprovedAdditionalInfo) {
    LOG.info("Received call to send approval notification for event type $bookerEventType, additional info - $additionalInfo")
    val bookerDetails = bookerRegistryClient.getBookerByBookerReference(additionalInfo.bookerReference) ?: throw NotFoundException("Booker details not found for reference ${additionalInfo.bookerReference}")
    val visitorDetails = VisitorRequestVisitorInfoDto(prisonerContactRegistryClient.getPrisonersSocialContacts(additionalInfo.prisonerId)?.firstOrNull { it.personId == additionalInfo.visitorId } ?: throw NotFoundException("Visitor details not found for prisonerId - ${additionalInfo.prisonerId} and visitorId - ${additionalInfo.visitorId}"))

    emailSenderService.sendBookerVisitorEmail(bookerDetails, visitorDetails, BookerEventType.VISITOR_APPROVED)
  }

  fun sendVisitorRequestRejectedEmail(bookerEventType: BookerEventType, additionalInfo: VisitorRejectedAdditionalInfo) {
    LOG.info("Received call to send rejection notification for event type $bookerEventType, additional info - $additionalInfo")
    val visitorRequest = bookerRegistryClient.getVisitorRequestByReference(additionalInfo.requestReference)

    val bookerInfo = BookerInfoDto(reference = visitorRequest.bookerReference, email = visitorRequest.bookerEmail)
    val visitorDetails = VisitorRequestVisitorInfoDto(visitorRequest)

    emailSenderService.sendBookerVisitorEmail(bookerInfo, visitorDetails, BookerEventType.VISITOR_REJECTED)
  }
}
