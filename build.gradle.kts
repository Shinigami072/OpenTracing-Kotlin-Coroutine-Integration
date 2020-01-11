plugins {
    kotlin("jvm") version "1.3.60"
    `maven-publish`
}

group = "shinigami"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("io.opentracing:opentracing-api:0.33.0")
    implementation("io.opentracing:opentracing-util:0.33.0")
    implementation("org.slf4j:slf4j-api:1.7.13")
    implementation("io.jaegertracing:jaeger-core:1.1.0")
    implementation("io.jaegertracing:jaeger-client:1.1.0")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testImplementation("io.opentracing:opentracing-mock:0.33.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.3")
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

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Shinigami072/OpenTracing-Kotlin-Coroutine-Integration")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("PASSWORD")
            }
        }
    }

    publications {
        register("gpr") {
            from(components["java"])
        }
    }
}
