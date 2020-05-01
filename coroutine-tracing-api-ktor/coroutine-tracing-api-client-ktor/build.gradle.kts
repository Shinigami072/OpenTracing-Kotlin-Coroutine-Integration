val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val junit_version: String by project
val coroutines_version: String by project
val opentracing_version: String by project

plugins {
    kotlin("jvm")
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    implementation(project(":coroutine-tracing-api-core"))
    implementation(project(":coroutine-tracing-api-ktor:coroutine-tracing-api-ktor-utils"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("io.ktor:ktor-client-core-jvm:$ktor_version")
    implementation("io.opentracing:opentracing-api:$opentracing_version")
    implementation("io.opentracing:opentracing-util:$opentracing_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.jaegertracing:jaeger-core:1.1.0")
    implementation("io.jaegertracing:jaeger-client:1.1.0")

    testImplementation("io.ktor:ktor-client-tests-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-mock:$ktor_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_version")
    testImplementation("io.opentracing:opentracing-mock:$opentracing_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit_version")

}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform()
    }
}