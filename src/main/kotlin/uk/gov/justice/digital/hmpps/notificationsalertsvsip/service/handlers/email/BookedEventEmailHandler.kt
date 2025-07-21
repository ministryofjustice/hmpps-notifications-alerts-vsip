package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_BOOKING_OR_REQUEST_APPROVED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.VISIT_REQUESTED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime

@Service
class BookedEventEmailHandler : BaseEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleBookedEvent (email) - Entered, visit reference: {}, visit sub status: {}", visit.reference, visit.visitSubStatus)

    val templateVars = getCommonTemplateVars(visit).toMutableMap()

    templateVars.putAll(
      mapOf(
        "time" to getFormattedTime(visit.startTimestamp.toLocalTime()),
        "end time" to getFormattedTime(visit.endTimestamp.toLocalTime()),
        "arrival time" to "45",
        "closed visit" to (visit.visitRestriction == VisitRestriction.CLOSED).toString(),
        "visitors" to getVisitors(visit),
      ),
    )

    return SendEmailNotificationDto(
      templateName = getTemplateName(visit),
      templateVars = templateVars,
    )
  }

  private fun getTemplateName(visit: VisitDto): String = if (visit.visitSubStatus == "REQUESTED") {
    getTemplateName(VISIT_REQUESTED)
  } else {
    getTemplateName(VISIT_BOOKING_OR_REQUEST_APPROVED)
  }
}
