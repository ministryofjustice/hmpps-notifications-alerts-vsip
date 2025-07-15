package uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.domainevents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.events.EventFeatureSwitch
import uk.gov.justice.digital.hmpps.notificationsalertsvsip.service.listeners.notifiers.PRISON_VISIT_BOOKED

@TestPropertySource(
  properties = [
    "hmpps.sqs.enabled=false",
  ],
)
internal class EventFeatureSwitchForAllEventsTest : IntegrationTestBase() {

  @Autowired
  private lateinit var featureSwitch: EventFeatureSwitch

  @Test
  fun `should return false when feature is disabled for event`() {
    assertThat(featureSwitch.isEnabled(PRISON_VISIT_BOOKED)).isFalse
  }

  @Test
  fun `should return false when feature is disabled for all events`() {
    assertThat(featureSwitch.isAllEventsEnabled()).isFalse
  }
}
