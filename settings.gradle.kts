pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HelioFlux"

include(":app")
include(":core:model")
include(":core:data")
include(":core:database")
include(":feature:globe")
include(":feature:widgets")
include(":feature:alerts")
