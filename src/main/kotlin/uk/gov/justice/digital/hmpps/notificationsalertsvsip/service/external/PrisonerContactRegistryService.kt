package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonerContactRegistryClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.contact.registry.ContactWithOptionalPrisonerRelationshipDto

@Service
class PrisonerContactRegistryService(
  val prisonerContactRegistryClient: PrisonerContactRegistryClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun searchPrisonerContacts(prisonerId: String, contactIds: List<Long>, withRestrictions: Boolean = false): List<ContactWithOptionalPrisonerRelationshipDto> {
    LOG.info("Calling prisoner contact registry searchPrisonerContacts for prisonerId - $prisonerId and contactIds - $contactIds with restrictions - $withRestrictions")
    return prisonerContactRegistryClient.searchPrisonerContacts(prisonerId = prisonerId, contactIds = contactIds, withRestrictions = withRestrictions) ?: emptyList()
  }
}
