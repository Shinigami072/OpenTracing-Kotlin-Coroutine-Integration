package shinigami

import OpenTracing
import activeSpan
import injectTracing
import io.jaegertracing.Configuration
import io.jaegertracing.internal.samplers.ConstSampler
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.opentracing.Tracer
import kotlinx.coroutines.async
import withTrace


suspend fun main(args: Array<String>): Unit {
//    val client = HttpClient()


    val samplerConfig: Configuration.SamplerConfiguration = Configuration.SamplerConfiguration
        .fromEnv()
        .withType(ConstSampler.TYPE)
        .withParam(1)


    val reporterConfig: Configuration.ReporterConfiguration = Configuration.ReporterConfiguration
        .fromEnv()
        .withLogSpans(true)


    val config: Configuration = Configuration(
        "TEST"
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
            val span = coroutineContext.activeSpan
            async {
                client.get<String>("http://google.com/").also {
//                println(it)
                }
            }
            async {
                val span2 = coroutineContext.activeSpan
                client.get<String>("http://localhost:8080/").also {
//                println(it)
                }
            }
            async {
                val span2 = coroutineContext.activeSpan
                client.get<String>("http://localhost:8080/").also {
//                println(it)
                }
            }
        }
    }
}
//@Suppress("unused") // Referenced in application.conf
//@kotlin.jvm.JvmOverloads
//fun Application.module(testing: Boolean = false) {
//    routing {
//        get {
//
//        }
//    }
//}

