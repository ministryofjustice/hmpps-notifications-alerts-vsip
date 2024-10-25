package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonerOffenderSearchClient

@Service
class PrisonerSearchService(
  val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisoner(prisonerId: String): String? {
    LOG.info("PrisonerSearchService getPrisoner called with prisonerId - $prisonerId")
    return prisonerOffenderSearchClient.getPrisoner(prisonerId)?.let {
      return it.firstName + " " + it.lastName
    } ?: return null
  }
}
