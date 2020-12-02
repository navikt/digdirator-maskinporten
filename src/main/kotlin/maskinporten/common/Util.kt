package maskinporten.common

import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTParser
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import maskinporten.redis.Cache
import maskinporten.token.AccessTokenResponse
import maskinporten.token.ClientAuthentication
import mu.KotlinLogging
import java.text.ParseException

private val log = KotlinLogging.logger { }

internal fun String.parseToRsaKey(): RSAKey = RSAKey.parse(this)

@KtorExperimentalAPI
fun Cache.setCurrent(currentJwk: String) {
    get()?.let { cachedJwk ->
        if (cachedJwk.parseToRsaKey().keyID != currentJwk.parseToRsaKey().keyID) {
            set(currentJwk)
        } else {
            throw RuntimeException("JWK was not rotated")
        }
    } ?: set(currentJwk)
}

@KtorExperimentalAPI
internal fun getTokenAndValidate(issuer: String, auth: ClientAuthentication) {
    runBlocking {
        val jwt = auth.tokenRequest().parseResponseJwt()
        val iss = jwt.jwtClaimsSet.getStringClaim("iss")
        val clientId = jwt.jwtClaimsSet.getStringClaim("client_id")
        if (issuer == iss) {
            log.info { "Application got token from iss: $iss with client: $clientId and is ready to run" }
        } else {
            log.error { "Application could not get token, shutting down.." }
            throw RuntimeException()
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
