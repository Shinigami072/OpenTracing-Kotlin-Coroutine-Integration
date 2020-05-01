package io.shinigami.coroutineTracingApi.ktor.utils

import io.ktor.http.HeadersBuilder
import io.opentracing.propagation.TextMap

internal class HeadersBuilderTextMapAdapter(private val headers: HeadersBuilder) : TextMap {
    override fun put(key: String, value: String) {
        headers.append(key, value)
    }

    override fun iterator(): MutableIterator<MutableMap.MutableEntry<String, String>> {
        return headers.entries()
            .filter { (_, values) -> values.isNotEmpty() }
            .map { (key, values) -> key to values.first() }
            .toMap()
            .toMutableMap()//This is only due to opentracing api using mutable map Iterator
            .iterator()
    }

}