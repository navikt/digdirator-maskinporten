package maskinporten

import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.runBlocking
import maskinporten.config.Environment
import mu.KotlinLogging

private val log = KotlinLogging.logger { }

data class ApplicationStatus(var running: Boolean = true, var initialized: Boolean = false)

data class AppConfiguration(
    val applicationStatus: ApplicationStatus = ApplicationStatus(
        running = true,
        initialized = false
    ),
    val environment: Environment = Environment()
)

@KtorExperimentalAPI
fun main() = startServer()

@KtorExperimentalAPI
fun startServer() {
    runBlocking {
        val app = AppConfiguration()
        val applicationStatus = app.applicationStatus
        val environment = app.environment
        log.info { "Application Profile: ${environment.application.profile}" }
        val server = createHttpServer(environment, applicationStatus)

        DefaultExports.initialize()
        Runtime.getRuntime().addShutdownHook(
            Thread {
                Thread {
                    log.info { "Shutdown hook called, shutting down gracefully" }
                    applicationStatus.initialized = false
                    applicationStatus.running = false
                    server.stop(1, 5)
                }
            }
        )
        server.start(wait = true)
    }
}
