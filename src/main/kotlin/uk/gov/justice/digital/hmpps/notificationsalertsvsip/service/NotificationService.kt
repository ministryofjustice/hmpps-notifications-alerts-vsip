package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.VisitSchedulerService
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.additionalinfo.VisitAdditionalInfo
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

  fun sendMessage(visitEventType: VisitEventType, additionalInfo: VisitAdditionalInfo) {
    LOG.info("Received call to send notification for event type $visitEventType, visit - ${additionalInfo.bookingReference}")

    val visit = visitSchedulerService.getVisit(additionalInfo.bookingReference)
    if (isVisitInvalidForNotifications(visit, additionalInfo)) {
      return
    }

    sendSmsNotificationIfAvailable(visit!!, visitEventType, additionalInfo)
    sendEmailNotificationIfAvailable(visit, visitEventType, additionalInfo)
  }

  private fun isVisitInvalidForNotifications(visit: VisitDto?, additionalInfo: VisitAdditionalInfo): Boolean {
    if (visit == null) {
      LOG.warn("No visit found for booking reference ${additionalInfo.bookingReference}")
      return true
    }

    if (visit.visitExternalSystemDetails != null) {
      LOG.info("External visit found for booking reference ${additionalInfo.bookingReference}, skipping notifications")
      return true
    }

    if (visit.startTimestamp < LocalDateTime.now()) {
      LOG.info("Visit in the past - ${visit.reference}")
      return true
    }

    return false
  }

  private fun sendSmsNotificationIfAvailable(visit: VisitDto, visitEventType: VisitEventType, additionalInfo: VisitAdditionalInfo) {
    val telephone = visit.visitContact.telephone
    if (telephone.isNullOrEmpty()) {
      LOG.info("No telephone number exists for contact on visit reference - ${visit.reference}")
      return
    }

    val notification = smsSenderService.sendSms(visit, visitEventType, additionalInfo.eventAuditId)
    notification?.let {
      try {
        visitSchedulerService.createNotifyNotification(it)
      } catch (e: Exception) {
        // TODO: Remove try-catch and convert to publish message 'notification-sent' instead of direct API call.
        LOG.info("Call to capture sms notification creation on visit-scheduler failed with exception: $e")
      }
    }
  }

  private fun sendEmailNotificationIfAvailable(visit: VisitDto, visitEventType: VisitEventType, additionalInfo: VisitAdditionalInfo) {
    val email = visit.visitContact.email
    if (email.isNullOrEmpty()) {
      LOG.info("No email exists for contact on visit reference - ${visit.reference}")
      return
    }

    val notification = emailSenderService.sendEmail(visit, visitEventType, additionalInfo.eventAuditId)
    notification?.let {
      try {
        visitSchedulerService.createNotifyNotification(it)
      } catch (e: Exception) {
        // TODO: Remove try-catch and convert to publish message 'notification-sent' instead of direct API call.
        LOG.info("Call to capture email notification creation on visit-scheduler failed with exception: $e")
      }
    }
  }
}
