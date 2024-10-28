package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.client.PrisonerOffenderSearchClient
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto

@Service
class PrisonerSearchService(
  val prisonerOffenderSearchClient: PrisonerOffenderSearchClient,
) {
  private companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisoner(prisonerId: String): PrisonerSearchResultDto? {
    LOG.info("PrisonerSearchService getPrisoner called with prisonerId - $prisonerId")
    return prisonerOffenderSearchClient.getPrisoner(prisonerId)
  }
}
