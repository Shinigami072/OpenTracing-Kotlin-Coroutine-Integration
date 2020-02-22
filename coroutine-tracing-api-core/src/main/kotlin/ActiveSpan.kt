import io.opentracing.Scope
import io.opentracing.Span
import io.opentracing.SpanContext
import io.opentracing.Tracer
import io.opentracing.propagation.Format
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext


private val logger = KotlinLogging.logger {}

/**
 * This is A CoroutineContextElement that manages active spans in coroutines, it needs to be aware of Tracer -
 * because it utilises it
 *
 * Dy default it uses Tracer to get the active span
 */
@ExperimentalCoroutinesTracingApi
class ActiveSpan(
    val tracer: Tracer,
    val span: Span? = tracer.activeSpan()
) :
    ThreadContextElement<Scope>,
    AbstractCoroutineContextElement(ActiveSpan) {

    companion object Key : CoroutineContext.Key<ActiveSpan>

    /**
     * Cleans up after the coroutine suspends
     */
    override fun restoreThreadContext(context: CoroutineContext, oldState: Scope) {
        logger.trace { "${context[CoroutineName]?.name ?: "coroutine"} exitScope $span" }
        logger.debug { "restore$context" }
        runCatching {
            if (context.isActive) {
                span?.log(mapOf("event" to "suspend", "coroutine" to (context[CoroutineName]?.name ?: "coroutine")))
            }
        }
        oldState.close()
    }

    /**
     * Restores stored active span when coroutine is resumed
     */
    override fun updateThreadContext(context: CoroutineContext): Scope {
        logger.trace { "${context[CoroutineName]?.name} enterScope $span" }
        if (context.isActive) {
            span?.log(mapOf("event" to "resumed", "coroutine" to (context[CoroutineName]?.name ?: "coroutine")))
        }

        return tracer.activateSpan(span)
    }

    override fun toString(): String {
        return "ActiveSpan{getActiveSpan=$span,getTracer=$tracer}"
    }
}

/**
 * Switch context to one containing new active Span,
 * This is required whenever we want to activate a new span but
 * is intended as a low level abstraction
 *
 * If you are looking for a way to trace your application @see [withTrace]
 */
@ExperimentalCoroutinesTracingApi
suspend fun <T> activateSpan(span: Span, block: suspend CoroutineScope.(Span) -> T): T {

    val context = if (coroutineContext[ActiveSpan]?.span != span)
        coroutineContext + ActiveSpan(coroutineContext.tracer, span)
    else {
        logger.debug { "Reusing Same CoroutineActiveSpan for $span" }
        coroutineContext
    }

    return withContext(context) {
        coroutineScope {
            block(span)
        }
    }
}

/**
 * A helper util for getting the tracer, if the coroutine context does not contain an @see [ActiveSpan] an error will be thrown @see [CoroutineContext.activeSpan]
 */
@ExperimentalCoroutinesTracingApi
val CoroutineContext.tracer: Tracer
    get() {
        return activeSpan.tracer
    }

/**
 * A helper util for getting the ActiveSpan, if the coroutine context does not contain an @see [ActiveSpan]
 * an error will be thrown
 */
@ExperimentalCoroutinesTracingApi
val CoroutineContext.activeSpan: ActiveSpan
    get() {
        return get(ActiveSpan) ?: error {
            logger.error { this.toString() }
            "CoroutineActiveSpan is required for proper propagation of Traces through coroutines"
        }
    }

suspend fun Span.addCleanup(
    cleanup: Span.(error: Throwable?) -> Unit = { this.finish() }
) {
    coroutineContext[Job]?.invokeOnCompletion {
        logger.debug { "Cleanup ${context.toSpanId()}" }
        it?.also {
            val errors = StringWriter()
            it.printStackTrace(PrintWriter(errors))
            setTag("error", true)
            log(mapOf("stack" to errors))
        }
        cleanup(it)
    }
}

/**
 * This is a helper function for creating spans using the provided tracer.
 * Its is intended to be used only by library creators,
 *
 * If you are looking for a way to trace your application @see [withTrace]
 */
@ExperimentalCoroutinesTracingApi
inline fun Tracer.span(
    operationName: String,
    builder: Tracer.SpanBuilder.() -> Tracer.SpanBuilder
): Span {
    return buildSpan(operationName)
        .builder()
        .start()
}

@ExperimentalCoroutinesTracingApi
suspend inline fun span(operationName: String, builder: Tracer.SpanBuilder.() -> Tracer.SpanBuilder): Span =
    coroutineContext.tracer
        .span(
            operationName, builder
        )

/**
 * This is intended as a low level api for span extraction
 * if used in a context not containing @see [ActiveSpan] it fill throw an error
 */
@ExperimentalCoroutinesTracingApi
fun <C> Tracer.SpanBuilder.extractSpan(
    carrier: C,
    format: Format<C>,
    tracer: Tracer
): Tracer.SpanBuilder {

    val context: SpanContext? = tracer.extract(format, carrier)

    return context?.let {
        asChildOf(it)
    } ?: this

}

///**
// * This is intended as a low level api for span injection
// * if used in a context not containing @see [ActiveSpan] it fill throw an error
// */
//@ExperimentalCoroutinesTracingApi
//suspend inline fun <C> Span.injectSpan(
//    format: Format<C>,
//    carrier: () -> C
//): C {
//    val carry = carrier()
//    coroutineContext.tracer.inject(context, format, carry)
//    return carry
//}




