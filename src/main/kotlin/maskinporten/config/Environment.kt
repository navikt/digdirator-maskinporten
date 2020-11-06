package maskinporten.config

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import maskinporten.http.defaultHttpClient
import maskinporten.http.getOAuthServerConfigurationMetadata
import maskinporten.token.OauthServerConfigurationMetadata
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

private val config: Configuration =
    systemProperties() overriding
        EnvironmentVariables()

data class Environment(
    val application: Application = Application(),
    val maskinporten: Maskinporten = Maskinporten(),
) {

    data class Application(
        val profile: String = config.getOrElse(Key("application.profile", stringType), "TEST"),
        val port: Int = config.getOrElse(Key("application.port", intType), 8080),
    )

    data class Maskinporten(
        val wellKnownUrl: String = config.getOrElse(
            Key("maskinporten.well.known.url", stringType),
            "https://ver2.maskinporten/.well-known/oauth-authorization-server"
        ),
        val clientId: String = config.getOrElse(Key("maskinporten.client.id", stringType), "clientID"),
        val privateJwk: String = config.getOrElse(
            Key("maskinporten.private.jwk", stringType),
            generateRsaKey().toJSONObject().toJSONString()
        ),
        val scopes: String = config.getOrElse(Key("maskinporten.scopes", stringType), "nav:scope1 nav:scope2"),
    ) {
        @KtorExperimentalAPI
        val metadata: OauthServerConfigurationMetadata =
            runBlocking {
                defaultHttpClient.getOAuthServerConfigurationMetadata(wellKnownUrl)
            }
    }
}

internal fun generateRsaKey(keyId: String = UUID.randomUUID().toString(), keySize: Int = 2048): RSAKey =
    KeyPairGenerator.getInstance("RSA").apply { initialize(keySize) }.generateKeyPair()
        .let {
            RSAKey.Builder(it.public as RSAPublicKey)
                .privateKey(it.private as RSAPrivateKey)
                .keyID(keyId)
                .keyUse(KeyUse.SIGNATURE)
                .build()
        }
