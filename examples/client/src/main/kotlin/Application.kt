package io.shinigami.example

import io.jaegertracing.Configuration
import io.jaegertracing.internal.samplers.ConstSampler
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.util.KtorExperimentalAPI
import io.opentracing.Tracer
import io.shinigami.coroutineTracingApi.injectTracing
import io.shinigami.coroutineTracingApi.ktor.client.OpenTracing
import io.shinigami.coroutineTracingApi.withTrace


@OptIn(KtorExperimentalAPI::class)
suspend fun main(args: Array<String>) {

    val samplerConfig: Configuration.SamplerConfiguration = Configuration.SamplerConfiguration
        .fromEnv()
        .withType(ConstSampler.TYPE)
        .withParam(1)


    val reporterConfig: Configuration.ReporterConfiguration = Configuration.ReporterConfiguration
        .fromEnv()
        .withLogSpans(true)


    val config: Configuration = Configuration(
        "Example_Client"
    )
        .withReporter(reporterConfig)
        .withSampler(samplerConfig)


    val tracer: Tracer = config.tracer

    val client = HttpClient(CIO) {
        install(OpenTracing) {
            this.tracer = tracer
        }
    }

    injectTracing(tracer) {
        withTrace("Outside Operation") {
            client.post<String>("http://localhost:8080/") {
                body = "Echo"
            }.also {
                println(it)
            }
        }
    }
}
