val coroutines_version: String by project
val opentracing_version: String by project
val slf4j_version: String by project
val kotlin_logging_version: String by project
val junit_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm")
}

dependencies {

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("io.opentracing:opentracing-api:$opentracing_version")
    implementation("io.opentracing:opentracing-util:$opentracing_version")
    implementation("io.github.microutils:kotlin-logging:$kotlin_logging_version")
    implementation("org.slf4j:slf4j-api:$slf4j_version")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_version")
    testImplementation("io.opentracing:opentracing-mock:$opentracing_version")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junit_version")
    testRuntimeOnly("ch.qos.logback:logback-classic:$logback_version")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform()
    }
}