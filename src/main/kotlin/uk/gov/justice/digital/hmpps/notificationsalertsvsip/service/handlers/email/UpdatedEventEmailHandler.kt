package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.visit.scheduler.VisitRestriction
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils.DateUtils.Companion.getFormattedTime

@Service
class UpdatedEventEmailHandler : BaseEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(visit: VisitDto): SendEmailNotificationDto {
    LOG.info("handleUpdatedEvent (email) - Entered")

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
      templateName = getTemplateName(EmailTemplateNames.VISIT_UPDATED),
      templateVars = templateVars,
    )
  }
}
