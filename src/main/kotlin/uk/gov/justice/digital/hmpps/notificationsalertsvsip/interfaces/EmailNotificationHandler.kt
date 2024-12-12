package uk.gov.justice.digital.hmpps.notificationsalertsvsip.interfaces

import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto

interface EmailNotificationHandler {
  fun handle(visit: VisitDto): SendEmailNotificationDto
}
