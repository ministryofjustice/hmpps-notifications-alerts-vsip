package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.PrisonerContactRegistryContactDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.visit.scheduler.VisitDto

@Service
class PrisonerContactRegistryService(
  val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisonerContacts(visit: VisitDto): List<PrisonerContactRegistryContactDto> {
    LOG.info("PrisonerContactRegistryService getPrisonerContacts called with prisonerId - $visit.prisonerId")
    val contacts = prisonerContactRegistryClient.getPrisonersSocialContacts(visit.prisonerId)

    val visitorIds = visit.visitors.map { it.nomisPersonId.toString() }

    return contacts?.filter { contact ->
      contact.personId in visitorIds
    } ?: emptyList()
  }
}
