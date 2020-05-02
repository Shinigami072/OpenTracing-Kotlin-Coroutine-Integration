package io.github.shinigami.coroutineTracingApi

import java.io.PrintWriter
import java.io.StringWriter

internal val Throwable.printedStackTrace: String
    get() {
        val errors = StringWriter()
        printStackTrace(PrintWriter(errors))
        return errors.toString()
    }