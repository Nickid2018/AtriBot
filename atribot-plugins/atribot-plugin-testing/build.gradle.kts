group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

dependencies {
    api(project(":atribot-core"))
    api(project(":atribot-plugin-wakatime"))
    api(project(":atribot-plugin-oauth2-service"))
}

tasks.register<JavaExec>("run") {
    mainClass = "io.github.nickid2018.atribot.core.AtriBotMain"
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = file("../../run")
    standardInput = System.`in`
    environment("DEV_PLUGIN", "true")
}.configure {
    dependsOn(":atribot-core:classes")
}