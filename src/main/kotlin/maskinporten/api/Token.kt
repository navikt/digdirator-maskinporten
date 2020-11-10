package maskinporten.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI
import maskinporten.config.Environment
import maskinporten.token.ClientAuthentication

@KtorExperimentalAPI
fun Routing.token(env: Environment) {
    get("/token") {
        call.respond(HttpStatusCode.OK, ClientAuthentication(env).tokenRequest())
    }
}
