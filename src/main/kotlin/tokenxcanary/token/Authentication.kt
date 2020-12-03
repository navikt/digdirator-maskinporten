package tokenxcanary.token

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.util.KtorExperimentalAPI
import mu.KotlinLogging
import tokenxcanary.common.parseToRsaKey
import tokenxcanary.config.ClientProperties
import tokenxcanary.token.Authentication.Companion.SCOPE
import java.time.Instant
import java.util.Date
import java.util.UUID

private val log = KotlinLogging.logger { }

@KtorExperimentalAPI
class Authentication(
    private val clientProperties: ClientProperties
) {
    private val rsaKey = clientProperties.clientJwk.parseToRsaKey()

    fun assertion(scopes: String? = null): String = assertion(clientProperties, rsaKey, scopes).also {
        log.info {
            "JWK with keyID: ${rsaKey.keyID} used to sign " +
                "generated JWT for integration with: ${clientProperties.metadata.issuer}"
        }
    }

    companion object {
        const val SCOPE = "scope"
    }
}

@KtorExperimentalAPI
internal fun assertion(clientProperties: ClientProperties, rsaKey: RSAKey, scopes: String?): String {
    val now = Date.from(Instant.now())
    val builder = JWTClaimsSet.Builder()

    return builder
        .commonClaims(clientProperties, now)
        .configurableClaims(clientProperties, now, scopes)
        .build()
        .signWithHeader(rsaKey)
        .serialize()
}

private fun JWTClaimsSet.Builder.commonClaims(clientProperties: ClientProperties, now: Date): JWTClaimsSet.Builder = this.issuer(clientProperties.clientId)
    .issueTime(now)
    .expirationTime(Date.from(Instant.now().plusSeconds(120)))
    .jwtID(UUID.randomUUID().toString())

@KtorExperimentalAPI
private fun JWTClaimsSet.Builder.configurableClaims(clientProperties: ClientProperties, now: Date, scopes: String?) = scopes?.let {
    // Maskinporten
    this.claim(SCOPE, it).audience(clientProperties.metadata.issuer)
    // TokenExchange
} ?: this.subject(clientProperties.clientId).notBeforeTime(now).audience(clientProperties.metadata.tokenEndpoint)

private fun JWTClaimsSet.signWithHeader(rsaKey: RSAKey): SignedJWT =
    SignedJWT(
        JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(rsaKey.keyID)
            .type(JOSEObjectType.JWT).build(),
        this
    ).apply {
        sign(RSASSASigner(rsaKey.toPrivateKey()))
    }
