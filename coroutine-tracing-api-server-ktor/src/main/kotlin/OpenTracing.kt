import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.features.toLogString
import io.ktor.util.AttributeKey
import io.ktor.util.toMap
import io.opentracing.SpanContext
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMapAdapter
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesTracingApi
class OpenTracing(val config: Configuration) {
    val tracer = config.tracer

    class Configuration {
        var tracer: Tracer = NoopTracerFactory.create()
    }


    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, OpenTracing> {
        override val key: AttributeKey<OpenTracing> = AttributeKey("OpenTracing")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): OpenTracing {
            // It is responsibility of the install code to call the `configure` method with the mutable configuration.
            val configuration = OpenTracing.Configuration().apply(configure)


            // Create the feature, providing the mutable configuration so the feature reads it keeping an immutable copy of the properties.
            val feature = OpenTracing(configuration)

            // Intercept a pipeline.
            pipeline.intercept(ApplicationCallPipeline.Monitoring/*Setup*/) {
                // Perform things in that interception point.
                val carrier = TextMapAdapter(this.context.request.headers.toMap().mapValues { it.value.first() })
                val spanContext: SpanContext? = feature.tracer.extract(Format.Builtin.HTTP_HEADERS, carrier)
                withContext(ActiveSpan(feature.tracer)) {
                    withTrace(context.request.toLogString(), builder = { asChildOf(spanContext) }) {
                        proceed()
                    }
                }
            }

            return feature
        }


    }
}
