package io.shinigami.coroutineTracingApi.ktor.client

import io.ktor.client.HttpClient
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.http.HttpMethod
import io.ktor.util.AttributeKey
import io.opentracing.Span
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.propagation.Format
import io.shinigami.coroutineTracingApi.activeSpan
import io.shinigami.coroutineTracingApi.context
import io.shinigami.coroutineTracingApi.extractSpan
import io.shinigami.coroutineTracingApi.ktor.utils.asTextMap
import io.shinigami.coroutineTracingApi.withTrace

class OpenTracing(config: Configuration) {
    private val tracer = config.tracer

    class Configuration {
        var tracer: Tracer = NoopTracerFactory.create()
    }

    fun getTraceName(method: HttpMethod, url: String) = "$method - $url"


    companion object Feature : HttpClientFeature<Configuration, OpenTracing> {
        override val key: AttributeKey<OpenTracing> = AttributeKey("OpenTracing")
        private val spanKey = AttributeKey<Span>("OpenTracing-SendingSpan")

        override fun install(feature: OpenTracing, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                withTrace(feature.getTraceName(context.method, context.url.toString())) {
                    proceed()
                }
            }

            scope.requestPipeline.intercept(HttpRequestPipeline.State) {
                (coroutineContext.activeSpan.span)?.let {
                    feature.tracer.inject(it.context, Format.Builtin.HTTP_HEADERS, context.headers.asTextMap())
                    context.attributes.put(spanKey, it)
                }
                proceed()
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.Before) {
                withTrace(feature.getTraceName(context.request.method, context.request.url.toString()), builder = {
                    extractSpan(
                        context.response.headers.asTextMap(),
                        Format.Builtin.HTTP_HEADERS,
                        feature.tracer
                    )
                    //this is required to connect the post processing with spans from previous pipeline
                    //this is required because both pipeline have separate context
                    asChildOf(context.attributes.getOrNull(spanKey))
                }) {
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