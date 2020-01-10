import io.opentracing.Scope
import io.opentracing.Span
import io.opentracing.Tracer
import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class CoroutineActiveSpan(val tracer: Tracer, val activeSpan: Span? = tracer.activeSpan()) :
    ThreadContextElement<Scope>,
    AbstractCoroutineContextElement(CoroutineActiveSpan) {

    private val logger: Logger = LoggerFactory.getLogger(CoroutineActiveSpan::class.java)

    companion object Key : CoroutineContext.Key<CoroutineActiveSpan>

    override fun restoreThreadContext(context: CoroutineContext, oldState: Scope) {
        if (logger.isTraceEnabled)
            logger.trace("${context[CoroutineName]?.name} exitScope $activeSpan")
        oldState.close()
    }

    override fun updateThreadContext(context: CoroutineContext): Scope {
        if (logger.isTraceEnabled)
            logger.trace("${context[CoroutineName]?.name} enterScope $activeSpan")

        return tracer.activateSpan(activeSpan)
    }

    fun nextSpan(span: Span): CoroutineActiveSpan = CoroutineActiveSpan(tracer, span)

    override fun toString(): String {
        return "ActiveSpan{activeSpan=$activeSpan,tracer=$tracer}"
    }
}

suspend fun <T> trace(
    operationName: String? = null,
    builder: Tracer.SpanBuilder.() -> Tracer.SpanBuilder = { this },
    cleanup: Span.() -> Unit = { finish() },
    action: suspend CoroutineScope.(Span?) -> T
): T {
    val activeSpan = coroutineContext[CoroutineActiveSpan]

    checkNotNull(activeSpan) {
        "OTActiveSpan is required for proper propagation of Traces through coroutines"
    }
    val span = activeSpan.tracer
        .buildSpan(operationName ?: coroutineContext[CoroutineName]?.name)
        .builder()
        .start()


    return withContext(activeSpan.nextSpan(span)) {
        coroutineContext[Job]?.invokeOnCompletion {
            span.cleanup()
        }
        action(span)
    }

}