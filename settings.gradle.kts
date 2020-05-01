rootProject.name = "coroutine-tracing-api"

include(":coroutine-tracing-api-core")
include("coroutine-tracing-api-ktor:coroutine-tracing-api-server-ktor")
include("coroutine-tracing-api-ktor:coroutine-tracing-api-client-ktor")
include("coroutine-tracing-api-ktor:coroutine-tracing-api-ktor-utils")
include("examples:server")
include("examples:client")
