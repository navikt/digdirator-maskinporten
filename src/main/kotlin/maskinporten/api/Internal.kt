package maskinporten.api

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respondText
import io.ktor.response.respondTextWriter
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

val collectorRegistry: CollectorRegistry = CollectorRegistry.defaultRegistry

inline fun Routing.selfTest(
    crossinline readySelfTestCheck: () -> Boolean,
    crossinline aLiveSelfTestCheck: () -> Boolean = { true }
) {
    get("/isAlive") {
        if (aLiveSelfTestCheck()) {
            call.respondText("I'm alive")
        } else {
            call.respondText("I'm dead!", status = HttpStatusCode.InternalServerError)
        }
    }
    get("/isReady") {
        if (readySelfTestCheck()) {
            call.respondText("I'm ready")
        } else {
            call.respondText("I'm dead!", status = HttpStatusCode.InternalServerError)
        }
    }
    get("/prometheus") {
        val names = call.request.queryParameters.getAll("name[]")?.toSet() ?: setOf()
        call.respondTextWriter(ContentType.parse(TextFormat.CONTENT_TYPE_004)) {
            TextFormat.write004(this, collectorRegistry.filteredMetricFamilySamples(names))
        }
    }
}
