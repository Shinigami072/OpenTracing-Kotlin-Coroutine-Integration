package io.shinigami.coroutineTracingApi.ktor.utils

import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.opentracing.propagation.TextMap


fun Headers.asTextMap(): TextMap = HeadersTextMapAdapter(this)

fun HeadersBuilder.asTextMap(): TextMap = HeadersBuilderTextMapAdapter(this)

