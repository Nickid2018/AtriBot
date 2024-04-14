group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

dependencies {
    api(project(":atribot-backend-bridge"))

    api("com.j256.ormlite:ormlite-jdbc:6.1")
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.nickid2018.atribot.core.AtriBotMain"
    }
}