import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.toLogString
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.contentType
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.toMap
import io.opentracing.mock.MockTracer
import io.opentracing.propagation.Format
import io.opentracing.propagation.TextMapAdapter
import io.shinigami.coroutineTracingApi.context
import io.shinigami.coroutineTracingApi.ktor.server.OpenTracing
import io.shinigami.coroutineTracingApi.toTextMap
import io.shinigami.coroutineTracingApi.withTrace
import junit.framework.TestCase.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class TestOpenTracing {

    private val mockTracer: MockTracer = MockTracer()

    @AfterEach
    fun setUp() {
        mockTracer.reset()
    }

    @Test
    fun `Without headers span containing operation name is defined`(): Unit = withTestApplication {
        //Given
        val path = "/get_path/"
        application.install(OpenTracing) {
            tracer = mockTracer
        }
        application.routing {
            get(path) {
                call.respond("OK")
            }
        }
        //When
        handleRequest(HttpMethod.Get, path) {

        }.let { call ->
            //Then
            assertEquals(HttpStatusCode.OK, call.response.status())
            assertEquals(ContentType.Text.Plain, call.response.contentType().withoutParameters())
            assertEquals("OK", call.response.content)

            mockTracer.finishedSpans()
                .first { it.parentId() == 0L }
                .let {
                    assertEquals(call.request.toLogString(), it.operationName())
                }
        }
    }

    @Test
    fun `Without headers span containing operation name is defined and a child span is correctly parented`(): Unit =
        withTestApplication {
            //Given
            val path = "/get_path/"
            application.install(OpenTracing) {
                tracer = mockTracer
            }
            application.routing {
                get(path) {
                    withTrace("Child-1") {
                        withTrace("Child-1-1") {

                        }
                    }
                    withTrace("Child-2") {
                        call.respond("OK")
                    }
                }
            }
            //When
            handleRequest(HttpMethod.Get, path) {

            }.let { call ->
                //Then
                assertEquals(HttpStatusCode.OK, call.response.status())
                assertEquals(ContentType.Text.Plain, call.response.contentType().withoutParameters())
                assertEquals("OK", call.response.content)

                mockTracer.finishedSpans()
                    .first { it.parentId() == 0L }
                    .let { root ->
                        mockTracer.finishedSpans()
                            .first { it.operationName() == "Child-1" }
                            .let { child1 ->
                                mockTracer.finishedSpans()
                                    .first { it.operationName() == "Child-1-1" }
                                    .let {
                                        assertEquals(child1.context().spanId(), it.parentId())
                                        assertEquals(root.context().traceId(), it.context().traceId())
                                    }
                                assertEquals(root.context().spanId(), child1.parentId())
                                assertEquals(root.context().traceId(), child1.context().traceId())
                            }
                        mockTracer.finishedSpans()
                            .first { it.operationName() == "Child-2" }
                            .let { child2 ->
                                assertEquals(root.context().spanId(), child2.parentId())
                                assertEquals(root.context().traceId(), child2.context().traceId())
                            }
                        assertEquals(call.request.toLogString(), root.operationName())
                    }
            }
        }

    @Test
    fun `With headers span containing operation name is defined and properly parented`(): Unit = withTestApplication {
        //Given
        val path = "/get_path/"
        application.install(OpenTracing) {
            tracer = mockTracer
        }
        application.routing {
            get(path) {
                call.respond("OK")
            }
        }
        //When
        val span = mockTracer.buildSpan("Root").start()
        span.log("A")
        span.log("B")
        span.log("C")
        span.setBaggageItem("bagA", "itemA")
        span.setBaggageItem("bagB", "itemB")
        span.setBaggageItem("bagC", "itemC")
        handleRequest(HttpMethod.Get, path) {
            val map = mutableMapOf<String, String>()
            val httpCarrier = TextMapAdapter(map)
            mockTracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, httpCarrier)
            map.forEach { (header, value) ->
                addHeader(header, value)
            }
        }.let { call ->
            val headers = call.response
                .headers
                .allValues()
                .toMap()
                .mapValues { (_, value) -> value.first() }
                .toMutableMap()
                .toTextMap()
            val sc = mockTracer.extract(Format.Builtin.HTTP_HEADERS, headers)
            //Then
            assertEquals(HttpStatusCode.OK, call.response.status())
            assertEquals(ContentType.Text.Plain, call.response.contentType().withoutParameters())
            assertEquals("OK", call.response.content)
            mockTracer.finishedSpans()
                .first { it.operationName() == call.request.toLogString() }
                .let { root ->
                    assertEquals(span.context().spanId(), root.parentId())
                }

            mockTracer.finishedSpans()
                .last()
                .let { exit ->
                    assertEquals(sc.toSpanId(), exit.context.toSpanId())
                }
        }
    }

}