package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerVisitorDto
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PrisonerContactRegistryService(
  val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerContacts(prisonerId: String): List<PrisonerVisitorDto>? {
    LOG.info("PrisonerContactRegistryService getPrisonerContacts called with prisonerId - $prisonerId")
    val prisonerContactRegistryClientResult = prisonerContactRegistryClient.getPrisonersSocialContacts(prisonerId)

    return prisonerContactRegistryClientResult?.map { contact ->
      val age = contact.dateOfBirth?.let {
        ChronoUnit.YEARS.between(it, LocalDate.now()).toInt()
      }

      PrisonerVisitorDto(
        firstName = contact.firstName,
        lastName = contact.lastName,
        age = age,
      )
    } ?: emptyList()
  }
}
