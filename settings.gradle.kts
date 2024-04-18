fun includeProject(name: String, path: String = name) {
    include(":${name}")
    project(":${name}").projectDir = file(path)
}

rootProject.name = "atribot"

includeProject("atribot-core") // Core module
includeProject("atribot-backend-bridge") // Backend Bridge

// Plugins Here
includeProject("atribot-plugin-testing", "atribot-plugins/atribot-plugin-testing")
includeProject("atribot-plugin-wakatime", "atribot-plugins/atribot-plugin-wakatime")
includeProject("atribot-plugin-oauth2-service", "atribot-plugins/atribot-plugin-oauth2-service")
includeProject("atribot-plugin-mc-ping", "atribot-plugins/atribot-plugin-mc-ping")
includeProject("atribot-plugin-wiki", "atribot-plugins/atribot-plugin-wiki")

// Backends Here
includeProject("atribot-backend-console") // Test Console Backend
includeProject("atribot-backend-onebot")