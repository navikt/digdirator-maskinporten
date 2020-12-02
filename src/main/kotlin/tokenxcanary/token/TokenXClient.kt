package tokenxcanary.token

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parametersOf
import io.ktor.util.KtorExperimentalAPI
import tokenxcanary.http.defaultHttpClient
import tokenxcanary.http.withLogAndErrorHandling

@KtorExperimentalAPI
class TokenX {

    suspend fun tokenRequest(oAuth2TokenExchangeRequest: OAuth2TokenExchangeRequest) =
        defaultHttpClient.tokenExchange(oAuth2TokenExchangeRequest)

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
    }
}

@KtorExperimentalAPI
private suspend fun HttpClient.tokenExchange(request: OAuth2TokenExchangeRequest) =
    withLogAndErrorHandling("Requesting token from", request.tokenEndpoint) {
        this.submitForm<AccessTokenResponse>(
            url = request.tokenEndpoint,
            formParameters = parametersOf(
                TokenX.PARAMS_CLIENT_ASSERTION to listOf(request.clientAssertion),
                TokenX.PARAMS_CLIENT_ASSERTION_TYPE to listOf(TokenX.CLIENT_ASSERTION_TYPE),
                TokenX.PARAMS_GRANT_TYPE to listOf(TokenX.GRANT_TYPE),
                TokenX.PARAMS_SUBJECT_TOKEN to listOf(request.subjectToken),
                TokenX.PARAMS_SUBJECT_TOKEN_TYPE to listOf(TokenX.SUBJECT_TOKEN_TYPE),
                TokenX.PARAMS_AUDIENCE to listOf(request.audience)
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
