import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.response.HttpReceivePipeline
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.util.AttributeKey
import io.opentracing.Span
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMap

@ExperimentalCoroutinesTracingApi
class OpenTracing(config: Configuration) {
    private val tracer = config.tracer
//    private val rootActiveSpan = ActiveSpan(tracer)

    class Configuration {
        var tracer: Tracer = NoopTracerFactory.create()
    }

//    private fun ApplicationRequest.extractSpan(tracer: Tracer): Span {
//        return tracer.span(toLogString()) {
//            extractSpan(
//                headers.asHeaders(),
//                Format.Builtin.HTTP_HEADERS,
//                tracer
//            )
//        }
//    }
//
//    private suspend fun PipelineContext<Unit, ApplicationCall>.injectRootSpan() {
//        injectTracing(tracer, span = {
//            context.request.extractSpan(tracer)
//        }) {
//            proceed()
//        }
//    }

    companion object Feature : HttpClientFeature<Configuration, OpenTracing> {
        override val key: AttributeKey<OpenTracing> = AttributeKey("OpenTracing")
        private val spanKey = AttributeKey<Span>("OpenTracing-SendingSpan")

        override fun install(feature: OpenTracing, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                coroutineContext.tracer
                withTrace("${context.method} - ${context.url}") {
                    proceed()
                }
            }

            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                (coroutineContext.activeSpan.span)?.let {
                    feature.tracer.inject(it.context, Format.Builtin.HTTP_HEADERS, context.headers.toTextMap())
                    context.attributes.put(spanKey, it)
                }
                proceed()

            }


            scope.receivePipeline.intercept(HttpReceivePipeline.Before) {
                withTrace("${context.request.method} - ${context.request.url} - Before", builder = {
                    extractSpan(
                        context.response.headers.asHeaders(),
                        Format.Builtin.HTTP_HEADERS,
                        feature.tracer
                    )
                    asChildOf(context.attributes.getOrNull(spanKey))
                }) {

                    proceed()

                }
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.State) {
                withTrace("${context.request.method} - ${context.request.url} - State") {
                    proceed()
                }
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.After) {
                withTrace("${context.request.method} - ${context.request.url} - After") {
                    proceed()
                }
            }


        }

        override fun prepare(block: Configuration.() -> Unit): OpenTracing {
            val config = Configuration().apply(block)
            return OpenTracing(config)
        }


    }
}

private fun Headers.asHeaders(): TextMap {
    return HeadersTextMap(this)
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

//
//private fun Headers.asHeaders(): TextMap {
//    return HeadersTextMap(this)
//}
//
fun HeadersBuilder.toTextMap(): TextMap = HeadersTextMapAdapter(this)

class HeadersTextMapAdapter(var headers: HeadersBuilder) : TextMap {
    override fun put(key: String, value: String) {
        headers.append(key, value)
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, String>> {
        return headers.entries()
            .filter { (_, values) -> values.isNotEmpty() }
            .map { (key, values) -> key to values.first() }
            .toMap().toMutableMap().iterator()
    }

}
//
//    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, String>> {
//
//        val map = headers.entries()
//            .associateBy { it.key }
//            .filterValues { it.value.isNotEmpty() }
//            .mapValues { (_, value) -> value.value.first() }
//            .toMutableMap()
//        return map.iterator()
//    }
//}
