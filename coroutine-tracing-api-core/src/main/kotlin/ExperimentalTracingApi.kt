import kotlin.Experimental.Level

/**
 * Marks declarations that are still **experimental** in coroutine tracing API, which means that the design of the
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near future or
 * the semantics of their behavior may change in some way that may break some code.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@Experimental(level = Level.WARNING)
annotation class ExperimentalCoroutinesTracingApi