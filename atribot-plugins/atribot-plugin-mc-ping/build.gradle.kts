group = "io.github.nickid2018"

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
version = "1.0-SNAPSHOT"

dependencies {
    api(project(":atribot-core"))
    implementation("com.github.Querz:NBT:6.1")
}