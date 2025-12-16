package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestVisitorInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames

@Service
class VisitorRejectedAlreadyLinkedEventBookerEmailHandler : BaseBookerEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(bookerInfoDto: BookerInfoDto, visitorInfo: VisitorRequestVisitorInfoDto): SendEmailNotificationDto {
    LOG.info("handle visitor rejected already linked event (email) - Entered, booker reference: {}, contact details: {}", bookerInfoDto.reference, visitorInfo)
    val templateName = templatesConfig.emailTemplates[EmailTemplateNames.BOOKER_VISITOR_REJECTED_ALREADY_LINKED.name]!!
    val templateVars = mapOf(
      "visitor" to visitorInfo.firstName.plus(" ").plus(visitorInfo.lastName),
    )

    return SendEmailNotificationDto(templateName, templateVars)
  }
}
