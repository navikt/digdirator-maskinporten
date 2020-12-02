package tokenxcanary.api

import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI
import tokenxcanary.config.Environment
import tokenxcanary.token.Authentication
import tokenxcanary.token.MaskinportenClient
import tokenxcanary.token.OAuth2TokenRequest

@KtorExperimentalAPI
fun Routing.token(maskinporten: Environment.Maskinporten) {
    get("/token") {
        val assertion = Authentication(maskinporten).assertion(maskinporten.scopes)

        val oAuth2TokenRequest = OAuth2TokenRequest(
            tokenEndpoint = maskinporten.metadata.tokenEndpoint,
            clientAssertion = assertion
        )
        call.respond(
            HttpStatusCode.OK,
            MaskinportenClient.tokenRequest(oAuth2TokenRequest)
        )
    }
}
