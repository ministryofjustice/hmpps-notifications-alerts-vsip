package uk.gov.justice.digital.hmpps.notificationsalertsvsip.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Configuration
class SwaggerConfig {
  init {
    val schema = io.swagger.v3.oas.models.media.Schema<LocalTime>()
    schema.example(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))).type("string").format("HH:mm").example("13:45")
    SpringDocUtils.getConfig().replaceWithSchema(LocalTime::class.java, schema)
  }
}

@Configuration
class OpenApiConfiguration(
  buildProperties: BuildProperties,
  val serviceUrlHelper: ServiceUrlHelper,
) {
  private val buildName: String = buildProperties.name
  private val buildVersion: String = buildProperties.version

  @Value("\${info.app.description}")
  private val description: String = "VSIP notifications service is a notify users via various channels to changes to visits"

  @Value("\${info.app.contact.name}")
  private val contactName: String = "Prison Visits Booking Project"

  @Value("\${info.app.contact.email}")
  private val contactEmail: String = "prisonvisitsbooking@digital.justice.gov.uk"

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url(serviceUrlHelper.getUrl("prod")).description("Production server"),
        Server().url(serviceUrlHelper.getUrl("preprod")).description("PreProd server"),
        Server().url(serviceUrlHelper.getUrl("staging")).description("Staging server"),
        Server().url(serviceUrlHelper.getUrl("dev")).description("Development server"),
        Server().url(serviceUrlHelper.getUrl("local")).description("Local server"),
      ),
    )
    .info(
      Info().title(buildName)
        .version(buildVersion)
        .description(description)
        .contact(Contact().name(contactName).email(contactEmail)),
    )
}
