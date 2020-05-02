package io.github.shinigami.coroutineTracingApi

import io.opentracing.Span
import io.opentracing.SpanContext
import io.opentracing.propagation.TextMap
import io.opentracing.propagation.TextMapAdapter
import io.opentracing.propagation.TextMapExtract

/**
 * This is a Kotlin Friendly Conversion to TextMap
 */
fun Map<String, String>.toTextMap(): TextMapExtract = TextMapAdapter(this)

fun MutableMap<String, String>.toTextMap(): TextMap = TextMapAdapter(this)

/**
 * This is intended to essentially transform context() call into a property
 */
val Span.context: SpanContext
    get() = context()

fun Span.setComponent(value: String): Span =
    setTag(TagConventions.component, value)

data class DbInfo(
    val instance: String,
    val statement: String,
    val user: String,
    val type: String
)

fun Span.setDbInfo(dbInfo: DbInfo): Span = this
    .setTag(TagConventions.db_instance, dbInfo.instance)
    .setTag(TagConventions.db_statement, dbInfo.statement)
    .setTag(TagConventions.db_user, dbInfo.user)
    .setTag(TagConventions.db_type, dbInfo.type)

fun Span.logError(e: Throwable): Span = this
    .setTag(TagConventions.error, true)
    .log(
        mapOf(
            LogFieldConventions.event to "error",
            LogFieldConventions.error_kind to e::class.simpleName,
            LogFieldConventions.error_object to e,
            LogFieldConventions.message to e.message,
            LogFieldConventions.stack to e.printedStackTrace
        )
    )

data class HttpInfo(
    val url: String,
    val status: Int,
    val method: String
)

fun Span.httpInfo(httpInfo: HttpInfo): Span = this
    .setTag(TagConventions.http_method, httpInfo.method)
    .setTag(TagConventions.http_status_code, httpInfo.status)
    .setTag(TagConventions.http_url, httpInfo.url)

object TagConventions {
    /**
     * 	string
     * 	The software package, framework, library, or module that generated the associated Span. E.g., "grpc", "django", "JDBI".
     */
    const val component = "component"

    /**
     * string
     * Database instance name. E.g., In java, if the jdbc.url="jdbc:mysql://127.0.0.1:3306/customers", the instance name is "customers".
     */
    const val db_instance = "db.instance"

    /**
     * string
     * A database statement for the given database type. E.g., for db.type="sql", "SELECT * FROM wuser_table"; for db.type="redis", "SET mykey 'WuValue'".
     */
    const val db_statement = "db.statement"

    /**
     * string
     * Database type. For any SQL database, "sql". For others, the lower-case database category, e.g. "cassandra", "hbase", or "redis".
     */
    const val db_type = "db.type"

    /**
     * string
     * Username for accessing database. E.g., "readonly_user" or "reporting_user"
     */
    const val db_user = "db.user"

    /**
     * bool
     * true if and only if the application considers the operation represented by the Span to have failed
     */
    const val error = "error"

    /**
     * string
     * HTTP method of the request for the associated Span. E.g., "GET", "POST"
     */
    const val http_method = "http.method"

    /**
     *     integer
     *     HTTP response status code for the associated Span. E.g., 200, 503, 404
     */
    const val http_status_code = "http.status_code"

    /**
     *     string
     *     URL of the request being handled in this segment of the trace, in standard URI format. E.g., "https://domain.net/path/to?resource=here"
     */
    const val http_url = "http.url"

    /**
     *     string
     *     An address at which messages can be exchanged. E.g. A Kafka record has an associated "topic name" that can be extracted by the instrumented producer or consumer and stored using this tag.
     */
    const val message_bus_destination = "message_bus.destination"

    /**
     *     string
     *     Remote "address", suitable for use in a networking client library. This may be a "ip:port", a bare "hostname", a FQDN, or even a JDBC substring like "mysql://prod-db:3306"
     */
    const val peer_address = "peer.address"

    /**
     *     string
     *     Remote hostname. E.g., "opentracing.io", "internal.dns.name"
     */
    const val peer_hostname = "peer.hostname"

    /**
     *     string
     *     Remote IPv4 address as a .-separated tuple. E.g., "127.0.0.1"
     */
    const val peer_ipv4 = "peer.ipv4"

    /**
     *     string
     *     Remote IPv6 address as a string of colon-separated 4-char hex tuples. E.g., "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
     */
    const val peer_ipv6 = "peer.ipv6"

    /**
     *     integer
     *     Remote port. E.g., 80
     */
    const val peer_port = "peer.port"

    /**
     *     string
     *     Remote service name (for some unspecified definition of "service"). E.g., "elasticsearch", "a_custom_microservice", "memcache"
     */
    const val peer_service = "peer.service"

    /**
     *     integer
     *     If greater than 0, a hint to the Tracer to do its best to capture the trace. If 0, a hint to the trace to not-capture the trace. If absent, the Tracer should use its default sampling mechanism.
     */
    const val sampling_priority = "sampling.priority"

    /**
     *     string
     *     Either "client" or "server" for the appropriate roles in an RPC, and "producer" or "consumer" for the appropriate roles in a messaging scenario.
     */
    const val span_kind = "span.kind"
}

object LogFieldConventions {
    /**
     * string
     * The type or "kind" of an error (only for event="error" logs). E.g., "Exception", "OSError"
     */
    const val error_kind = "error.kind"

    /**
     * object
     * For languages that support such a thing (e.g., Java, Python), the actual Throwable/Exception/Error object instance itself. E.g., A java.lang.UnsupportedOperationException instance, a python exceptions.NameError instance
     */
    const val error_object = "error.object"

    /**
     *   string
     *   A stable identifier for some notable moment in the lifetime of a Span. For instance, a mutex lock acquisition or release or the sorts of lifetime events in a browser page load described in the Performance.timing specification. E.g., from Zipkin, "cs", "sr", "ss", or "cr". Or, more generally, "initialized" or "timed out". For errors, "error"
     */
    const val event = "event"

    /**
     * string
     * A concise, human-readable, one-line message explaining the event. E.g., "Could not connect to backend", "Cache invalidation succeeded"
     */
    const val message = "message"

    /**
     *  string
     *  A stack trace in platform-conventional format; may or may not pertain to an error. E.g., "File \"example.py\", line 7, in \<module\>\ncaller()\nFile \"example.py\", line 5, in caller\ncallee()\nFile \"example.py\", line 2, in callee\nraise Exception(\"Yikes\")\n"
     */
    const val stack = "stack"
}