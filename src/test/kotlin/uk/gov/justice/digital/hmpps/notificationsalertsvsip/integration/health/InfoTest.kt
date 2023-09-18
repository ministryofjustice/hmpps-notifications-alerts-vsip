package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.health

import org.assertj.core.api.Assertions.assertThat
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.IntegrationTestBase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InfoTest : IntegrationTestBase() {

  // TODO re enable when Dev ops is complete
  // @Test
  fun `Info page is accessible`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("build.name").isEqualTo("hmpps-notifications-alerts-vsip")
  }

  // TODO re enable when Dev ops is complete
  // @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("build.version").value<String> {
        assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
      }
  }
}
