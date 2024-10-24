package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.VisitSchedulerClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.VisitEventType
import java.time.LocalDateTime

@Service
class NotificationService(
  val visitSchedulerClient: VisitSchedulerClient,
  val smsSenderService: SmsSenderService,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendMessage(visitEventType: VisitEventType, bookingReference: String) {
    LOG.info("Visit booked event with reference - $bookingReference")

    val visit = getVisit(bookingReference)

    visit?.let {
      if (visit.startTimestamp > LocalDateTime.now() && !visit.visitContact.telephone.isNullOrEmpty()) {
        smsSenderService.sendSms(visit, visitEventType)
      } else {
        LOG.info("Visit in past or no telephone number exists for contact on visit reference - ${visit.reference}")
      }
    }
  }

  private fun getVisit(bookingReference: String): VisitDto? {
    try {
      return visitSchedulerClient.getVisitByReference(bookingReference)
    } catch (e: Exception) {
      // if there was an error getting the visit return null
      if (e is WebClientResponseException && e.statusCode == HttpStatus.NOT_FOUND) {
        LOG.error("no visit found with booking reference - $bookingReference")
        return null
      }
      throw e
    }
  }
}
