plugins {
    kotlin("jvm") version "1.3.71"
    id("org.jetbrains.dokka") version "0.10.1" apply false
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
    jcenter()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Shinigami072/OpenTracing-Kotlin-Coroutine-Integration")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("PASSWORD")
        }
    }
}

allprojects {
    group = "io.github.shinigami072"
    version = "0.2.1"
}
val signRequired: String? by project
val toPublish = setOf(
    "coroutine-tracing-api-server-ktor",
    "coroutine-tracing-api-client-ktor",
    "coroutine-tracing-api-ktor-utils",
    "coroutine-tracing-api-core"
)

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    if (name in toPublish) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")
        apply(plugin = "org.jetbrains.dokka")
    }



    repositories {
        mavenCentral()
        jcenter()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Shinigami072/OpenTracing-Kotlin-Coroutine-Integration")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("PASSWORD")
            }
        }
    }

    if (name in toPublish) {
        java {
            withJavadocJar()
            withSourcesJar()
        }

        tasks {
            val dokka by getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
                outputFormat = "html"
                outputDirectory = "$buildDir/dokka"
            }
            val javadocJar by getting(Jar::class) {
                archiveClassifier.set("javadoc")
                dependsOn("dokka")
                from("$buildDir/dokka")
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
                maven {
                    name = "OSSRH"
                    url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                    credentials {
                        username = project.findProperty("OSSRH_user") as String? ?: System.getenv("OSSRH_USERNAME")
                        password = project.findProperty("OSSRH_key") as String? ?: System.getenv("OSSRH_PASSWORD")
                    }

                }
            }

            publications {
                register("gpr", MavenPublication::class) {
                    from(components["java"])
                    afterEvaluate {
                        artifactId = tasks.named<Jar>("jar").get().archiveBaseName.get()
                    }
                    versionMapping {
                        usage("java-api") {
                            fromResolutionOf("runtimeClasspath")
                        }
                        usage("java-runtime") {
                            fromResolutionResult()
                        }
                    }
                    pom {
                        name.set(project.name)
                        description.set(
                            "An Integration Between open Tracing Api and Kotlin Coroutines allowing the use of a Tracer in a trans Thread Context"
                        )

                        url.set("https://github.com/Shinigami072/OpenTracing-Kotlin-Coroutine-Integration")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://github.com/Shinigami072/OpenTracing-Kotlin-Coroutine-Integration/LICENSE")
                            }
                        }
                        developers {
                            developer {
                                id.set("shinigami072")
                                name.set("Krzysztof Stasiowski")
                                email.set("krzys.stasiowski@gmail.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/Shinigami072/OpenTracing-Kotlin-Coroutine-Integration.git")
                            developerConnection.set("scm:git:ssh:git@github.com:Shinigami072/OpenTracing-Kotlin-Coroutine-Integration.git")
                            url.set("https://github.com/Shinigami072/OpenTracing-Kotlin-Coroutine-Integration")
                        }
                    }

                }
            }

        }

        if (signRequired?.toBoolean() ?: false)
            signing {
                val signingKey: String? by project
                val signingPassword: String? by project
                useInMemoryPgpKeys(signingKey, signingPassword)
                sign(publishing.publications["gpr"])
            }
    }
}


