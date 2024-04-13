plugins {
    id("java")
    id("java-library")
}

group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":atribot-core"))
    api(project(":atribot-plugin-wakatime"))
    api(project(":atribot-plugin-oauth2-service"))

    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
}

tasks.test {
    useJUnitPlatform()
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