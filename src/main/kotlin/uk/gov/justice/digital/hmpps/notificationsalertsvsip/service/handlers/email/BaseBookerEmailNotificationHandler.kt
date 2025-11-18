package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.handlers.email

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.TemplatesConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.SendEmailNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.booker.registry.BookerInfoDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto

@Service
abstract class BaseBookerEmailNotificationHandler {
  @Autowired
  lateinit var templatesConfig: TemplatesConfig

  abstract fun handle(bookerInfoDto: BookerInfoDto, contactDto: PrisonerContactRegistryContactDto): SendEmailNotificationDto
}
