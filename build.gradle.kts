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
    // Log dependencies
    api("org.slf4j:slf4j-api:2.0.12")
    api("org.apache.logging.log4j:log4j-api:2.23.0")
    api("org.apache.logging.log4j:log4j-core:2.23.0")
    api("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.0")

    // Utility dependencies
    api("org.apache.commons:commons-lang3:3.14.0")
    api("commons-io:commons-io:2.15.1")
    api("org.apache.httpcomponents.client5:httpclient5:5.3.1")
    api("com.google.guava:guava:33.0.0-jre")
    api("it.unimi.dsi:fastutil:8.5.13")

    // Lombok
    compileOnlyApi("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
}