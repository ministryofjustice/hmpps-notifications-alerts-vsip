package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.NotifyEmailConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerSearchService

@Service
class ReplyToEmailService(
  private val notifyEmailConfig: NotifyEmailConfig,
  private val prisonerSearchService: PrisonerSearchService,
) {
  companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun getReplyToEmailIdForPrisoner(prisonerId: String): String {
    val prisonCode = prisonerSearchService.getPrisoner(prisonerId)?.prisonId

    if (prisonCode.isNullOrBlank()) {
      LOG.info("Could not resolve prison code for prisoner $prisonerId, using default reply-to email id")
      return notifyEmailConfig.defaultReplyToEmailId
    }

    return getReplyToEmailIdForPrison(prisonCode)
  }

  fun getReplyToEmailIdForPrison(prisonCode: String): String {
    val replyToEmailId = notifyEmailConfig.replyToEmailIds[prisonCode]

    if (replyToEmailId.isNullOrBlank()) {
      LOG.info("No reply-to email id configured for prison $prisonCode, using default reply-to email id")
      return notifyEmailConfig.defaultReplyToEmailId
    }

    return replyToEmailId
  }
}
