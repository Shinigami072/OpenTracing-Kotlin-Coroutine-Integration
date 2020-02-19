import io.opentracing.Scope
import io.opentracing.Span
import io.opentracing.Tracer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.ThreadContextElement
import mu.KotlinLogging
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
class ActiveSpan(val tracer: Tracer, val span: Span? = tracer.activeSpan()) :
    ThreadContextElement<Scope>,
    AbstractCoroutineContextElement(ActiveSpan) {


    companion object Key : CoroutineContext.Key<ActiveSpan>

    /**
     * Cleans up after the coroutine suspends
     */
    override fun restoreThreadContext(context: CoroutineContext, oldState: Scope) {
        logger.trace { "${context[CoroutineName]?.name} exitScope $span" }
        oldState.close()
    }

    /**
     * Restores stored active span when coroutine is resumed
     */
    override fun updateThreadContext(context: CoroutineContext): Scope {
        logger.trace { "${context[CoroutineName]?.name} enterScope $span" }
        return tracer.activateSpan(span)
    }

    override fun toString(): String {
        return "ActiveSpan{activeSpan=$span,tracer=$tracer}"
    }
}

/**
 * Construct new coroutine context Element containing new active Span,
 * This is required whenever we want to activate a new span
 */
@ExperimentalCoroutinesTracingApi
suspend fun nextSpan(span: Span): CoroutineContext {

    return if (coroutineContext.activeSpan.span != span)
        coroutineContext + ActiveSpan(coroutineContext.tracer, span)
    else {
        logger.debug { "Reusing Same CoroutineActiveSpan for $span" }
        coroutineContext
    }
}

@ExperimentalCoroutinesTracingApi
val CoroutineContext.tracer: Tracer
    get() {
        return activeSpan.tracer
    }

@ExperimentalCoroutinesTracingApi
val CoroutineContext.activeSpan: ActiveSpan
    get() {
        return get(ActiveSpan) ?: error {
            logger.error { this.toString() }
            "CoroutineActiveSpan is required for proper propagation of Traces through coroutines"
        }
    }

