fun includeProject(name: String, path: String = name) {
    include(":${name}")
    project(":${name}").projectDir = file(path)
}

rootProject.name = "atribot"

includeProject("atribot-core") // Core module
includeProject("atribot-backend-bridge") // Backend Bridge

// Plugins Here
includeProject("atribot-plugin-testing", "atribot-plugins/atribot-plugin-testing")
includeProject("atribot-plugin-command", "atribot-plugins/atribot-plugin-command")
includeProject("atribot-plugin-wakatime", "atribot-plugins/atribot-plugin-wakatime")
includeProject("atribot-plugin-oauth2-service", "atribot-plugins/atribot-plugin-oauth2-service")
includeProject("atribot-plugin-mc-ping", "atribot-plugins/atribot-plugin-mc-ping")
includeProject("atribot-plugin-wiki", "atribot-plugins/atribot-plugin-wiki")
includeProject("atribot-plugin-web-renderer", "atribot-plugins/atribot-plugin-web-renderer")
includeProject("atribot-plugin-calc", "atribot-plugins/atribot-plugin-calc")
includeProject("atribot-plugin-bilibili", "atribot-plugins/atribot-plugin-bilibili")
includeProject("atribot-plugin-permission-utils", "atribot-plugins/atribot-plugin-permission-utils")
includeProject("atribot-plugin-qrcode", "atribot-plugins/atribot-plugin-qrcode")

// Backends Here
includeProject("atribot-backend-console") // Test Console Backend
includeProject("atribot-backend-onebot")