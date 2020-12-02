package tokenxcanary

import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import tokenxcanary.config.Environment
import mu.KotlinLogging
import kotlin.system.exitProcess

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
        val server =
            createHttpServer(environment, applicationStatus)
                .start(wait = false)

        DefaultExports.initialize()
        try {
            val job = launch {
                while (app.applicationStatus.running) {
                    delay(100)
                }
            }

            Runtime.getRuntime().addShutdownHook(
                Thread {
                    log.info { "Shutdown hook called, shutting down gracefully" }
                    applicationStatus.initialized = false
                    applicationStatus.running = false
                    server.stop(5000, 5000)
                }
            )

            app.applicationStatus.initialized = true
            log.info { "Application ready" }

            job.join()
        } finally {
            exitProcess(1)
        }
    }
}
