package tokenxcanary.token

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parametersOf
import io.ktor.util.KtorExperimentalAPI
import tokenxcanary.http.defaultHttpClient
import tokenxcanary.http.withLogAndErrorHandling

@KtorExperimentalAPI
class TokenXClient {

    companion object {
        internal const val PARAMS_GRANT_TYPE = "grant_type"
        internal const val GRANT_TYPE = "urn:ietf:params:oauth:grant-type:token-exchange"
        internal const val PARAMS_SUBJECT_TOKEN_TYPE = "subject_token_type"
        internal const val SUBJECT_TOKEN_TYPE = "urn:ietf:params:oauth:token-type:jwt"
        internal const val PARAMS_SUBJECT_TOKEN = "subject_token"
        internal const val PARAMS_AUDIENCE = "audience"
        internal const val PARAMS_CLIENT_ASSERTION = "client_assertion"
        internal const val PARAMS_CLIENT_ASSERTION_TYPE = "client_assertion_type"
        internal const val CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer"

        suspend fun token(oAuth2TokenExchangeRequest: OAuth2TokenExchangeRequest) =
            defaultHttpClient.tokenExchange(oAuth2TokenExchangeRequest)
    }
}

@KtorExperimentalAPI
private suspend fun HttpClient.tokenExchange(request: OAuth2TokenExchangeRequest) =
    withLogAndErrorHandling("Requesting token from", request.tokenEndpoint) {
        this.submitForm<AccessTokenResponse>(
            url = request.tokenEndpoint,
            formParameters = parametersOf(
                TokenXClient.PARAMS_CLIENT_ASSERTION to listOf(request.clientAssertion),
                TokenXClient.PARAMS_CLIENT_ASSERTION_TYPE to listOf(TokenXClient.CLIENT_ASSERTION_TYPE),
                TokenXClient.PARAMS_GRANT_TYPE to listOf(TokenXClient.GRANT_TYPE),
                TokenXClient.PARAMS_SUBJECT_TOKEN to listOf(request.subjectToken),
                TokenXClient.PARAMS_SUBJECT_TOKEN_TYPE to listOf(TokenXClient.SUBJECT_TOKEN_TYPE),
                TokenXClient.PARAMS_AUDIENCE to listOf(request.audience)
            )
        )
    }

@KtorExperimentalAPI
data class OAuth2TokenExchangeRequest(
    val clientAssertion: String,
    val subjectToken: String,
    val audience: String,
    val tokenEndpoint: String
)
