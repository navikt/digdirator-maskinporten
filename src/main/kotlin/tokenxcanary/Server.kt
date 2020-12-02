package tokenxcanary

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.request.path
import io.ktor.routing.Routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.util.KtorExperimentalAPI
import tokenxcanary.api.selfTest
import tokenxcanary.api.token
import tokenxcanary.common.getTokenAndValidate
import tokenxcanary.common.setCurrent
import tokenxcanary.config.Environment
import tokenxcanary.redis.Cache
import tokenxcanary.token.ClientAuthentication
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

    val maskinporten = environment.maskinporten

    log.info { "Installing:" }
    val logLevel = Level.INFO
    log.info { "Log level -> $logLevel" }
    install(CallLogging) {
        level = logLevel
        filter { call -> call.request.path().startsWith("/token") }
    }
    log.info { "ContentNegotiation Configuration" }
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(Jackson.defaultMapper))
    }

    log.info { "Client Authentication" }
    val auth = ClientAuthentication(maskinporten)

    log.info { "Cache" }
    Cache(environment).setCurrent(maskinporten.clientJwk)

    log.info { "Routes" }
    install(Routing) {
        selfTest(
            readySelfTestCheck = { applicationStatus.initialized },
            aLiveSelfTestCheck = { applicationStatus.running }
        )
        token(maskinporten)
    }

    getTokenAndValidate(maskinporten.metadata.issuer, auth)
    applicationStatus.initialized = true
}

object Jackson {
    val defaultMapper: ObjectMapper = jacksonObjectMapper()

    init {
        defaultMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    }
}
