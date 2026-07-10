package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.NotifyConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerSearchService

class ReplyToEmailResolverTest {
  private val prisonerSearchService: PrisonerSearchService = mock()
  private val notifyConfig = NotifyConfig().apply {
    replyToEmailIds = mapOf("DEFAULT" to "00000000-0000-0000-0000-000000000001", "MDI" to "00000000-0000-0000-0000-000000000003")
  }
  private val replyToEmailResolver = ReplyToEmailResolver(notifyConfig, prisonerSearchService)

  @Test
  fun `uses configured reply-to email id for prisoner's current prison`() {
    whenever(prisonerSearchService.getPrisoner("A1234BC")).thenReturn(
      PrisonerSearchResultDto(firstName = "John", lastName = "Smith", prisonId = "MDI"),
    )

    val replyToEmailId = replyToEmailResolver.getReplyToEmailIdForPrisoner("A1234BC")

    assertEquals("00000000-0000-0000-0000-000000000003", replyToEmailId)
  }

  @Test
  fun `uses default reply-to email id when prison does not have a configured reply-to email id`() {
    whenever(prisonerSearchService.getPrisoner("A1234BC")).thenReturn(
      PrisonerSearchResultDto(firstName = "John", lastName = "Smith", prisonId = "LEI"),
    )

    val replyToEmailId = replyToEmailResolver.getReplyToEmailIdForPrisoner("A1234BC")

    assertEquals("00000000-0000-0000-0000-000000000001", replyToEmailId)
  }

  @Test
  fun `uses default reply-to email id when prisoner search does not return a prison code`() {
    whenever(prisonerSearchService.getPrisoner("A1234BC")).thenReturn(null)

    val replyToEmailId = replyToEmailResolver.getReplyToEmailIdForPrisoner("A1234BC")

    assertEquals("00000000-0000-0000-0000-000000000001", replyToEmailId)
  }

  @Test
  fun `throws when default reply-to email id is not configured`() {
    val replyToEmailResolver = ReplyToEmailResolver(
      NotifyConfig().apply { replyToEmailIds = mapOf("MDI" to "00000000-0000-0000-0000-000000000003") },
      prisonerSearchService,
    )

    whenever(prisonerSearchService.getPrisoner("A1234BC")).thenReturn(
      PrisonerSearchResultDto(firstName = "John", lastName = "Smith", prisonId = "LEI"),
    )

    val exception = assertThrows(IllegalStateException::class.java) {
      replyToEmailResolver.getReplyToEmailIdForPrisoner("A1234BC")
    }

    assertEquals("Default reply-to email id must be configured against prison code DEFAULT", exception.message)
  }
}
