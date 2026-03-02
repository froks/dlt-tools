rootProject.name = "dlt-tools"

include("dlt-database")
include("dlt-filter-app")
include("dlt-analyzer-app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev/")
        mavenCentral()
        google()
    }
    plugins {
        kotlin("jvm").version("2.3.10")
        id("org.jetbrains.kotlin.multiplatform").version("2.3.10")
        id("org.jetbrains.kotlin.plugin.compose").version("2.3.10")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        mavenCentral()
        google()
    }
}
