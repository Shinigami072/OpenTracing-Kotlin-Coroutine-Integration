import io.opentracing.Scope
import io.opentracing.Span
import io.opentracing.Tracer
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext


/**
 * This is A CoroutineContextElement that manages active spans in coroutines, it needs to be aware of Tracer -
 * because it utilises it
 *
 * Dy default it uses Tracer to get the active span
 */
class CoroutineActiveSpan(val tracer: Tracer, private val activeSpan: Span? = tracer.activeSpan()) :
    ThreadContextElement<Scope>,
    AbstractCoroutineContextElement(CoroutineActiveSpan) {

    private val logger: Logger = LoggerFactory.getLogger(CoroutineActiveSpan::class.java)

    companion object Key : CoroutineContext.Key<CoroutineActiveSpan>

    /**
     * Cleans up after the coroutine suspends
     */
    override fun restoreThreadContext(context: CoroutineContext, oldState: Scope) {
        if (logger.isTraceEnabled)
            logger.trace("${context[CoroutineName]?.name} exitScope $activeSpan")
        oldState.close()
    }

    /**
     * Restores stored active span when coroutine is resumed
     */
    override fun updateThreadContext(context: CoroutineContext): Scope {
        if (logger.isTraceEnabled)
            logger.trace("${context[CoroutineName]?.name} enterScope $activeSpan")

        return tracer.activateSpan(activeSpan)
    }

    /**
     * Construct new coroutine context Element containing new active Span,
     * This is required whenever we want to activate a new span
     */
    fun nextSpan(span: Span): CoroutineActiveSpan = CoroutineActiveSpan(tracer, span)

    override fun toString(): String {
        return "ActiveSpan{activeSpan=$activeSpan,tracer=$tracer}"
    }
}

/**
 * Add a new Span Representing current Job
 */
suspend fun <T> trace(
    operationName: String,
    builder: Tracer.SpanBuilder.() -> Tracer.SpanBuilder = { this },
    cleanup: Span.() -> Unit = { finish() },
    action: suspend CoroutineScope.(Span) -> T
): T {
    val activeSpan = coroutineContext[CoroutineActiveSpan]
    checkNotNull(activeSpan) {
        "CoroutineActiveSpan is required for proper propagation of Traces through coroutines"
    }
    val span: Span = activeSpan.tracer
        .buildSpan(operationName)
        .builder()
        .start()


    return withContext(activeSpan.nextSpan(span)) {
        coroutineContext[Job]?.invokeOnCompletion {
            span.cleanup()
        }
        action(span)
    }

}