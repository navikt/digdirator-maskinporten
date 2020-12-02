package maskinporten.redis

import maskinporten.config.Environment
import maskinporten.config.Profile
import redis.clients.jedis.Jedis

class Cache(
    env: Environment
) {
    private val redis = Jedis(env.redis.host, env.redis.port)

    init {
        when (env.application.profile) {
            Profile.TEST.value -> {
                redis.flushAll()
            }
            else -> {
                redis.auth(env.redis.password)
            }
        }
    }

    fun set(jwk: String) {
        redis.set(CURRENT_JWK, jwk)
    }

    fun get(): String? = redis.get(CURRENT_JWK)

    companion object {
        const val CURRENT_JWK = "jwk"
    }
}
