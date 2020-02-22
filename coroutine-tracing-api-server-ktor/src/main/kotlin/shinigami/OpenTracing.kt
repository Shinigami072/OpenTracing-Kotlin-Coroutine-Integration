package shinigami

import activeSpan
import context
import extractSpan
import injectTracing
import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.features.toLogString
import io.ktor.http.Headers
import io.ktor.request.ApplicationRequest
import io.ktor.response.ResponseHeaders
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.opentracing.Span
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMap
import span

class OpenTracing(config: Configuration) {
    private val tracer = config.tracer
//    private val rootActiveSpan = ActiveSpan(tracer)

    class Configuration {
        var tracer: Tracer = NoopTracerFactory.create()
    }

    private fun ApplicationRequest.extractSpan(tracer: Tracer): Span {
        return tracer.span(toLogString()) {
            extractSpan(
                headers.asHeaders(),
                Format.Builtin.HTTP_HEADERS,
                tracer
            )
        }
    }

    private suspend fun PipelineContext<Unit, ApplicationCall>.injectRootSpan() {
        injectTracing(tracer, span = {
            context.request.extractSpan(tracer)
        }) {
            proceed()
        }
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, OpenTracing> {
        override val key: AttributeKey<OpenTracing> = AttributeKey("OpenTracing")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): OpenTracing {
            // It is responsibility of the install code to call the `configure` method with the mutable configuration.
            val configuration = Configuration().apply(configure)

            // Create the feature, providing the mutable configuration so the feature reads it keeping an immutable copy of the properties.
            val feature = OpenTracing(configuration)

            // Intercept a pipeline.
            pipeline.intercept(ApplicationCallPipeline.Setup) {
                // Perform things in that interception point.
                feature.apply {
                    injectRootSpan()
                }
            }
            pipeline.intercept(ApplicationCallPipeline.Call) {
                // Perform things in that interception point.
//                withTrace("${context.request.httpMethod} - ${context.request.uri} - Call") {
                (coroutineContext.activeSpan.span)?.let {
                    feature.tracer.inject(
                        it.context,
                        Format.Builtin.HTTP_HEADERS,
                        context.response.headers.asHeaders()
                    )
//                    }
                }
            }


            return feature
        }


    }
}

private fun Headers.asHeaders(): TextMap {
    return HeadersTextMap(this)
}

private fun ResponseHeaders.asHeaders(): TextMap {
    return ResponseHeadersTextMap(this)
}

class ResponseHeadersTextMap(private var headers: ResponseHeaders) : TextMap {
    override fun put(key: String, value: String) {
        headers.append(key, value)
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, String>> {
        val map = headers.allValues().entries()
            .associateBy { it.key }
            .filterValues { it.value.isNotEmpty() }
            .mapValues { (_, value) -> value.value.first() }
            .toMutableMap()
        return map.iterator()
    }
}

class HeadersTextMap(var headers: Headers) : TextMap {
    override fun put(key: String, value: String) {
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, String>> {

        val map = headers.entries()
            .associateBy { it.key }
            .filterValues { it.value.isNotEmpty() }
            .mapValues { (_, value) -> value.value.first() }
            .toMutableMap()
        return map.iterator()
    }
}
