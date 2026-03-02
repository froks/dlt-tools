plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.3.10" apply false
    id("org.jetbrains.compose") version "1.6.10" apply false
    id("org.jetbrains.kotlin.plugin.compose")
}

allprojects {
    group = "de.debugco"
    version = "1.6.0"
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
