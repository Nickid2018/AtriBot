plugins {
    id("java")
    id("java-library")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "io.github.nickid2018"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // Log dependencies
    api("org.slf4j:slf4j-api:2.0.13")
    api("org.slf4j:jul-to-slf4j:2.0.13")
    api("org.apache.logging.log4j:log4j-api:2.23.1")
    api("org.apache.logging.log4j:log4j-core:2.23.1")
    api("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")

    // Utility dependencies
    api("org.apache.commons:commons-lang3:3.14.0")
    api("commons-io:commons-io:2.15.1")
    api("org.apache.httpcomponents.client5:httpclient5:5.3.1")
    api("com.google.guava:guava:33.1.0-jre")
    api("com.google.code.gson:gson:2.10.1")
    api("it.unimi.dsi:fastutil:8.5.13")
    api("org.snakeyaml:snakeyaml-engine:2.7")

    // Lombok
    compileOnlyApi("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
}

subprojects {
    val project = this

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.9.1"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        annotationProcessor("org.projectlombok:lombok:1.18.32")
        testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
    }

    tasks.test {
        useJUnitPlatform()
    }

    fun isPluginProject(): Boolean {
        return project.configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts
            .any { it.id.componentIdentifier.displayName == "project :atribot-core" }
    }

    tasks.register("generateDependenciesFile") {
        val dependenciesData = project.configurations.runtimeClasspath.get().resolvedConfiguration.resolvedArtifacts

        val coreDependenciesFileData = if (isPluginProject()) {
            project(":atribot-core").configurations.runtimeClasspath.get()
                .resolvedConfiguration.resolvedArtifacts
                .filter { it.type == "jar" }
        } else {
            emptyList()
        }

        val dependenciesFileData = dependenciesData
            .asSequence()
            .filter { it.type == "jar" }
            .filter { !isPluginProject() || !coreDependenciesFileData.contains(it) }
            .map { it.id.componentIdentifier.displayName }
            .filter { !it.startsWith("project") }
            .sorted()
            .joinToString("\n") { it }

        val file = project.layout.buildDirectory.file("DEPENDENCIES").get().asFile
        file.parentFile.mkdirs()
        file.writeText(dependenciesFileData)
    }

    tasks.withType<ProcessResources> {
        dependsOn("generateDependenciesFile")
        from(project.layout.buildDirectory.file("DEPENDENCIES")) {
            into("META-INF")
        }
    }

    tasks.shadowJar {
        dependencies {
            if (!isPluginProject()) {
                include(dependency("io.github.nickid2018:atribot:"))
                include(dependency("io.github.nickid2018:atribot-backend-bridge:"))
            } else {
                exclude(dependency("::"))
            }
        }
        manifest.inheritFrom(tasks.jar.get().manifest)
    }

    tasks.compileJava {
        options.compilerArgs.add("--enable-preview")
    }
}

