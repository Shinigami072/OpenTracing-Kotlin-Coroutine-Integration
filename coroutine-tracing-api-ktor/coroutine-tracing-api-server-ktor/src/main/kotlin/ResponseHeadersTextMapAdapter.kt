package io.shinigami.coroutineTracingApi.ktor.server

import io.ktor.http.Headers
import io.ktor.response.ResponseHeaders
import io.opentracing.propagation.TextMap
import io.shinigami.coroutineTracingApi.ktor.utils.HeadersTextMapAdapter

internal class ResponseHeadersTextMapAdapter(private val responseHeaders: ResponseHeaders) :
    HeadersTextMapAdapter(responseHeaders.allValues()) {

    override val headers: Headers
        get() = responseHeaders.allValues()

    override fun put(key: String, value: String) {
        responseHeaders.append(key, value)
    }
}

internal fun ResponseHeaders.asTextMap(): TextMap {
    return ResponseHeadersTextMapAdapter(this)
}