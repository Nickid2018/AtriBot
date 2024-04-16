group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

dependencies {
   api(project(":atribot-backend-bridge"))
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.nickid2018.atribot.backend.console.ConsoleBackendMain"
    }
}