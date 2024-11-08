package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonRegisterClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prison.register.PrisonContactDetailsDto

@Service
class PrisonRegisterService(
  val prisonRegisterClient: PrisonRegisterClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonSocialVisitsContactDetails(prisonCode: String): PrisonContactDetailsDto? {
    LOG.info("PrisonRegisterService getPrisonSocialVisitsContactNumber entered, prison code - $prisonCode")
    return prisonRegisterClient.getSocialVisitContact(prisonCode)
  }

  fun getPrisonName(prisonCode: String): String {
    LOG.info("PrisonRegisterService getPrisonName entered, prison code - $prisonCode")
    return prisonRegisterClient.getPrison(prisonCode)?.prisonName ?: prisonCode
  }
}
