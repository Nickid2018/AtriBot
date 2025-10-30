group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

dependencies {
    api(project(":atribot-core"))
    api(project(":atribot-plugin-bugtracker"))
    api(project(":atribot-plugin-bilibili"))
    api(project(":atribot-plugin-command"))
    api(project(":atribot-plugin-mc-ping"))
    api(project(":atribot-plugin-oauth2-service"))
    api(project(":atribot-plugin-permission-utils"))
    api(project(":atribot-plugin-qrcode"))
    api(project(":atribot-plugin-server"))
    api(project(":atribot-plugin-wakatime"))
    api(project(":atribot-plugin-web-renderer"))
    api(project(":atribot-plugin-wiki"))
}

tasks.register<JavaExec>("run") {
    mkdir("../../run")
    mainClass = "io.github.nickid2018.atribot.core.AtriBotMain"
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = file("../../run")
    standardInput = System.`in`
    systemProperties["java.net.useSystemProxies"] = "true"
    environment("DEV_PLUGIN", "true")
    jvmArgs("--enable-preview")
}.configure {
    dependsOn(":atribot-core:classes")
}