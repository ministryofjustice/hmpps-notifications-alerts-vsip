package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.NotifyEmailConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.prisoner.search.PrisonerSearchResultDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerSearchService

class ReplyToEmailServiceTest {
  private val prisonerSearchService: PrisonerSearchService = mock()
  private val notifyEmailConfig = NotifyEmailConfig().apply {
    defaultReplyToEmailId = "blah@test.com"
    replyToEmailIds = mapOf("MDI" to "mdi-reply-to-id")
  }
  private val replyToEmailService = ReplyToEmailService(notifyEmailConfig, prisonerSearchService)

  @Test
  fun `uses configured reply-to email id for prisoner's current prison`() {
    whenever(prisonerSearchService.getPrisoner("A1234BC")).thenReturn(
      PrisonerSearchResultDto(firstName = "John", lastName = "Smith", prisonId = "MDI"),
    )

    val replyToEmailId = replyToEmailService.getReplyToEmailIdForPrisoner("A1234BC")

    assertEquals("mdi-reply-to-id", replyToEmailId)
  }

  @Test
  fun `uses default reply-to email id when prison does not have a configured reply-to email id`() {
    whenever(prisonerSearchService.getPrisoner("A1234BC")).thenReturn(
      PrisonerSearchResultDto(firstName = "John", lastName = "Smith", prisonId = "LEI"),
    )

    val replyToEmailId = replyToEmailService.getReplyToEmailIdForPrisoner("A1234BC")

    assertEquals("blah@test.com", replyToEmailId)
  }

  @Test
  fun `uses default reply-to email id when prisoner search does not return a prison code`() {
    whenever(prisonerSearchService.getPrisoner("A1234BC")).thenReturn(null)

    val replyToEmailId = replyToEmailService.getReplyToEmailIdForPrisoner("A1234BC")

    assertEquals("blah@test.com", replyToEmailId)
  }
}
