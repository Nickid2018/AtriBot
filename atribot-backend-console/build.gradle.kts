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
   api(project(":atribot-backend-bridge"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
}

tasks.test {
    useJUnitPlatform()
}