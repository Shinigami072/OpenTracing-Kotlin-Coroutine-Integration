package io.shinigami.coroutineTracingApi.ktor.server

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.ApplicationFeature
import io.ktor.features.toLogString
import io.ktor.request.ApplicationRequest
import io.ktor.response.ApplicationResponse
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.opentracing.Span
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.propagation.Format
import io.shinigami.coroutineTracingApi.*
import io.shinigami.coroutineTracingApi.ktor.utils.asTextMap

class OpenTracing(config: Configuration) {
    private val tracer = config.tracer

    class Configuration {
        var tracer: Tracer = NoopTracerFactory.create()
    }

    private fun ApplicationRequest.extractSpan(tracer: Tracer): Span {
        return tracer.span(toLogString()) {
            this.extractSpan(
                headers.asTextMap(),
                Format.Builtin.HTTP_HEADERS,
                tracer
            )
        }
    }

    private fun ApplicationResponse.injectSpan(tracer: Tracer, span: Span) {
        tracer.inject(
            span.context,
            Format.Builtin.HTTP_HEADERS,
            headers.asTextMap()
        )
    }

    private fun PipelineContext<Unit, ApplicationCall>.injectActiveSpanToHeaders() {
        (coroutineContext.activeSpan.span)?.let {
            context.response.injectSpan(tracer, it)
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
                feature.apply {
                    injectRootSpan()
                }
            }

            pipeline.intercept(ApplicationCallPipeline.Call) {
                feature.apply {
                    injectActiveSpanToHeaders()
                }
            }

            return feature
        }


    }
}



