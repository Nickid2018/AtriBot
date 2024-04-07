fun includeProject(name: String) {
    include(":${name}")
    project(":${name}").projectDir = file(name)
}

rootProject.name = "AtriBot"

includeProject("atribot-core") // Core module
includeProject("atribot-backend-bridge") // Backend Bridge

// Plugins Here

// Backends Here
includeProject("atribot-backend-console") // Test Console Backend