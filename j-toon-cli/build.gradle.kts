plugins {
    java
    application
    `maven-publish`
    signing
}

application {
    mainClass.set("io.github.koinsaari.jtoon.cli.Main")
}

dependencies {
    implementation(project(":j-toon-core"))
    implementation("tools.jackson.core:jackson-databind:3.0.1")
}

tasks.jar {
    dependsOn(configurations.runtimeClasspath)

    manifest {
        attributes(
            "Main-Class" to "io.github.koinsaari.jtoon.cli.Main",
            "Implementation-Title" to "j-toon-cli",
            "Implementation-Version" to project.version
        )
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }
    })
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
                name.set("j-toon-cli")
                description.set("Command-line tool for converting between JSON and TOON format")
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
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}
