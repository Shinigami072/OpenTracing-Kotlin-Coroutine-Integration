import io.opentracing.Span
import io.opentracing.Tracer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Add a new Span Representing current Job
 */
@ExperimentalCoroutinesTracingApi
suspend inline fun <T> withTrace(
    operationName: String,
    builder: Tracer.SpanBuilder.() -> Tracer.SpanBuilder = { this },
    crossinline cleanup: Span.(error: Throwable?) -> Unit = { this.finish() },
    crossinline block: suspend CoroutineScope.(Span) -> T
): T {

    val span: Span = coroutineContext.tracer
        .buildSpan(operationName)
        .builder()
        .start()

    return withContext(nextSpan(span)) {
        coroutineContext[Job]?.invokeOnCompletion {
            it?.also { span.log(it.message) }
            span.cleanup(it)
        }
        block(span)

    }
}

/**
 * Helper method for creating
 */
@ExperimentalCoroutinesTracingApi
suspend inline fun <T> injectTracing(tracer: Tracer, noinline block: suspend CoroutineScope.() -> T): T {

    return withContext(ActiveSpan(tracer), block = block)
}