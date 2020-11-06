package maskinporten.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI
import maskinporten.config.Environment
import maskinporten.http.defaultHttpClient
import maskinporten.token.ClientAuthentication
import maskinporten.token.token

@KtorExperimentalAPI
fun Routing.token(environment: Environment) {
    get("/token") {
        val maskinportenConfig = environment.maskinporten
        val authentication = ClientAuthentication(maskinportenConfig)
        val tokenResponse = defaultHttpClient.token(
            maskinportenConfig.metadata.tokenEndpoint,
            authentication.clientAssertion()
        )
        call.respond(HttpStatusCode.OK, tokenResponse)
    }
}
