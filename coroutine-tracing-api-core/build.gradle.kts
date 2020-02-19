plugins {
    kotlin("jvm")
    `maven-publish`
}

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

group = rootProject.group
version = rootProject.version

dependencies {
    val coroutinesVersion = project.findProperty("coroutines_version") as String
    val opentracingVersion = project.findProperty("opentracing_version") as String
    val slf4jVersion = project.findProperty("slf4j_version") as String
    val kotlinLoggingVersion = project.findProperty("kotlin_logging_version") as String
    val junitVersion = project.findProperty("junit_version") as String
    val logbackVersion = project.findProperty("logback_version") as String

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.opentracing:opentracing-api:$opentracingVersion")
    implementation("io.opentracing:opentracing-util:$opentracingVersion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoggingVersion")
    implementation("org.slf4j:slf4j-api:$slf4jVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("io.opentracing:opentracing-mock:$opentracingVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
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
        register("gpr", MavenPublication::class) {
            from(components["java"])
        }
    }
}
