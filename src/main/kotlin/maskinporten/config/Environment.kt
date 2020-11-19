package maskinporten.config

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.runBlocking
import maskinporten.http.defaultHttpClient
import maskinporten.http.configurationMetadata
import maskinporten.token.OauthServerConfigurationMetadata

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
        val wellKnownUrl: String = config[Key("maskinporten.well.known.url", stringType)],
        val clientId: String = config[Key("maskinporten.client.id", stringType)],
        val privateJwk: String = config[Key("maskinporten.client.jwk", stringType)],
        val scopes: String = config[Key("maskinporten.scopes", stringType)],
    ) {
        @KtorExperimentalAPI
        val metadata: OauthServerConfigurationMetadata =
            runBlocking {
                defaultHttpClient.configurationMetadata(wellKnownUrl)
            }
    }
}
