package tokenxcanary

import io.ktor.util.KtorExperimentalAPI
import tokenxcanary.common.setCurrent
import tokenxcanary.redis.Cache
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

@KtorExperimentalAPI
class CacheTest {

    @Test
    fun `Redis get and set cached jwk`() {
        withMockOAuth2Server {
            val environment = testEnvironment(this.wellKnownUrl(issuerId = "maskinporten").toString())
            val cache = Cache(environment)
            val jwk = generateRsaKey().toJSONString()
            cache.get() shouldBe null
            cache.set(jwk)
            cache.get() shouldBeEqualTo jwk
        }
    }

    @Test
    fun `Set Current cache with every deploy, check that cache is rotated`() {
        withMockOAuth2Server {
            val environment = testEnvironment(this.wellKnownUrl(issuerId = "maskinporten").toString())
            val cache = Cache(environment)
            val jwk = generateRsaKey().toJSONString()
            cache.get() shouldBe null
            cache.setCurrent(jwk)
            cache.get() shouldBeEqualTo jwk
            Assertions.assertThrows(RuntimeException::class.java) { cache.setCurrent(jwk) }
            cache.setCurrent(generateRsaKey().toJSONString())
            cache.get() shouldNotBeEqualTo jwk
        }
    }
}
