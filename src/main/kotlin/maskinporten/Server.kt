package maskinporten

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.util.KtorExperimentalAPI
import maskinporten.api.exceptionHandler
import maskinporten.api.selfTest
import maskinporten.api.token
import maskinporten.config.Environment
import mu.KotlinLogging
import org.slf4j.event.Level

private val log = KotlinLogging.logger { }

@KtorExperimentalAPI
fun createHttpServer(environment: Environment, applicationStatus: ApplicationStatus): NettyApplicationEngine {
    return embeddedServer(
        Netty,
        port = environment.application.port,
        module = {
            setupHttpServer(
                environment = environment,
                applicationStatus = applicationStatus
            )
        }
    )
}

@KtorExperimentalAPI
fun Application.setupHttpServer(environment: Environment, applicationStatus: ApplicationStatus) {

    log.info { "Application Profile running: ${environment.application.profile}" }

    val logLevel = Level.INFO
    log.info { "Installing log level: $logLevel" }
    install(CallLogging) {
        level = logLevel
    }
    log.info { "Installing Api-Exception handler" }
    install(StatusPages) {
        exceptionHandler()
    }
    log.info { "Installing ObjectMapper" }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(Jackson.defaultMapper))
    }
    log.info { "Installing routes" }
    install(Routing) {
        selfTest(
            readySelfTestCheck = { applicationStatus.initialized },
            aLiveSelfTestCheck = { applicationStatus.running }
        )
        token(environment)
    }
    applicationStatus.initialized = true
    log.info { "Application is up and running" }
}

object Jackson {
    val defaultMapper: ObjectMapper = jacksonObjectMapper()

    init {
        defaultMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    }
}