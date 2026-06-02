package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.VisitorRequestVisitorInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames.BOOKER_VISITOR_REJECTED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.LanguagePreference

@Service
class VisitorRejectedEventBookerEmailHandler : BaseBookerEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(bookerInfoDto: BookerInfoDto, visitorInfo: VisitorRequestVisitorInfoDto): SendEmailNotificationDto {
    LOG.info("handle visitor rejected event (email) - Entered, booker reference: {}, contact details: {}", bookerInfoDto.reference, visitorInfo)
    val templateName = getTemplateName(BOOKER_VISITOR_REJECTED, LanguagePreference.EN)
    val templateVars = mapOf(
      "visitor" to visitorInfo.firstName.plus(" ").plus(visitorInfo.lastName),
    )

    return SendEmailNotificationDto(templateName, templateVars)
  }
}
