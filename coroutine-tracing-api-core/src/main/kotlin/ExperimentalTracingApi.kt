package io.shinigami.coroutineTracingApi

/**
 * Marks declarations that are still **experimental** in coroutine tracing API, which means that the design of the
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near future or
 * the semantics of their behavior may change in some way that may break some code.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This part of the api is still experimantal and may change in the future"
)
annotation class ExperimentalCoroutinesTracingApi