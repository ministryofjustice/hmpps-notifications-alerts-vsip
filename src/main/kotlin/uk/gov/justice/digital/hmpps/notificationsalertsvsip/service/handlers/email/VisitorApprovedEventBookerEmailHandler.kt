package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.enums.EmailTemplateNames

@Service
class VisitorApprovedEventBookerEmailHandler : BaseBookerEmailNotificationHandler() {

  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(bookerInfoDto: BookerInfoDto, contactDto: PrisonerContactRegistryContactDto): SendEmailNotificationDto {
    LOG.info("handle visitor approved event (email) - Entered, booker reference: {}, contact details: {}", bookerInfoDto.reference, contactDto)
    val templateName = templatesConfig.emailTemplates[EmailTemplateNames.BOOKER_VISITOR_APPROVED.name]!!
    val templateVars = mapOf(
      "visitorFirstName" to contactDto.firstName,
      "visitorLastName" to contactDto.lastName,
    )

    return SendEmailNotificationDto(templateName, templateVars)
  }
}
