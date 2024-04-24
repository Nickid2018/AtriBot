group = "io.github.nickid2018"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://jitpack.io")
}

dependencies {
    api(project(":atribot-core"))
    implementation("io.github.nickid2018:smcl:1.0.4")
}