package tokenxcanary.token

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parametersOf
import io.ktor.util.KtorExperimentalAPI
import tokenxcanary.http.defaultHttpClient
import tokenxcanary.http.withLogAndErrorHandling

@KtorExperimentalAPI
class MaskinportenClient {

    companion object {
        internal const val PARAMS_GRANT_TYPE = "grant_type"
        internal const val GRANT_TYPE_JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer"
        internal const val PARAMS_ASSERTION = "assertion"

        suspend fun token(oAuth2TokenRequest: OAuth2TokenRequest) =
            defaultHttpClient.token(oAuth2TokenRequest)
    }
}

@KtorExperimentalAPI
suspend fun HttpClient.token(request: OAuth2TokenRequest) =
    withLogAndErrorHandling("Requesting token from", request.tokenEndpoint) {
        this.submitForm<AccessTokenResponse>(
            url = request.tokenEndpoint,
            formParameters = parametersOf(
                MaskinportenClient.PARAMS_ASSERTION to listOf(request.clientAssertion),
                MaskinportenClient.PARAMS_GRANT_TYPE to listOf(MaskinportenClient.GRANT_TYPE_JWT_BEARER)
            )
        )
    }

data class OAuth2TokenRequest(
    val clientAssertion: String,
    val tokenEndpoint: String,
)
