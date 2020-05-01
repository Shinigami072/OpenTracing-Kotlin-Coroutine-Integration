import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import io.ktor.client.tests.utils.config
import io.ktor.client.tests.utils.test
import io.ktor.client.tests.utils.testWithEngine
import io.ktor.util.InternalAPI
import io.ktor.util.toMap
import io.opentracing.Span
import io.opentracing.SpanContext
import io.opentracing.mock.MockTracer
import io.opentracing.propagation.Format
import io.shinigami.coroutineTracingApi.context
import io.shinigami.coroutineTracingApi.injectTracing
import io.shinigami.coroutineTracingApi.ktor.client.OpenTracing
import io.shinigami.coroutineTracingApi.toTextMap
import io.shinigami.coroutineTracingApi.withTrace
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@InternalAPI
class TestOpenTracing {

    private val mockTracer: MockTracer = MockTracer()


    @AfterEach
    fun setUp() {
        mockTracer.reset()
    }

    private fun Map<String, List<String>>.toFlatMap() = this.mapValues { (_, v) -> v.first() }

    private fun MockEngineConfig.addSpanHandler(span: Span? = null) {
        addHandler { request ->
            val c: SpanContext? = mockTracer
                .extract(
                    Format.Builtin.HTTP_HEADERS,
                    request.headers
                        .toMap().toFlatMap()
                        .toMutableMap().toTextMap()
                )


            val lastSpan: Span? = span ?: mockTracer.activeSpan()
            if (lastSpan == null) {
                assertEquals(null, c)
                return@addHandler respondOk()
            } else {
                assertNotNull(c)

                c.baggageItems().forEach { (key, value) ->
                    assertEquals(lastSpan.getBaggageItem(key), value, "baggage[$key] not the same")
                }

                assertEquals(
                    lastSpan.context.toSpanId(),
                    c.toSpanId(),
                    "SpanId not the same"
                )

                assertEquals(
                    lastSpan.context.toTraceId(),
                    c.toTraceId(),
                    "TraceId not the same"
                )

                return@addHandler respondOk()
            }
        }
    }

    @Test
    fun `With tracing injected call succeeds`(): Unit = testWithEngine(MockEngine) {
        config {
            install(OpenTracing) {
                tracer = mockTracer
            }

            engine {
                addSpanHandler()
            }
        }
        test { client ->
            injectTracing(mockTracer) {
                client.get<String>("/")
            }
        }
    }

    @Test
    fun `With parent span - it is transferred`(): Unit = testWithEngine(MockEngine) {
        config {
            install(OpenTracing) {
                tracer = mockTracer
            }

            engine {
                addSpanHandler()
            }
        }
        test { client ->
            injectTracing(mockTracer) {
                withTrace("Child") {
                    it.setBaggageItem("Test1", "Test1")
                    it.setBaggageItem("Test2", "$#^&<!>#T%#R><!@!@)!@(#")
                    client.get<String>("/")
                }
            }
        }
    }

    @Test
    fun `Without injected tracing call fails`(): Unit = testWithEngine(MockEngine) {

        config {
            install(OpenTracing) {
                tracer = mockTracer
            }

            engine {
                addHandler { respondOk() }
            }
        }
        test { client ->

            assertThrows<IllegalStateException> {
                runBlocking { client.get<String>("/") }
            }

        }
    }

}