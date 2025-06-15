group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven.nova-committee.cn/s3/")
    maven("https://maven.nova-committee.cn/releases/")
}

dependencies {
    api(project(":atribot-backend-bridge"))
    implementation("cn.evole.onebot:OneBot-Client:0.4.3")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.nickid2018.atribot.backend.onebot.OnebotBackendMain"
    }
}