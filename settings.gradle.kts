pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "To-do list"

include(":app")

// Pure Kotlin contracts shared between modules. Depend on nothing.
include(":lib:tasks-api")
include(":lib:navigation-api")

// Shared infrastructure and design system. May depend on :lib only.
include(":core:common")
include(":core:ui")
include(":core:testing")

// Implementations of the :lib contracts. Bound to the contracts in :app.
include(":data:tasks")

// One screen per module. May depend on :core and :lib only — never on each other or on :data.
include(":feature:task-list")
include(":feature:task-editor")
