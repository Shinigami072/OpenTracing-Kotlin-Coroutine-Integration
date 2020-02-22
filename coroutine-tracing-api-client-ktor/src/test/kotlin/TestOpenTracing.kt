import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import io.ktor.client.tests.utils.clientTest
import io.ktor.client.tests.utils.config
import io.ktor.client.tests.utils.test
import io.ktor.util.InternalAPI
import io.opentracing.mock.MockTracer
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@ExperimentalCoroutinesTracingApi
@InternalAPI
class TestOpenTracing {

    private val mockTracer: MockTracer = MockTracer()


    @AfterEach
    fun setUp() {
        mockTracer.reset()
    }

    @Test
    fun `Without headers span containing operation name is defined`(): Unit = clientTest(MockEngine) {
        config {
            install(OpenTracing) {
                tracer = mockTracer
            }

            engine {
                addHandler { request ->
                    assertEquals(
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/70.0.3538.77 Chrome/70.0.3538.77 Safari/537.36",
                        request.headers["user-agent"]
                    )
                    respondOk()
                }
            }
        }
        test { client ->
            injectTracing(mockTracer) {
                client.get<String>("/") {}.let {

                }
            }
        }
    }

    @Test
    fun `Without seheaders span containing operation name is defined`(): Unit = clientTest(MockEngine) {
        config {
            install(OpenTracing) {
                tracer = mockTracer
            }

            engine {
                addHandler { request ->
                    assertEquals(
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/70.0.3538.77 Chrome/70.0.3538.77 Safari/537.36",
                        request.headers["user-agent"]
                    )
                    respondOk()
                }
            }
        }
        test { client ->
            injectTracing(mockTracer) {
                withTrace("Child") {
                    it.log("Log")
                    it.setBaggageItem("Test1", "Test1")
                    client.get<String>("/") {}.let {

                    }
                }
            }
        }
    }

    @Test
    fun `Witehout headers span containing operation name is defined`(): Unit = clientTest(MockEngine) {
        config {
            install(OpenTracing) {
                tracer = mockTracer
            }

            engine {
                addHandler { request ->
                    assertEquals(
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Ubuntu Chromium/70.0.3538.77 Chrome/70.0.3538.77 Safari/537.36",
                        request.headers["user-agent"]
                    )
                    respondOk()
                }
            }
        }
        test { client ->
            assertThrows<IllegalStateException> {
                runBlocking { client.get<String>("/") }
            }

        }
    }

}