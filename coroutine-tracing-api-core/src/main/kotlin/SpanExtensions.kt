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

//Todo if needed
//val Span.spanId:String
//    get() = context.toSpanId()

//val Span.traceId:String
//    get() = context.toTraceId()
//
//val Span.baggage:Iterable<Map.Entry<String,String>>
//    get() = context.baggageItems()