package io.shinigami.example

import io.jaegertracing.Configuration
import io.jaegertracing.internal.samplers.ConstSampler
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.opentracing.Tracer
import io.shinigami.coroutineTracingApi.ktor.server.OpenTracing
import io.shinigami.coroutineTracingApi.withTrace

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    val samplerConfig: Configuration.SamplerConfiguration = Configuration.SamplerConfiguration
        .fromEnv()
        .withType(ConstSampler.TYPE)
        .withParam(1)


    val reporterConfig: Configuration.ReporterConfiguration = Configuration.ReporterConfiguration
        .fromEnv()
        .withLogSpans(true)


    val config: Configuration = Configuration(
        "Example_Server"
    )
        .withReporter(reporterConfig)
        .withSampler(samplerConfig)


    val tracer: Tracer = config.tracer

    install(OpenTracing) {
        this.tracer = tracer
    }

    routing {
        post {
            withTrace(toString()) {
                call.respond(call.receiveText())
                finish()
            }
        }
    }
}

