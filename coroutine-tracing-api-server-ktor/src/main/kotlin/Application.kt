//package shinigami
//
//import OpenTracing
//import io.jaegertracing.Configuration
//import io.jaegertracing.internal.samplers.ConstSampler
//import io.ktor.application.Application
//import io.ktor.application.call
//import io.ktor.application.install
//import io.ktor.client.HttpClient
//import io.ktor.client.engine.cio.CIO
//import io.ktor.response.respond
//import io.ktor.routing.get
//import io.ktor.routing.routing
//import io.opentracing.Tracer
//import kotlinx.coroutines.coroutineScope
//import withTrace
//
//fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)
//
//@Suppress("unused") // Referenced in application.conf
//@kotlin.jvm.JvmOverloads
//fun Application.module(testing: Boolean = false) {
//
//    val samplerConfig: Configuration.SamplerConfiguration = Configuration.SamplerConfiguration
//        .fromEnv()
//        .withType(ConstSampler.TYPE)
//        .withParam(1)
//
//
//    val reporterConfig: Configuration.ReporterConfiguration = Configuration.ReporterConfiguration
//        .fromEnv()
//        .withLogSpans(true)
//
//
//    val config: Configuration = Configuration(
//        "TEST_Server"
//    )
//        .withReporter(reporterConfig)
//        .withSampler(samplerConfig)
//
//
//    val tracer: Tracer = config.tracer
//
//    install(shinigami.OpenTracing) {
//        this.tracer = tracer
//    }
//
//    val client = HttpClient(CIO) {
//        this.install(OpenTracing) {
//            this.tracer = tracer
//        }
//    }
//
//    routing {
//        get {
//            withTrace(toString()) {
//                coroutineScope {
//                    throw IllegalStateException(it.toString())
//                }
//                call.respond("")
//                finish()
//            }
//        }
//    }
//}
//
