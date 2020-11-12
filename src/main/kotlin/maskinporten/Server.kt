package maskinporten

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nimbusds.jwt.JWTParser
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
import kotlinx.coroutines.runBlocking
import maskinporten.api.selfTest
import maskinporten.api.token
import maskinporten.config.Environment
import maskinporten.token.AccessTokenResponse
import maskinporten.token.ClientAuthentication
import mu.KotlinLogging
import org.slf4j.event.Level
import java.text.ParseException

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
    log.info { "Routes" }
    install(Routing) {
        selfTest(
            readySelfTestCheck = { applicationStatus.initialized },
            aLiveSelfTestCheck = { applicationStatus.running }
        )
        token(environment)
    }
    getTokenAndValidate(environment)
    applicationStatus.initialized = true
}

object Jackson {
    val defaultMapper: ObjectMapper = jacksonObjectMapper()

    init {
        defaultMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
    }
}

@KtorExperimentalAPI
fun getTokenAndValidate(environment: Environment) {
    runBlocking {
        val jwt = ClientAuthentication(environment).tokenRequest().parseResponseJwt()
        val metadataIssuer = environment.maskinporten.metadata.issuer
        val iss = jwt.jwtClaimsSet.getStringClaim("iss")
        val clientId = jwt.jwtClaimsSet.getStringClaim("client_id")
        if (metadataIssuer == iss) {
            log.info { "Application got token from iss: $iss with client: $clientId and is ready to run" }
        } else {
            log.error { "Application could not get token, shutting down.." }
        }
    }
}

internal fun AccessTokenResponse.parseResponseJwt() =
    try {
        JWTParser.parse(this.accessToken)
    } catch (p: ParseException) {
        log.error { "Could not parse response token" }
        throw p
    }
