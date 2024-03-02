plugins {
    id("java")
    id("java-library")
}

group = "io.github.nickid2018"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    api(rootProject)
    api("io.netty:netty-all:4.1.107.Final")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
}

tasks.test {
    useJUnitPlatform()
}