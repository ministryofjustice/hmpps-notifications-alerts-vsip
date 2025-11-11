package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.BookerRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.booker.registry.BookerEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.exception.NotFoundException
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitorApprovedAdditionalInfo
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.EmailReferenceGeneratorUtil

@Service
class BookerNotificationService(
  val emailSenderService: EmailSenderService,
  val bookerRegistryClient: BookerRegistryClient,
  val prisonerContactRegistryClient: PrisonerContactRegistryClient,
  val emailReferenceGeneratorUtil: EmailReferenceGeneratorUtil,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendMessage(bookerEventType: BookerEventType, additionalInfo: VisitorApprovedAdditionalInfo) {
    LOG.info("Received call to send notification for event type $bookerEventType, additional info - $additionalInfo")
    val bookerDetails = bookerRegistryClient.getBookerByBookerReference(additionalInfo.bookerReference) ?: throw NotFoundException("Booker details not found for reference ${additionalInfo.bookerReference}")
    val visitorDetails = prisonerContactRegistryClient.getPrisonersSocialContacts(additionalInfo.prisonerId)?.firstOrNull { it.personId == additionalInfo.visitorId }

    if (visitorDetails != null) {
      sendEmailNotificationWhenVisitorApproved(bookerDetails, visitorDetails)
    } else {
      LOG.error("Visitor details not found for prisonerId ${additionalInfo.prisonerId} and visitorId ${additionalInfo.visitorId}")
    }
  }

  private fun sendEmailNotificationWhenVisitorApproved(bookerInfoDto: BookerInfoDto, contactDto: PrisonerContactRegistryContactDto) {
    // TODO - what needs to be the reference- currently using a random value?
    val reference = emailReferenceGeneratorUtil.generateReference()
    emailSenderService.sendBookerContactEmail(bookerInfoDto, contactDto, BookerEventType.VISITOR_APPROVED, reference)
    LOG.info("Email notification sent for event type ${BookerEventType.VISITOR_APPROVED}, reference $reference")
  }
}
