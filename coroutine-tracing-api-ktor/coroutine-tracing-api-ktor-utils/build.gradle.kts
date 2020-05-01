val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val junit_version: String by project
val coroutines_version: String by project
val opentracing_version: String by project

plugins {
    kotlin("jvm")
    `maven-publish`
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Shinigami072/OpenTracing-Kotlin-Coroutine-Integration")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("PASSWORD")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(project(":coroutine-tracing-api-core"))
    implementation("io.opentracing:opentracing-api:$opentracing_version")
    implementation("io.opentracing:opentracing-util:$opentracing_version")
    implementation("io.ktor:ktor-utils-jvm:$ktor_version")
    implementation("io.ktor:ktor-http-jvm:$ktor_version")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
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
        register("gpr", MavenPublication::class) {
            from(components["java"])
        }
    }
}