import io.opentracing.Scope
import io.opentracing.ScopeManager
import io.opentracing.Span
import io.opentracing.mock.MockTracer
import io.opentracing.propagation.Format
import io.opentracing.util.ThreadLocalScopeManager
import io.shinigami.coroutineTracingApi.ActiveSpan
import io.shinigami.coroutineTracingApi.extractSpan
import io.shinigami.coroutineTracingApi.toTextMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import mu.KLogger
import mu.KotlinLogging
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@ExperimentalCoroutinesApi
class SpanExtractionTest {
    private var logger: KLogger = KotlinLogging.logger {}
    var tracer: MockTracer = MockTracer(LoggingScopeManager(logger = logger))

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

    @AfterEach
    fun tearDown() {
        tracer.reset()
    }

    @Test
    fun `Span is properly extracted from a text Map`() = runBlockingTest(ActiveSpan(tracer, null)) {
        val source = mapOf<String, String>(
            "traceid" to "9",
            "spanid" to "9",
            "baggage-bag" to "a",
            "additionalKey" to "isignored"
        ).toTextMap()

        val span = tracer
            .buildSpan("ChildSpan")
            .extractSpan(source, Format.Builtin.TEXT_MAP_EXTRACT, tracer)
            .start()

        span.finish()

        tracer.finishedSpans().first { it.operationName() == "ChildSpan" }.let {
            assertEquals(9L, it.parentId())
            assertEquals(9L, it.context().traceId())
            assertEquals("a", it.context().baggageItems().first { it.key == "bag" }.value)
        }
    }

    @Test
    fun `Span without a parent is created if the key is missing`() = runBlockingTest(ActiveSpan(tracer, null)) {
        val source = mapOf<String, String>(
            "traceid" to "9",
            "spani" to "9"
        ).toTextMap()

        val span = tracer
            .buildSpan("ChildSpan")
            .extractSpan(source, Format.Builtin.TEXT_MAP_EXTRACT, tracer)
            .start()

        span.finish()

        tracer.finishedSpans().first { it.operationName() == "ChildSpan" }.let {
            assertEquals(0L, it.parentId())
            assertNotEquals(9L, it.context().traceId())
        }
    }

    @Test
    fun `Malformed map makes throws an error`() {
        assertThrows<IllegalArgumentException> {
            runBlockingTest(ActiveSpan(tracer, null)) {
                val source = mapOf<String, String>(
                    "traceid" to "9",
                    "spanid" to "a9"
                ).toTextMap()

                val span = tracer
                    .buildSpan("ChildSpan")
                    .extractSpan(source, Format.Builtin.TEXT_MAP_EXTRACT, tracer)
                    .start()
            }
        }
    }


}