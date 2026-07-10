package uk.gov.justice.digital.hmpps.notificationsalertsvsip.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.NotifyConfig
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.PrisonerSearchService

@Service
class ReplyToEmailResolver(
  private val notifyConfig: NotifyConfig,
  private val prisonerSearchService: PrisonerSearchService,
) {
  companion object {
    const val DEFAULT_REPLY_TO_EMAIL_PRISON_CODE = "DEFAULT"

    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  fun getReplyToEmailIdForPrisoner(prisonerId: String): String {
    val prisonCode = prisonerSearchService.getPrisoner(prisonerId)?.prisonId

    if (prisonCode.isNullOrBlank()) {
      LOG.info("Could not resolve prison code for prisoner $prisonerId, using default reply-to email id")
      return getDefaultReplyToEmailId()
    }

    return getReplyToEmailIdForPrison(prisonCode)
  }

  fun getReplyToEmailIdForPrison(prisonCode: String): String {
    val replyToEmailId = notifyConfig.replyToEmailIds[prisonCode]

    if (replyToEmailId.isNullOrBlank()) {
      LOG.info("No reply-to email id configured for prison $prisonCode, using default reply-to email id")
      return getDefaultReplyToEmailId()
    }

    return replyToEmailId
  }

  private fun getDefaultReplyToEmailId(): String = notifyConfig.replyToEmailIds[DEFAULT_REPLY_TO_EMAIL_PRISON_CODE]
    ?.takeIf { it.isNotBlank() }
    ?: throw IllegalStateException("Default reply-to email id must be configured against prison code $DEFAULT_REPLY_TO_EMAIL_PRISON_CODE")
}
