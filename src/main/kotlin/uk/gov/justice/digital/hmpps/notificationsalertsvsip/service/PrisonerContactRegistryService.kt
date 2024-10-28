package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

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

    // TODO: VB-4332 - Loop over returned contacts and only map / return ones that match VisitDto (need to add visitors to visitDto).
    //  Add integration test to only capture visitors who are on the visitDto.
    return contacts?.filter { contact ->
      contact.personId in visitorIds
    } ?: emptyList()
  }
}
