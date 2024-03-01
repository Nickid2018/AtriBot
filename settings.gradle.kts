fun includeProject(name: String) {
    include(":${name}")
    project(":${name}").projectDir = file(name)
}

rootProject.name = "AtriBot"

includeProject("atribot-core") // Core module
includeProject("atribot-plugin-api") // Plugin API
includeProject("atribot-network-api") // Network API

// Plugins Here

// Backends Here