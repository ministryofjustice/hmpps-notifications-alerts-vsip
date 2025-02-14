package uk.gov.justice.digital.hmpps.notificationsalertsvsip.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.config.ErrorResponse
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.dto.NotifyCallbackNotificationDto
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.external.VisitSchedulerService

const val GOV_NOTIFY_CALLBACK: String = "/visits/notify/callback"

@RestController
@Validated
@Tag(name = "1. Gov notify callback controller")
@RequestMapping(name = "Accept callback", produces = [MediaType.APPLICATION_JSON_VALUE])
class GovNotifyCallbackController(
  private val visitSchedulerService: VisitSchedulerService,
) {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PostMapping(GOV_NOTIFY_CALLBACK)
  @Operation(
    summary = "Process gov notify callback",
    description = "Accept and process the gov notify callback for notifications",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Gov notify callback processed",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun processGovNotifyCallback(
    @RequestBody
    govNotifyCallbackNotificationDto: NotifyCallbackNotificationDto,
  ) {
    govNotifyCallbackNotificationDto.eventAuditReference?.let {
      LOG.info("Gov notify callback body - {}", govNotifyCallbackNotificationDto)
      LOG.info("Received callback with valid token, processing request for event - ${govNotifyCallbackNotificationDto.eventAuditReference}")
      visitSchedulerService.processNotifyCallback(govNotifyCallbackNotificationDto)
    } ?: {
      LOG.info("Received callback with a null reference, skipping processing")
    }
  }
}
