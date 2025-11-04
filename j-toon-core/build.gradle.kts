plugins {
    java
    `java-library`
    `maven-publish`
    signing
}

dependencies {
    implementation("tools.jackson.core:jackson-databind:3.0.1")
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "j-toon-core",
            "Implementation-Version" to project.version
        )
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("j-toon-core")
                description.set("Java implementation of Token-Oriented Object Notation (TOON) - A compact, human-readable serialization format optimized for LLM input with significantly reduced token usage")
                url.set("https://github.com/koinsaari/j-toon")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("koinsaari")
                        name.set("Aaro Koinsaari")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/koinsaari/j-toon.git")
                    developerConnection.set("scm:git:ssh://git@github.com:koinsaari/j-toon.git")
                    url.set("https://github.com/koinsaari/j-toon")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
