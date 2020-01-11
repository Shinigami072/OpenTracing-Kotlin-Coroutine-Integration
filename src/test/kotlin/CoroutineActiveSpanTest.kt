import io.opentracing.Scope
import io.opentracing.ScopeManager
import io.opentracing.Span
import io.opentracing.mock.MockSpan
import io.opentracing.mock.MockTracer
import io.opentracing.util.ThreadLocalScopeManager
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.Executors

@ExperimentalCoroutinesApi
class CoroutineActiveSpanTest {

    lateinit var tracer: MockTracer
    private var logger: Logger = LoggerFactory.getLogger(this::class.java)

    class LoggingScopeManager(
        private val scopeManager: ScopeManager = ThreadLocalScopeManager(),
        val logger: Logger = LoggerFactory.getLogger(LoggingScopeManager::class.java)
    ) : ScopeManager by scopeManager {
        override fun activate(span: Span?): Scope {
            logger.debug("Activting Span:$span")
            return scopeManager.activate(span)
        }

        override fun activeSpan(): Span? {
            val span = scopeManager.activeSpan()
            logger.debug("Active Span:$span")
            return span
        }
    }

    @BeforeEach
    fun setUp() {
        tracer = MockTracer(LoggingScopeManager(logger = logger))
    }

    @AfterEach
    fun teatDown() {
        tracer.reset()
        tracer.close()
    }

    @Test
    fun `Should Create Span`() = runBlockingTest {
        advanceTimeBy(1)
        withContext(CoroutineActiveSpan(tracer)) {
            assert(tracer.activeSpan() == null)
            trace("TestOperation", builder = {
                withStartTimestamp(currentTime * 1000)
            },
                cleanup = {
                    finish(currentTime * 1000)
                }) {
                delay(500)
            }
        }

        cleanupTestCoroutines()

        val traces: List<MockSpan> = tracer.finishedSpans()
        assert(tracer.activeSpan() == null)
        assert(1 == traces.size)
        assert(traces.first().operationName() == "TestOperation")
        assert(traces.first().parentId() == 0L)
        assert(traces.first().startMicros() == 1000L) { traces.first().startMicros() }
        assert(traces.first().finishMicros() == 501000L) { traces.first().finishMicros() }
    }

    @Test
    fun `Should Create  nested Spans`() = runBlockingTest {
        withContext(CoroutineActiveSpan(tracer)) {
            assert(tracer.activeSpan() == null)
            trace("TestOperation1") {
                assert(tracer.activeSpan() != null)
                delay(500)
                trace("TestOperation2") {
                    assert(tracer.activeSpan() != null)
                    delay(500)
                }
                assert(tracer.activeSpan() != null)
            }
            assert(tracer.activeSpan() == null)
        }
        advanceUntilIdle()
        cleanupTestCoroutines()

        val traces: List<MockSpan> = tracer.finishedSpans()
        assert(tracer.activeSpan() == null)
        assert(2 == traces.size)
        assert(traces.last().operationName() == "TestOperation1") { traces.toString() }
        assert(traces.last().parentId() == 0L) { traces.toString() }
        assert(traces.first().operationName() == "TestOperation2") { traces.toString() }
        assert(traces.first().parentId() == traces.last().context().spanId()) { traces.toString() }
        assert(traces.first().context().traceId() == traces.last().context().traceId()) { traces.toString() }
    }


    @Test
    fun `Should Create  nested independent spans Spans`() = runBlockingTest {
        withContext(CoroutineActiveSpan(tracer)) {
            trace("TestOperation1") {
                delay(500)
                assert(tracer.activeSpan() != null)
                launch {
                    assert(tracer.activeSpan() != null)
                    trace("TestOperation2-1") {
                        delay(500)
                    }
                }
                launch {
                    assert(tracer.activeSpan() != null)
                    trace("TestOperation2-2") {
                        assert(tracer.activeSpan() != null)
                        delay(500)
                    }
                }
                assert(tracer.activeSpan() != null)
            }
            assert(tracer.activeSpan() == null)
        }

        advanceUntilIdle()
        cleanupTestCoroutines()

        val traces: List<MockSpan> = tracer.finishedSpans()
        assert(tracer.activeSpan() == null)
        assert(3 == traces.size)
        val span1 = traces.find { it.operationName() == "TestOperation1" }
        val span21 = traces.find { it.operationName() == "TestOperation2-1" }
        val span22 = traces.find { it.operationName() == "TestOperation2-2" }
        assertNotNull(span1) { traces.toString() }
        assertNotNull(span21) { traces.toString() }
        assertNotNull(span22) { traces.toString() }
        assert(span1?.parentId() == 0L) { traces.toString() }
        assert(span21?.parentId() == span1?.context()?.spanId()) { traces.toString() }
        assert(span21?.context()?.traceId() == span1?.context()?.traceId()) { traces.toString() }
        assert(span22?.parentId() == span1?.context()?.spanId()) { traces.toString() }
        assert(span22?.context()?.traceId() == span1?.context()?.traceId()) { traces.toString() }
    }


    @Test
    fun `Should Create  nested independent spans Spans evenifexception is thrown`() = runBlockingTest {
        try {
            async(CoroutineActiveSpan(tracer)) {
                trace("TestOperation1") {
                    delay(500)
                    assert(tracer.activeSpan() != null)
                    async {
                        assert(tracer.activeSpan() != null)
                        trace("TestOperation2-1") {
                            delay(500)
                        }
                    }.await()

                    async {
                        assert(tracer.activeSpan() != null)
                        trace("TestOperation2-2") {
                            assert(tracer.activeSpan() != null)
                            throw IOException()
                            trace("TestOperation3-2") {
                                delay(100)
                            }
                        }
                    }.await()
                }
            }.await()
        } catch (e: IOException) {
        }


        advanceUntilIdle()
        cleanupTestCoroutines()

        val traces: List<MockSpan> = tracer.finishedSpans()
        assert(tracer.activeSpan() == null)
        assert(3 == traces.size)
        val span1 = traces.find { it.operationName() == "TestOperation1" }
        val span21 = traces.find { it.operationName() == "TestOperation2-1" }
        val span22 = traces.find { it.operationName() == "TestOperation2-2" }
        assertNotNull(span1) { traces.toString() }
        assertNotNull(span21) { traces.toString() }
        assertNotNull(span22) { traces.toString() }
        assert(span1?.parentId() == 0L) { traces.toString() }
        assert(span21?.parentId() == span1?.context()?.spanId()) { traces.toString() }
        assert(span21?.context()?.traceId() == span1?.context()?.traceId()) { traces.toString() }
        assert(span22?.parentId() == span1?.context()?.spanId()) { traces.toString() }
        assert(span22?.context()?.traceId() == span1?.context()?.traceId()) { traces.toString() }
    }

    @Test
    fun `Switch Threads`() {
        val differentThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

        runBlocking {
            launch(CoroutineActiveSpan(tracer)) {
                trace("Outer Operation") {
                    withContext(differentThread) {
                        trace("Inner Operation 1") {
                            delay(100)
                        }
                    }
                    withContext(differentThread) {
                        trace("Inner Operation 2") {
                            delay(100)
                        }
                    }
                    withContext(differentThread) {
                        trace("Inner Operation 3") {
                            delay(100)
                        }
                    }
                }
                withContext(differentThread) {
                    assertEquals(null, tracer.activeSpan())
                }
            }
            assertEquals(null, tracer.activeSpan())
        }
        differentThread.close()
        val spans = tracer.finishedSpans()
        val rootSpan = spans.first { it.operationName() == "Outer Operation" }
        val inner1 = spans.first { it.operationName() == "Inner Operation 1" }
        val inner2 = spans.first { it.operationName() == "Inner Operation 2" }
        val inner3 = spans.first { it.operationName() == "Inner Operation 3" }
        logger.debug(tracer.finishedSpans().toString())
        assertEquals(0, rootSpan.parentId())
        assertEquals(rootSpan.context().spanId(), inner1.parentId())
        assertEquals(rootSpan.context().spanId(), inner2.parentId())
        assertEquals(rootSpan.context().spanId(), inner3.parentId())
        assertEquals(rootSpan.context().traceId(), inner1.context().traceId())
        assertEquals(rootSpan.context().traceId(), inner2.context().traceId())
        assertEquals(rootSpan.context().traceId(), inner3.context().traceId())
    }

    @Test
    fun `Coroutine Unaware API works correctly`() {

        fun nonCoroutineAwareTracer(span: Span, time_ms: Long) {
            tracer.activateSpan(span).use {
                tracer.buildSpan("Non Coroutine Span")
                    .withStartTimestamp(time_ms * 1000)
                    .start()
                    .finish(time_ms * 1000 + 100)
            }
        }

        val activeSpan: Span = tracer.buildSpan("Root Span").start()
        runBlockingTest {
            launch(CoroutineActiveSpan(tracer)) {
                trace("Outer Operation") {
                    nonCoroutineAwareTracer(activeSpan, currentTime)
                    delay(100)
                    nonCoroutineAwareTracer(activeSpan, currentTime)
                    delay(100)
                    nonCoroutineAwareTracer(activeSpan, currentTime)
                }
            }
        }
        activeSpan.finish()

        val finishedSpans = tracer.finishedSpans()
        val rootSpan = finishedSpans.first { it.operationName() == "Root Span" }
        val nc = finishedSpans.filter { it.operationName() == "Non Coroutine Span" }

        assertEquals(null, tracer.activeSpan())
        nc.forEach {
            assertEquals(rootSpan.context().spanId(), it.parentId())
            assertEquals(rootSpan.context().traceId(), it.context().traceId())
        }
    }
}