package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import java.time.LocalDateTime

@Service
class NotificationService(
  val visitSchedulerService: VisitSchedulerService,
  val smsSenderService: SmsSenderService,
  val emailSenderService: EmailSenderService,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendMessage(visitEventType: VisitEventType, bookingReference: String) {
    LOG.info("Received call to send notification for event type $visitEventType, visit - $bookingReference")

    val visit = visitSchedulerService.getVisit(bookingReference)
    visit?.let {
      if (visit.startTimestamp > LocalDateTime.now()) {
        if (!visit.visitContact.telephone.isNullOrEmpty()) {
          smsSenderService.sendSms(visit, visitEventType)
        } else {
          LOG.info("No telephone number exists for contact on visit reference - ${visit.reference}")
        }

        if (!visit.visitContact.email.isNullOrEmpty()) {
          emailSenderService.sendEmail(visit, visitEventType)
        } else {
          LOG.info("No email exists for contact on visit reference - ${visit.reference}")
        }
      } else {
        LOG.info("Visit in past - ${visit.reference}")
      }
    }
  }
}
