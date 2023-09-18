package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CANCELLED
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_CHANGED

@TestPropertySource(
  properties = [
    "feature.event.$PRISON_VISIT_BOOKED=true",
    "feature.event.$PRISON_VISIT_CANCELLED=false",
  ],
)
internal class EventFeatureSwitchTest : IntegrationTestBase() {

  @Autowired
  private lateinit var featureSwitch: EventFeatureSwitch

  @Test
  fun `should return true when feature is enabled`() {
    assertThat(featureSwitch.isEnabled(PRISON_VISIT_BOOKED)).isTrue
  }

  @Test
  fun `should return false when feature is disabled`() {
    assertThat(featureSwitch.isEnabled(PRISON_VISIT_CANCELLED)).isFalse
  }

  @Test
  fun `should return true when feature switch is not present`() {
    assertThat(featureSwitch.isEnabled(PRISON_VISIT_CHANGED)).isTrue
  }
}
