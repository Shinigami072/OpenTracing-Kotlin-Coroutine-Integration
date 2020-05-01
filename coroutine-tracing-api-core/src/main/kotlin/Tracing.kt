package io.shinigami.coroutineTracingApi

import io.opentracing.Span
import io.opentracing.Tracer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Add a new Span Representing current Job
 */
suspend inline fun <T> withTrace(
    operationName: String,
    noinline builder: Tracer.SpanBuilder.() -> Tracer.SpanBuilder = { this },
    noinline cleanup: Span.(error: Throwable?) -> Unit = { this.finish() },
    crossinline block: suspend CoroutineScope.(Span) -> T
): T {
    val span: Span = span(operationName, builder)
    return activateSpan(span) {
        span.addCleanup(cleanup)
        coroutineScope {
            block(span)
        }
    }
}

/**
 * Helper method for creating
 */
suspend fun <T> injectTracing(
    tracer: Tracer,
    span: Tracer.() -> Span? = { null },
    block: suspend CoroutineScope.() -> T
): T {
    val buildSpan = tracer.span()
    return withContext(ActiveSpan(tracer, buildSpan)) {
        buildSpan?.addCleanup { finish() }
        block()
    }
}

