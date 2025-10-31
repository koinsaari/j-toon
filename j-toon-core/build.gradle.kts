plugins {
    java
    `java-library`
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
