package uk.gov.justice.digital.hmpps.notificationsalertsvsip.utils

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class EmailReferenceGeneratorUtil {
  fun generateReference(): String = UUID.randomUUID().toString()
}
