plugins {
    java
    application
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
